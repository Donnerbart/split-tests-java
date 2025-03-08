package de.donnerbart.split;

import de.donnerbart.split.model.Splits;
import de.donnerbart.split.model.TestCase;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.Set;

import static de.donnerbart.split.util.FormatUtil.formatTime;

public class TestSplit {

    private static final @NotNull Logger LOG = LoggerFactory.getLogger(TestSplit.class);

    private final @NotNull Set<TestCase> testCases;
    private final int splitTotal;
    private final @NotNull FormatOption formatOption;
    private final boolean debug;

    public TestSplit(
            final @NotNull Set<TestCase> testCases,
            final int splitTotal,
            final @NotNull FormatOption formatOption,
            final boolean debug) {
        this.testCases = testCases;
        this.splitTotal = splitTotal;
        this.formatOption = formatOption;
        this.debug = debug;
    }

    public @NotNull Splits split() {
        // split tests
        if (debug) {
            LOG.debug("Splitting {} tests", testCases.size());
        }
        final var splits = new Splits(splitTotal, formatOption);
        testCases.stream().sorted(Comparator.reverseOrder()).forEach(testCase -> {
            final var split = splits.add(testCase);
            if (debug) {
                LOG.debug("Adding test {} to split #{}", testCase.name(), split.index());
            }
        });

        if (debug) {
            if (splitTotal > 1) {
                final var fastestSplit = splits.getFastest();
                LOG.debug("Fastest test plan is #{} with {} tests ({})",
                        fastestSplit.formatIndex(),
                        fastestSplit.tests().size(),
                        formatTime(fastestSplit.totalRecordedTime()));
                final var slowestSplit = splits.getSlowest();
                LOG.debug("Slowest test plan is #{} with {} tests ({})",
                        slowestSplit.formatIndex(),
                        slowestSplit.tests().size(),
                        formatTime(slowestSplit.totalRecordedTime()));
                LOG.debug("Difference between the fastest and slowest test plan: {}",
                        formatTime(slowestSplit.totalRecordedTime() - fastestSplit.totalRecordedTime()));
            }
            LOG.debug("Test splits:");
            splits.forEach(split -> LOG.debug(split.toString()));
        }
        return splits;
    }
}
