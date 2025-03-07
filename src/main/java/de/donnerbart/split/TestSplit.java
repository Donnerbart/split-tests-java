package de.donnerbart.split;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithName;
import de.donnerbart.split.model.Splits;
import de.donnerbart.split.model.TestCase;
import de.donnerbart.split.model.TestSuite;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.NOPLogger;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import static de.donnerbart.split.util.FormatUtil.formatTime;

public class TestSplit {

    private static final @NotNull Set<String> SKIP_TEST_IMPORTS =
            Set.of("org.junit.jupiter.api.Disabled", "org.junit.Ignore");
    private static final @NotNull Set<String> SKIP_TEST_ANNOTATIONS = Set.of("Disabled", "Ignore");

    private final int splitTotal;
    private final @NotNull String glob;
    private final @Nullable String excludeGlob;
    private final @Nullable String junitGlob;
    private final @NotNull FormatOption formatOption;
    private final @NotNull NewTestTimeOption newTestTimeOption;
    private final @NotNull Path workingDirectory;
    private final @NotNull Logger log;
    private final boolean debug;
    private final @NotNull Consumer<Integer> exitCodeConsumer;

    public TestSplit(
            final int splitTotal,
            final @NotNull String glob,
            final @Nullable String excludeGlob,
            final @Nullable String junitGlob,
            final @NotNull FormatOption formatOption,
            final @NotNull NewTestTimeOption newTestTimeOption,
            final @NotNull Path workingDirectory,
            final boolean log,
            final boolean debug,
            final @NotNull Consumer<Integer> exitCodeConsumer) {
        this.splitTotal = splitTotal;
        this.glob = glob;
        this.excludeGlob = excludeGlob;
        this.junitGlob = junitGlob;
        this.formatOption = formatOption;
        this.newTestTimeOption = newTestTimeOption;
        this.workingDirectory = workingDirectory;
        this.log = log ? LoggerFactory.getLogger(TestSplit.class) : NOPLogger.NOP_LOGGER;
        this.debug = debug;
        this.exitCodeConsumer = exitCodeConsumer;
    }

    public @NotNull Splits run() throws Exception {
        final var testPaths = getPaths(workingDirectory, glob, excludeGlob);
        final var classNames = fileToClassName(testPaths, exitCodeConsumer);
        if (classNames.isEmpty()) {
            log.error("Found no test classes");
            exitCodeConsumer.accept(1);
        } else {
            log.info("Found {} test classes", classNames.size());
        }

        final var testCases = new HashSet<TestCase>();
        if (junitGlob != null) {
            // analyze JUnit reports
            final var junitPaths = getPaths(workingDirectory, junitGlob, null);
            log.info("Found {} JUnit report files", junitPaths.size());
            if (!junitPaths.isEmpty()) {
                var fastestTest = new TestCase("", Double.MAX_VALUE);
                var slowestTest = new TestCase("", Double.MIN_VALUE);
                final var xmlMapper = new XmlMapper();
                for (final var junitPath : junitPaths) {
                    final var testSuite = xmlMapper.readValue(junitPath.toFile(), TestSuite.class);
                    final var testCase = new TestCase(testSuite.getName(), testSuite.getTime());
                    if (classNames.contains(testCase.name())) {
                        if (testCases.add(testCase)) {
                            log.debug("Adding test {} [{}]", testCase.name(), formatTime(testCase.time()));
                            if (testCase.time() < fastestTest.time()) {
                                fastestTest = testCase;
                            }
                            if (testCase.time() > slowestTest.time()) {
                                slowestTest = testCase;
                            }
                        }
                    } else {
                        log.info("Skipping test {} from JUnit report", testCase.name());
                    }
                }
                log.debug("Found {} recorded test classes with time information", testCases.size());
                log.debug("Fastest test class: {} ({})", fastestTest.name(), formatTime(fastestTest.time()));
                log.debug("Slowest test class: {} ({})", slowestTest.name(), formatTime(slowestTest.time()));
            }
        }
        // add tests without timing records
        final var newTestTime = getNewTestTime(newTestTimeOption, testCases);
        classNames.forEach(className -> {
            final var testCase = new TestCase(className, newTestTime);
            if (testCases.add(testCase)) {
                log.debug("Adding test {} [estimated {}]", testCase.name(), formatTime(testCase.time()));
            }
        });

        // split tests
        log.debug("Splitting {} tests", testCases.size());
        final var splits = new Splits(splitTotal, formatOption);
        testCases.stream().sorted(Comparator.reverseOrder()).forEach(testCase -> {
            final var split = splits.add(testCase);
            log.debug("Adding test {} to split #{}", testCase.name(), split.index());
        });

        if (debug) {
            if (splitTotal > 1) {
                final var fastestSplit = splits.getFastest();
                log.debug("Fastest test plan is #{} with {} tests ({})",
                        fastestSplit.formatIndex(),
                        fastestSplit.tests().size(),
                        formatTime(fastestSplit.totalRecordedTime()));
                final var slowestSplit = splits.getSlowest();
                log.debug("Slowest test plan is #{} with {} tests ({})",
                        slowestSplit.formatIndex(),
                        slowestSplit.tests().size(),
                        formatTime(slowestSplit.totalRecordedTime()));
                log.debug("Difference between the fastest and slowest test plan: {}",
                        formatTime(slowestSplit.totalRecordedTime() - fastestSplit.totalRecordedTime()));
            }
            log.debug("Test splits:");
            splits.forEach(split -> log.debug(split.toString()));
        }
        return splits;
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
                            log.debug("Excluding test file {}", candidate);
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
                    log.info("Skipping interface {}", className);
                    continue;
                } else if (declaration.isAbstract()) {
                    log.info("Skipping abstract class {}", className);
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
                    log.info("Skipping disabled test class {}", className);
                    continue;
                }
                classNames.add(className);
            } catch (final Exception e) {
                log.error("Failed to parse test class {}", testPath, e);
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
                log.info("Average test time is {}", formatTime(averageTime));
                yield averageTime;
            }
            case MIN -> {
                final var minTime = testCases.stream().mapToDouble(TestCase::time).min().orElseThrow();
                log.info("Minimum test time is {}", formatTime(minTime));
                yield minTime;
            }
            case MAX -> {
                final var maxTime = testCases.stream().mapToDouble(TestCase::time).max().orElseThrow();
                log.info("Maximum test time is {}", formatTime(maxTime));
                yield maxTime;
            }
        };
    }
}
