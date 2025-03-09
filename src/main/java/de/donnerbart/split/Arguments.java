package de.donnerbart.split;

import com.beust.jcommander.IDefaultProvider;
import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.converters.PathConverter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.Arrays;

class Arguments {

    @Parameter(names = {"--help", "-h"}, description = "Prints the usage.", help = true)
    boolean help;

    @Parameter(names = {"--split-index", "-i"}, description = "This test split index.", required = true, order = 0)
    int splitIndex;

    @Parameter(names = {"--split-total", "-t"},
               description = "Total number of test splits.",
               required = true,
               order = 1)
    int splitTotal;

    @Parameter(names = {"--glob", "-g"},
               description = "Glob pattern to find test files. Make sure to single-quote the pattern to avoid shell expansion.",
               required = true,
               order = 2)
    @SuppressWarnings("NotNullFieldNotInitialized")
    @NotNull String glob;

    @Parameter(names = {"--exclude-glob", "-e"},
               description = "Glob pattern to exclude test files. Make sure to single-quote the pattern to avoid shell expansion.")
    @Nullable String excludeGlob;

    @Parameter(names = {"--junit-glob", "-j"},
               description = "Glob pattern to find JUnit reports. Make sure to single-quote the pattern to avoid shell expansion.")
    @Nullable String junitGlob;

    @Parameter(names = {"--format", "-f"}, description = "The output format.", converter = FormatOptionConverter.class)
    @NotNull FormatOption formatOption = FormatOption.LIST;

    @Deprecated
    @Parameter(names = {"--average-time", "-a"},
               description = "This option is deprecated and should no longer be used. Use --newTestTimeOption instead.",
               hidden = true)
    boolean useAverageTimeForNewTests = false;

    @Parameter(names = {"--new-test-time", "-n"},
               description = "Configures the calculation of the test time for tests without JUnit reports.",
               converter = NewTestTimeOptionConverter.class)
    @NotNull NewTestTimeOption newTestTimeOption = NewTestTimeOption.AVERAGE;

    @Parameter(names = {"--working-directory", "-w"},
               description = "The working directory. Defaults to the current directory.",
               converter = WorkingDirectoryOptionConverter.class)
    @SuppressWarnings("NotNullFieldNotInitialized")
    @NotNull Path workingDirectory;

    @Parameter(names = {"--calculate-optimal-total-split", "-c"},
               description = "Calculates the optimal test split (only on the first split index). Logs a warning if --split-total does not match.")
    boolean calculateOptimalTotalSplit = false;

    @Parameter(names = {"--max-optimal-total-split-calculations", "-m"},
               description = "The maximum number of --calculate-optimal-total-split calculations.")
    int maxOptimalTotalSplitCalculations = 50;

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

    public static class NewTestTimeOptionConverter implements IStringConverter<NewTestTimeOption> {

        @Override
        public @NotNull NewTestTimeOption convert(final @NotNull String value) {
            return Arrays.stream(NewTestTimeOption.values())
                    .filter(option -> option.toString().equals(value))
                    .findFirst()
                    .orElseThrow();
        }
    }

    public static class WorkingDirectoryOptionConverter extends PathConverter {

        public WorkingDirectoryOptionConverter(final @NotNull String optionName) {
            super(optionName);
        }

        @Override
        public @NotNull Path convert(final @NotNull String value) {
            return super.convert(value).toAbsolutePath().normalize();
        }
    }

    public static class DefaultProvider implements IDefaultProvider {

        @Override
        public @Nullable String getDefaultValueFor(final @NotNull String optionName) {
            if (optionName.equals("--working-directory")) {
                return System.getProperty("user.dir");
            }
            return null;
        }
    }
}
