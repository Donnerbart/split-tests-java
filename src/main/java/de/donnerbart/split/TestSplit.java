package de.donnerbart.split;

import de.donnerbart.split.model.Splits;
import de.donnerbart.split.model.TestCase;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.NOPLogger;

import java.util.Comparator;
import java.util.Set;

import static de.donnerbart.split.util.FormatUtil.formatTime;

public class TestSplit {

    private final @NotNull Set<TestCase> testCases;
    private final int splitTotal;
    private final @NotNull FormatOption formatOption;
    private final @NotNull Logger log;
    private final boolean debug;

    public TestSplit(
            final @NotNull Set<TestCase> testCases,
            final int splitTotal,
            final @NotNull FormatOption formatOption,
            final boolean log,
            final boolean debug) {
        this.testCases = testCases;
        this.splitTotal = splitTotal;
        this.formatOption = formatOption;
        this.log = log ? LoggerFactory.getLogger(TestSplit.class) : NOPLogger.NOP_LOGGER;
        this.debug = debug;
    }

    public @NotNull Splits split() {
        // split tests
        log.debug("Splitting {} tests", testCases.size());
        final var splits = new Splits(splitTotal, formatOption);
        testCases.stream().sorted(Comparator.reverseOrder()).forEach(testCase -> {
            final var split = splits.add(testCase);
            log.debug("Adding test {} to split #{}", testCase.name(), split.index());
        });

        if (debug) {
            if (splitTotal > 1) {
                final var fastestSplit = splits.getFastest();
                log.debug("Fastest test plan is #{} with {} tests ({})",
                        fastestSplit.formatIndex(),
                        fastestSplit.tests().size(),
                        formatTime(fastestSplit.totalRecordedTime()));
                final var slowestSplit = splits.getSlowest();
                log.debug("Slowest test plan is #{} with {} tests ({})",
                        slowestSplit.formatIndex(),
                        slowestSplit.tests().size(),
                        formatTime(slowestSplit.totalRecordedTime()));
                log.debug("Difference between the fastest and slowest test plan: {}",
                        formatTime(slowestSplit.totalRecordedTime() - fastestSplit.totalRecordedTime()));
            }
            log.debug("Test splits:");
            splits.forEach(split -> log.debug(split.toString()));
        }
        return splits;
    }
}
