package de.donnerbart.split;

import ch.qos.logback.classic.Level;
import com.beust.jcommander.JCommander;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

public class TestSplitMain {

    public static void main(final @Nullable String @NotNull [] args) throws Exception {
        final var arguments = new Arguments();
        final var jCommander = JCommander.newBuilder().addObject(arguments).build();
        jCommander.parse(args);
        if (arguments.help) {
            jCommander.usage();
            System.exit(0);
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
            System.exit(1);
        }
        final var testSplit = new TestSplit(arguments.splitIndex,
                arguments.splitTotal,
                arguments.glob,
                arguments.excludeGlob,
                arguments.junitGlob,
                arguments.format,
                workingDirectory,
                arguments.debug,
                System::exit);
        System.out.print(String.join(" ", testSplit.run()));
    }

    @VisibleForTesting
    static boolean validateArguments(final @NotNull Arguments arguments, final @NotNull Path workingDirectory) {
        if (arguments.splitTotal < 1) {
            System.out.println("--split-total must be greater than 0");
            return false;
        }
        if (arguments.splitIndex > arguments.splitTotal - 1) {
            System.out.println("--split-index must lesser than --split-total");
            return false;
        }
        if (!Files.exists(workingDirectory)) {
            System.out.println("Working directory does not exist: " + arguments.workingDirectory);
            return false;
        }
        return true;
    }
}
