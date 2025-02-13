package de.donnerbart.split;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.Parameter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.Arrays;

class Arguments {

    @Parameter(names = {"--help", "-h"}, description = "Prints the usage.", help = true)
    boolean help;

    @Parameter(names = {"--split-index", "-i"}, description = "This test split index.", required = true)
    int splitIndex;

    @Parameter(names = {"--split-total", "-t"}, description = "Total number of test splits.", required = true)
    int splitTotal;

    @Parameter(names = {"--glob", "-g"},
               description = "Glob pattern to find test files. Make sure to single-quote the pattern to avoid shell expansion.",
               required = true)
    @SuppressWarnings("NotNullFieldNotInitialized")
    @NotNull String glob;

    @Parameter(names = {"--exclude-glob", "-e"},
               description = "Glob pattern to exclude test files. Make sure to single-quote the pattern to avoid shell expansion.")
    @Nullable String excludeGlob;

    @Parameter(names = {"--junit-glob", "-j"},
               description = "Glob pattern to find JUnit reports. Make sure to single-quote the pattern to avoid shell expansion.")
    @Nullable String junitGlob;

    @Parameter(names = {"--format", "-f"}, description = "The output format.", converter = FormatOptionConverter.class)
    @NotNull FormatOption format = FormatOption.LIST;

    @Parameter(names = {"--average-time", "-a"},
               description = "Use the average test time from tests with JUnit reports for tests without JUnit reports.")
    boolean useAverageTimeForNewTests = false;

    @Parameter(names = {"--working-directory", "-w"},
               description = "The working directory. Defaults to the current directory.")
    @Nullable Path workingDirectory;

    @Parameter(names = {"--debug", "-d"}, description = "Enables debug logging.")
    boolean debug = false;

    public static class FormatOptionConverter implements IStringConverter<FormatOption> {

        @Override
        public @NotNull FormatOption convert(final @NotNull String value) {
            return Arrays.stream(FormatOption.values())
                    .filter(option -> option.toString().equals(value))
                    .findFirst()
                    .orElseThrow();
        }
    }
}
