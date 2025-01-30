package de.donnerbart.split;

import ch.qos.logback.classic.Level;
import com.beust.jcommander.JCommander;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Paths;

public class TestSplitMain {

    public static void main(final @Nullable String @NotNull [] args) throws Exception {
        final var arguments = new Arguments();
        final var jCommander = JCommander.newBuilder().addObject(arguments).build();
        jCommander.parse(args);
        if (arguments.help) {
            jCommander.usage();
            System.exit(0);
        }
        if (arguments.splitTotal < 1) {
            System.out.println("--split-total must be greater than 0");
            System.exit(1);
        }
        if (arguments.splitIndex > arguments.splitTotal - 1) {
            System.out.println("--split-index must lesser than --split-total");
            System.exit(1);
        }
        final var workingDirectory = arguments.workingDirectory != null ?
                arguments.workingDirectory.toAbsolutePath() :
                Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        if (!Files.exists(workingDirectory)) {
            System.out.println("Working directory does not exist: " + workingDirectory);
            System.exit(1);
        }
        if (arguments.debug) {
            final var root = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
            root.setLevel(Level.DEBUG);
        }

        final var testSplit = new TestSplit(arguments.splitIndex,
                arguments.splitTotal,
                arguments.glob,
                arguments.excludeGlob,
                arguments.junitGlob,
                workingDirectory,
                arguments.debug,
                System::exit);
        testSplit.run();
    }
}
