package de.donnerbart.split;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithName;
import de.donnerbart.split.model.Split;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static de.donnerbart.split.util.FormatUtil.formatTime;

public class TestSplit {

    private static final @NotNull Set<String> SKIP_TEST_IMPORTS =
            Set.of("org.junit.jupiter.api.Disabled", "org.junit.Ignore");
    private static final @NotNull Set<String> SKIP_TEST_ANNOTATIONS = Set.of("Disabled", "Ignore");

    private static final @NotNull Logger LOG = LoggerFactory.getLogger(TestSplit.class);

    private final int splitIndex;
    private final int splitTotal;
    private final @NotNull String glob;
    private final @Nullable String excludeGlob;
    private final @Nullable String junitGlob;
    private final @NotNull FormatOption format;
    private final boolean useAverageTimeForNewTests;
    private final @NotNull Path workingDirectory;
    private final boolean debug;
    private final @NotNull Consumer<Integer> exitCodeConsumer;

    public TestSplit(
            final int splitIndex,
            final int splitTotal,
            final @NotNull String glob,
            final @Nullable String excludeGlob,
            final @Nullable String junitGlob,
            final @NotNull FormatOption format,
            final boolean useAverageTimeForNewTests,
            final @NotNull Path workingDirectory,
            final boolean debug,
            final @NotNull Consumer<Integer> exitCodeConsumer) {
        this.splitIndex = splitIndex;
        this.splitTotal = splitTotal;
        this.glob = glob;
        this.excludeGlob = excludeGlob;
        this.junitGlob = junitGlob;
        this.format = format;
        this.useAverageTimeForNewTests = useAverageTimeForNewTests;
        this.workingDirectory = workingDirectory;
        this.debug = debug;
        this.exitCodeConsumer = exitCodeConsumer;
    }

    public @NotNull List<String> run() throws Exception {
        LOG.info("Split index {} (total: {})", splitIndex, splitTotal);
        LOG.info("Working directory: {}", workingDirectory);
        LOG.info("Glob: {}", glob);
        if (excludeGlob != null) {
            LOG.info("Exclude glob: {}", excludeGlob);
        }
        if (junitGlob != null) {
            LOG.info("JUnit glob: {}", junitGlob);
        }
        LOG.info("Output format: {}", format);
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
                var slowestTest = new TestCase("", 0d);
                final var xmlMapper = new XmlMapper();
                for (final var junitPath : junitPaths) {
                    final var testSuite = xmlMapper.readValue(junitPath.toFile(), TestSuite.class);
                    final var testCase = new TestCase(testSuite.getName(), testSuite.getTime());
                    if (classNames.contains(testCase.name())) {
                        if (testCases.add(testCase)) {
                            LOG.debug("Adding test {} [{}]", testCase.name(), formatTime(testCase.time()));
                        }
                    } else {
                        LOG.info("Skipping test {} from JUnit report", testCase.name());
                    }
                    if (testCase.time() > slowestTest.time()) {
                        slowestTest = testCase;
                    }
                }
                LOG.debug("Found {} recorded test classes with time information", testCases.size());
                LOG.debug("Slowest test class: {} ({})", slowestTest.name(), formatTime(slowestTest.time()));
            }
        }
        // add tests without timing records
        final var newTestTime = getNewTestTime(useAverageTimeForNewTests, testCases);
        classNames.forEach(className -> {
            final var testCase = new TestCase(className, newTestTime);
            if (testCases.add(testCase)) {
                LOG.debug("Adding test {}", testCase.name());
            }
        });

        // split tests
        LOG.debug("Splitting {} tests", testCases.size());
        final var splits = new ArrayList<Split>(splitTotal);
        for (int i = 0; i < splitTotal; i++) {
            splits.add(new Split(i));
        }
        testCases.stream()
                .sorted(Comparator.reverseOrder())
                .forEach(testCase -> splits.stream().sorted().findFirst().ifPresent(split -> {
                    LOG.debug("Adding test {} to split #{}", testCase.name(), split.index());
                    split.add(testCase);
                }));

        if (debug) {
            if (splitTotal > 1) {
                final var fastestSplit = splits.stream().min(Comparator.naturalOrder()).orElseThrow();
                LOG.debug("Fastest test plan is #{} with {} tests ({})",
                        fastestSplit.formatIndex(),
                        fastestSplit.tests().size(),
                        formatTime(fastestSplit.totalRecordedTime()));
                final var slowestSplit = splits.stream().max(Comparator.naturalOrder()).orElseThrow();
                LOG.debug("Slowest test plan is #{} with {} tests ({})",
                        slowestSplit.formatIndex(),
                        slowestSplit.tests().size(),
                        formatTime(slowestSplit.totalRecordedTime()));
                LOG.debug("Difference between the fastest and slowest test plan: {}",
                        formatTime(slowestSplit.totalRecordedTime() - fastestSplit.totalRecordedTime()));
            }
            LOG.debug("Test splits:");
            splits.stream().sorted(Comparator.reverseOrder()).forEach(n -> LOG.debug(n.toString()));
        }
        final var split = splits.get(splitIndex);
        LOG.info("This test split has {} tests ({})", split.tests().size(), formatTime(split.totalRecordedTime()));
        return split.tests()
                .stream()
                .sorted(Comparator.reverseOrder())
                .map(TestCase::name)
                .map(test -> switch (format) {
                    case LIST -> test;
                    case GRADLE -> "--tests " + test;
                })
                .collect(Collectors.toList());
    }

    private static @NotNull Set<Path> getPaths(
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

    private static @NotNull Set<String> fileToClassName(
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

    private static double getNewTestTime(
            final boolean useAverageTimeForNewTests,
            final @NotNull Set<TestCase> testCases) {
        if (!useAverageTimeForNewTests || testCases.isEmpty()) {
            return 0d;
        }
        final var averageTime = testCases.stream().mapToDouble(TestCase::time).sum() / (double) testCases.size();
        LOG.info("Average test time is {}", formatTime(averageTime));
        return averageTime;
    }
}
