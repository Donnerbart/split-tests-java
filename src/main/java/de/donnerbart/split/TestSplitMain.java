package de.donnerbart.split;

import ch.qos.logback.classic.Level;
import com.beust.jcommander.JCommander;
import de.donnerbart.split.model.Splits;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
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
        if (arguments.calculateOptimalTotalSplit) {
            calculateOptimalTotalSplit(arguments, workingDirectory);
        }
        final var testSplit = new TestSplit(arguments.splitTotal,
                arguments.glob,
                arguments.excludeGlob,
                arguments.junitGlob,
                arguments.formatOption,
                arguments.newTestTimeOption,
                workingDirectory,
                true,
                arguments.debug,
                System::exit);
        final var splits = testSplit.run();
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
    static int calculateOptimalTotalSplit(final @NotNull Arguments arguments, final @NotNull Path workingDirectory)
            throws Exception {
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
            final var testSplit = new TestSplit(optimalSplit,
                    arguments.glob,
                    arguments.excludeGlob,
                    arguments.junitGlob,
                    arguments.formatOption,
                    arguments.newTestTimeOption,
                    workingDirectory,
                    false,
                    false,
                    System::exit);
            final var splits = testSplit.run();
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
}
