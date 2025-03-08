package de.donnerbart.split;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithName;
import de.donnerbart.split.model.TestCase;
import de.donnerbart.split.model.TestSuite;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import static de.donnerbart.split.util.FormatUtil.formatTime;

public class TestLoader {

    private static final @NotNull Set<String> SKIP_TEST_IMPORTS =
            Set.of("org.junit.jupiter.api.Disabled", "org.junit.Ignore");
    private static final @NotNull Set<String> SKIP_TEST_ANNOTATIONS = Set.of("Disabled", "Ignore");

    private static final @NotNull Logger LOG = LoggerFactory.getLogger(TestLoader.class);

    private final @NotNull String glob;
    private final @Nullable String excludeGlob;
    private final @Nullable String junitGlob;
    private final @NotNull NewTestTimeOption newTestTimeOption;
    private final @NotNull Path workingDirectory;
    private final @NotNull Consumer<Integer> exitCodeConsumer;

    public TestLoader(
            final @NotNull String glob,
            final @Nullable String excludeGlob,
            final @Nullable String junitGlob,
            final @NotNull NewTestTimeOption newTestTimeOption,
            final @NotNull Path workingDirectory,
            final @NotNull Consumer<Integer> exitCodeConsumer) {
        this.glob = glob;
        this.excludeGlob = excludeGlob;
        this.junitGlob = junitGlob;
        this.newTestTimeOption = newTestTimeOption;
        this.workingDirectory = workingDirectory;
        this.exitCodeConsumer = exitCodeConsumer;
    }

    public @NotNull Set<TestCase> load() throws Exception {
        final var testPaths = getPaths(workingDirectory, glob, excludeGlob);
        final var classNames = fileToClassName(testPaths, exitCodeConsumer);
        if (classNames.isEmpty()) {
            LOG.error("Found no test classes");
            exitCodeConsumer.accept(1);
        } else {
            LOG.info("Found {} test classes", classNames.size());
        }

        final var testCases = new HashSet<TestCase>();
        if (junitGlob != null) {
            // analyze JUnit reports
            final var junitPaths = getPaths(workingDirectory, junitGlob, null);
            LOG.info("Found {} JUnit report files", junitPaths.size());
            if (!junitPaths.isEmpty()) {
                var fastestTest = new TestCase("", Double.MAX_VALUE);
                var slowestTest = new TestCase("", Double.MIN_VALUE);
                final var xmlMapper = new XmlMapper();
                for (final var junitPath : junitPaths) {
                    final var testSuite = xmlMapper.readValue(junitPath.toFile(), TestSuite.class);
                    final var testCase = new TestCase(testSuite.getName(), testSuite.getTime());
                    if (classNames.contains(testCase.name())) {
                        if (testCases.add(testCase)) {
                            LOG.debug("Adding test {} [{}]", testCase.name(), formatTime(testCase.time()));
                            if (testCase.time() < fastestTest.time()) {
                                fastestTest = testCase;
                            }
                            if (testCase.time() > slowestTest.time()) {
                                slowestTest = testCase;
                            }
                        }
                    } else {
                        LOG.info("Skipping test {} from JUnit report", testCase.name());
                    }
                }
                LOG.debug("Found {} recorded test classes with time information", testCases.size());
                LOG.debug("Fastest test class: {} ({})", fastestTest.name(), formatTime(fastestTest.time()));
                LOG.debug("Slowest test class: {} ({})", slowestTest.name(), formatTime(slowestTest.time()));
            }
        }
        // add tests without timing records
        final var newTestTime = getNewTestTime(newTestTimeOption, testCases);
        classNames.forEach(className -> {
            final var testCase = new TestCase(className, newTestTime);
            if (testCases.add(testCase)) {
                LOG.debug("Adding test {} [estimated {}]", testCase.name(), formatTime(testCase.time()));
            }
        });
        return testCases;
    }

    private @NotNull Set<Path> getPaths(
            final @NotNull Path rootPath,
            final @NotNull String glob,
            final @Nullable String excludeGlob) throws Exception {
        final var files = new HashSet<Path>();
        final var includeMatcher = FileSystems.getDefault().getPathMatcher("glob:" + glob);
        final var excludeMatcher = FileSystems.getDefault().getPathMatcher("glob:" + excludeGlob);
        Files.walkFileTree(rootPath, new SimpleFileVisitor<>() {
            @Override
            public @NotNull FileVisitResult visitFile(
                    final @Nullable Path path,
                    final @NotNull BasicFileAttributes attributes) {
                if (path != null) {
                    final var candidate = path.normalize();
                    if (includeMatcher.matches(candidate)) {
                        if (excludeMatcher.matches(candidate)) {
                            LOG.debug("Excluding test file {}", candidate);
                        } else {
                            files.add(candidate);
                        }
                    }
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public @NotNull FileVisitResult visitFileFailed(final @Nullable Path file, final @NotNull IOException e) {
                return FileVisitResult.CONTINUE;
            }
        });
        return files;
    }

    private @NotNull Set<String> fileToClassName(
            final @NotNull Set<Path> testPaths,
            final @NotNull Consumer<Integer> exitCodeConsumer) {
        final var javaParser = new JavaParser();
        final var classNames = new HashSet<String>();
        for (final var testPath : testPaths) {
            try {
                final var compilationUnit = javaParser.parse(testPath).getResult().orElseThrow();
                final var declaration = compilationUnit.findFirst(ClassOrInterfaceDeclaration.class).orElseThrow();
                final var className = declaration.getFullyQualifiedName().orElseThrow();
                if (declaration.isInterface()) {
                    LOG.info("Skipping interface {}", className);
                    continue;
                } else if (declaration.isAbstract()) {
                    LOG.info("Skipping abstract class {}", className);
                    continue;
                }
                final var hasSkipTestImport = compilationUnit.getImports()
                        .stream()
                        .map(NodeWithName::getNameAsString)
                        .anyMatch(SKIP_TEST_IMPORTS::contains);
                final var hasSkipTestAnnotation = declaration.getAnnotations()
                        .stream()
                        .map(AnnotationExpr::getNameAsString)
                        .anyMatch(SKIP_TEST_ANNOTATIONS::contains);
                if (hasSkipTestImport && hasSkipTestAnnotation) {
                    LOG.info("Skipping disabled test class {}", className);
                    continue;
                }
                classNames.add(className);
            } catch (final Exception e) {
                LOG.error("Failed to parse test class {}", testPath, e);
                exitCodeConsumer.accept(1);
            }
        }
        return classNames;
    }

    private double getNewTestTime(
            final @NotNull NewTestTimeOption useAverageTimeForNewTests,
            final @NotNull Set<TestCase> testCases) {
        if (testCases.isEmpty()) {
            return 0d;
        }
        return switch (useAverageTimeForNewTests) {
            case ZERO -> 0d;
            case AVERAGE -> {
                final var averageTime =
                        testCases.stream().mapToDouble(TestCase::time).sum() / (double) testCases.size();
                LOG.info("Average test time is {}", formatTime(averageTime));
                yield averageTime;
            }
            case MIN -> {
                final var minTime = testCases.stream().mapToDouble(TestCase::time).min().orElseThrow();
                LOG.info("Minimum test time is {}", formatTime(minTime));
                yield minTime;
            }
            case MAX -> {
                final var maxTime = testCases.stream().mapToDouble(TestCase::time).max().orElseThrow();
                LOG.info("Maximum test time is {}", formatTime(maxTime));
                yield maxTime;
            }
        };
    }
}
