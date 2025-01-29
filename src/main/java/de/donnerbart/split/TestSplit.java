package de.donnerbart.split;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
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
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static de.donnerbart.split.Util.formatIndex;
import static de.donnerbart.split.Util.formatTime;

public class TestSplit {

    private static final @NotNull Pattern PACKAGE_PATTERN = Pattern.compile("package\\s+([\\S.]+?)\\s*;");
    private static final @NotNull Pattern CLASS_NAME_PATTERN = Pattern.compile("class\\s+(\\S+?)\\s+");

    private static final @NotNull Logger LOG = LoggerFactory.getLogger(TestSplit.class);

    private final int splitIndex;
    private final int splitTotal;
    private final @NotNull String glob;
    private final @Nullable String excludeGlob;
    private final @Nullable String junitGlob;
    private final @NotNull Path workingDirectory;
    private final boolean debug;
    private final @NotNull Consumer<Integer> exitCodeConsumer;

    public TestSplit(
            final int splitIndex,
            final int splitTotal,
            final @NotNull String glob,
            final @Nullable String excludeGlob,
            final @Nullable String junitGlob,
            final @NotNull Path workingDirectory,
            final boolean debug,
            final @NotNull Consumer<Integer> exitCodeConsumer) {
        this.splitIndex = splitIndex;
        this.splitTotal = splitTotal;
        this.glob = glob;
        this.excludeGlob = excludeGlob;
        this.junitGlob = junitGlob;
        this.workingDirectory = workingDirectory;
        this.debug = debug;
        this.exitCodeConsumer = exitCodeConsumer;
    }

    public void run() throws Exception {
        LOG.info("Split index {} (total: {})", splitIndex, splitTotal);
        LOG.info("Working directory: {}", workingDirectory);
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
        // add tests without timing records
        classNames.forEach(className -> {
            final var testCase = new TestCase(className, 0d);
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
                    LOG.debug("Splitting test {} to split #{}", testCase.name(), split.index());
                    split.add(testCase);
                }));

        if (debug) {
            final var fastestSplit = splits.stream().min(Comparator.naturalOrder()).orElseThrow();
            LOG.debug("Fastest test plan is #{} with {} tests ({})",
                    formatIndex(fastestSplit.index()),
                    fastestSplit.tests().size(),
                    formatTime(fastestSplit.totalRecordedTime()));
            final var slowestSplit = splits.stream().max(Comparator.naturalOrder()).orElseThrow();
            LOG.debug("Slowest test plan is #{} with {} tests ({})",
                    formatIndex(slowestSplit.index()),
                    slowestSplit.tests().size(),
                    formatTime(slowestSplit.totalRecordedTime()));
            LOG.debug("Difference between the fastest and slowest test plan: {}",
                    formatTime(slowestSplit.totalRecordedTime() - fastestSplit.totalRecordedTime()));
            LOG.debug("Test splits:");
            splits.stream().sorted(Comparator.reverseOrder()).forEach(n -> LOG.debug(n.toString()));
        }
        final var split = splits.get(splitIndex);
        LOG.info("This test split has {} tests ({})", split.tests().size(), formatTime(split.totalRecordedTime()));
        System.out.print(split.tests()
                .stream()
                .sorted(Comparator.reverseOrder())
                .map(TestCase::name)
                .collect(Collectors.joining(" ")));
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
                    if (includeMatcher.matches(candidate) && !excludeMatcher.matches(candidate)) {
                        files.add(candidate);
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
            final @NotNull Consumer<Integer> exitCodeConsumer)
            throws Exception {
        final var classNames = new HashSet<String>();
        for (final var testPath : testPaths) {
            final var testClassContent = Files.readString(testPath);
            final var packagematcher = PACKAGE_PATTERN.matcher(testClassContent);
            if (!packagematcher.find()) {
                LOG.error("Found no package in file '{}'", testPath);
                exitCodeConsumer.accept(1);
                continue;
            }
            final var packageName = packagematcher.group(1);
            final var classNameMatcher = CLASS_NAME_PATTERN.matcher(testClassContent);
            if (!classNameMatcher.find()) {
                LOG.error("Found no class name in file '{}'", testPath);
                exitCodeConsumer.accept(1);
                continue;
            }
            final var className = classNameMatcher.group(1);
            classNames.add(packageName + "." + className);
        }
        return classNames;
    }
}
