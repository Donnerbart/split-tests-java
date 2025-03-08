package de.donnerbart.split;

import ch.qos.logback.classic.Level;
import com.beust.jcommander.JCommander;
import de.donnerbart.split.model.Splits;
import de.donnerbart.split.model.TestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.function.Consumer;

import static de.donnerbart.split.util.FormatUtil.formatTime;

public class TestSplitMain {

    private static final @NotNull Logger LOG = LoggerFactory.getLogger(TestSplitMain.class);

    public static void main(final @Nullable String @NotNull [] args) throws Exception {
        run(System::exit, args);
    }

    @VisibleForTesting
    static @NotNull Splits run(final @NotNull Consumer<Integer> exitConsumer, final @Nullable String @NotNull [] args)
            throws Exception {
        final var arguments = new Arguments();
        final var jCommander = JCommander.newBuilder().addObject(arguments).build();
        jCommander.parse(args);
        if (arguments.help) {
            jCommander.usage();
            exitConsumer.accept(0);
        }
        if (arguments.debug) {
            final var root = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
            root.setLevel(Level.DEBUG);
        }
        final var workingDirectory =
                Objects.requireNonNullElse(arguments.workingDirectory, Paths.get(System.getProperty("user.dir")))
                        .toAbsolutePath()
                        .normalize();
        if (!validateArguments(arguments, workingDirectory)) {
            exitConsumer.accept(1);
        }
        final var properties = readProperties("split-tests-java.properties");
        LOG.info("split-tests-java {} (commit: {} on branch: {}) built on {}",
                properties.getProperty("version", "unknown"),
                properties.getProperty("git.commit.id.abbrev", "unknown"),
                properties.getProperty("git.branch", "unknown"),
                getBuiltTime(properties.getProperty("git.commit.time", "unknown")));
        LOG.info("Split index {} (total: {})", arguments.splitIndex, arguments.splitTotal);
        LOG.info("Working directory: {}", workingDirectory);
        LOG.info("Glob: {}", arguments.glob);
        if (arguments.excludeGlob != null) {
            LOG.info("Exclude glob: {}", arguments.excludeGlob);
        }
        if (arguments.junitGlob != null) {
            LOG.info("JUnit glob: {}", arguments.junitGlob);
        }
        LOG.info("Output format: {}", arguments.formatOption);
        final var testLoader = new TestLoader(arguments.glob,
                arguments.excludeGlob,
                arguments.junitGlob,
                arguments.newTestTimeOption,
                workingDirectory,
                System::exit);
        final var testCases = testLoader.load();
        if (arguments.calculateOptimalTotalSplit) {
            calculateOptimalTotalSplit(arguments, testCases);
        }
        final var testSplit = new TestSplit(testCases, arguments.splitTotal, arguments.formatOption, arguments.debug);
        final var splits = testSplit.split();
        final var split = splits.get(arguments.splitIndex);
        LOG.info("This test split has {} tests ({})", split.tests().size(), formatTime(split.totalRecordedTime()));
        System.out.print(String.join(" ", splits.get(arguments.splitIndex).sortedTests()));
        return splits;
    }

    @VisibleForTesting
    static boolean validateArguments(final @NotNull Arguments arguments, final @NotNull Path workingDirectory) {
        if (arguments.splitTotal < 1) {
            LOG.error("--split-total must be greater than 0");
            return false;
        }
        if (arguments.splitIndex > arguments.splitTotal - 1) {
            LOG.error("--split-index must lesser than --split-total");
            return false;
        }
        if (!Files.exists(workingDirectory)) {
            LOG.error("Working directory does not exist: {}", arguments.workingDirectory);
            return false;
        }
        return true;
    }

    @VisibleForTesting
    static int calculateOptimalTotalSplit(final @NotNull Arguments arguments, final @NotNull Set<TestCase> testCases) {
        if (arguments.junitGlob == null) {
            LOG.warn("The option --calculate-optimal-total-split requires --junit-glob");
            return 0;
        }
        if (arguments.splitIndex != 0) {
            LOG.debug("Skipping calculation of optimal test split (only done on the first index)");
            return 0;
        }
        LOG.info("Calculating optimal test split");
        var optimalSplit = 1;
        var lastSlowestSplit = Double.MAX_VALUE;
        while (true) {
            final var testSplit = new TestSplit(testCases, optimalSplit, arguments.formatOption, false);
            final var splits = testSplit.split();
            final var slowestSplit = splits.getSlowest().totalRecordedTime();
            if (Double.compare(slowestSplit, lastSlowestSplit) == 0) {
                optimalSplit--;
                LOG.info("The optimal --total-split value for this test suite is {}", optimalSplit);
                if (optimalSplit != arguments.splitTotal) {
                    LOG.warn("The --split-total value of {} does not match the optimal split of {}",
                            arguments.splitTotal,
                            optimalSplit);
                }
                return optimalSplit;
            }
            LOG.debug("The slowest split with {} splits takes {}", optimalSplit, formatTime(slowestSplit));
            if (optimalSplit++ >= arguments.maxOptimalTotalSplitCalculations) {
                LOG.warn(
                        "The option --max-optimal-total-split-calculations of {} is too low to calculate the optimal test split",
                        arguments.maxOptimalTotalSplitCalculations);
                return 0;
            }
            lastSlowestSplit = slowestSplit;
        }
    }

    @VisibleForTesting
    static @NotNull Properties readProperties(final @NotNull String resourceFile) throws Exception {
        final var properties = new Properties();
        try (final var inputStream = TestSplitMain.class.getClassLoader().getResourceAsStream(resourceFile)) {
            if (inputStream != null) {
                properties.load(inputStream);
            }
        }
        return properties;
    }

    @VisibleForTesting
    static @NotNull Date getBuiltTime(final @NotNull String dateString) {
        try {
            return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").parse(dateString);
        } catch (final Exception e) {
            return Date.from(Instant.EPOCH);
        }
    }
}
