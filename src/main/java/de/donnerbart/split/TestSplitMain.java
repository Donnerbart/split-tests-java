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
        final var testSplit = new TestSplit(arguments.splitTotal,
                arguments.glob,
                arguments.excludeGlob,
                arguments.junitGlob,
                arguments.formatOption,
                arguments.newTestTimeOption,
                workingDirectory,
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
}
