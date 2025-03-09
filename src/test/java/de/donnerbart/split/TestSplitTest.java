package de.donnerbart.split;

import de.donnerbart.split.model.TestCase;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class TestSplitTest {

    private final @NotNull Set<TestCase> testCasesWithoutTiming = new HashSet<>();
    private final @NotNull Set<TestCase> testCasesWithTiming = new HashSet<>();

    @BeforeEach
    void setUp() {
        testCasesWithoutTiming.add(new TestCase("de.donnerbart.example.FastTest", 0d));
        testCasesWithoutTiming.add(new TestCase("de.donnerbart.example.SlowTest", 0d));
        testCasesWithoutTiming.add(new TestCase("de.donnerbart.example.SlowestTest", 0d));
        testCasesWithoutTiming.add(new TestCase("de.donnerbart.example.NoTimingOneTest", 0d));
        testCasesWithoutTiming.add(new TestCase("de.donnerbart.example.NoTimingTwoTest", 0d));

        testCasesWithTiming.add(new TestCase("de.donnerbart.example.FastTest", 2.374d));
        testCasesWithTiming.add(new TestCase("de.donnerbart.example.SlowTest", 12.386d));
        testCasesWithTiming.add(new TestCase("de.donnerbart.example.SlowestTest", 153.457d));
        testCasesWithTiming.add(new TestCase("de.donnerbart.example.NoTimingOneTest", 0d));
        testCasesWithTiming.add(new TestCase("de.donnerbart.example.NoTimingTwoTest", 0d));
    }

    @Test
    void split_noTests() {
        final var splits = splitTests(Set.of(), 1, FormatOption.LIST);
        assertThat(splits).containsExactly(List.of());
    }

    @Test
    void split_withoutTiming_withOneSplit() {
        final var splits = splitTests(testCasesWithoutTiming, 1, FormatOption.LIST);
        assertThat(splits).containsExactly( //
                List.of("de.donnerbart.example.FastTest",
                        "de.donnerbart.example.NoTimingOneTest",
                        "de.donnerbart.example.NoTimingTwoTest",
                        "de.donnerbart.example.SlowTest",
                        "de.donnerbart.example.SlowestTest"));
    }

    @Test
    void split_withoutTiming_withTwoSplits() {
        final var splits = splitTests(testCasesWithoutTiming, 2, FormatOption.LIST);
        assertThat(splits).containsExactly( //
                List.of("de.donnerbart.example.FastTest",
                        "de.donnerbart.example.NoTimingTwoTest",
                        "de.donnerbart.example.SlowestTest"),
                List.of("de.donnerbart.example.NoTimingOneTest", "de.donnerbart.example.SlowTest"));
    }

    @Test
    void split_withoutTiming_withThreeSplits() {
        final var splits = splitTests(testCasesWithoutTiming, 3, FormatOption.LIST);
        assertThat(splits).containsExactly( //
                List.of("de.donnerbart.example.FastTest", "de.donnerbart.example.SlowTest"),
                List.of("de.donnerbart.example.NoTimingOneTest", "de.donnerbart.example.SlowestTest"),
                List.of("de.donnerbart.example.NoTimingTwoTest"));
    }

    @Test
    void split_withoutTiming_withThreeSplits_withGradleFormat() {
        final var splits = splitTests(testCasesWithoutTiming, 3, FormatOption.GRADLE);
        assertThat(splits).containsExactly( //
                List.of("--tests de.donnerbart.example.FastTest", "--tests de.donnerbart.example.SlowTest"),
                List.of("--tests de.donnerbart.example.NoTimingOneTest", "--tests de.donnerbart.example.SlowestTest"),
                List.of("--tests de.donnerbart.example.NoTimingTwoTest"));
    }

    @Test
    void split_withJUnit_withOneSplit() {
        final var splits = splitTests(testCasesWithTiming, 1, FormatOption.LIST);
        assertThat(splits).containsExactly( //
                List.of("de.donnerbart.example.SlowestTest",
                        "de.donnerbart.example.SlowTest",
                        "de.donnerbart.example.FastTest",
                        "de.donnerbart.example.NoTimingOneTest",
                        "de.donnerbart.example.NoTimingTwoTest"));
    }

    @Test
    void split_withJUnit_withTwoSplits() {
        final var splits = splitTests(testCasesWithTiming, 2, FormatOption.LIST);
        assertThat(splits).containsExactly( //
                List.of("de.donnerbart.example.SlowestTest"),
                List.of("de.donnerbart.example.SlowTest",
                        "de.donnerbart.example.FastTest",
                        "de.donnerbart.example.NoTimingOneTest",
                        "de.donnerbart.example.NoTimingTwoTest"));
    }

    @Test
    void split_withJUnit_withThreeSplits() {
        final var splits = splitTests(testCasesWithTiming, 3, FormatOption.LIST);
        assertThat(splits).containsExactly( //
                List.of("de.donnerbart.example.SlowestTest"),
                List.of("de.donnerbart.example.SlowTest"),
                List.of("de.donnerbart.example.FastTest",
                        "de.donnerbart.example.NoTimingOneTest",
                        "de.donnerbart.example.NoTimingTwoTest"));
    }

    @Test
    void split_withJUnit_withFourSplits() {
        final var splits = splitTests(testCasesWithTiming, 4, FormatOption.LIST);
        assertThat(splits).containsExactly( //
                List.of("de.donnerbart.example.SlowestTest"),
                List.of("de.donnerbart.example.SlowTest"),
                List.of("de.donnerbart.example.FastTest"),
                List.of("de.donnerbart.example.NoTimingOneTest", "de.donnerbart.example.NoTimingTwoTest"));
    }

    @Test
    void split_withJUnit_withAverageTestTime_withOneSplit() {
        updateNoTimingTests(testCasesWithTiming, 56.0723d);
        final var splits = splitTests(testCasesWithTiming, 1, FormatOption.LIST);
        assertThat(splits).containsExactly( //
                List.of("de.donnerbart.example.SlowestTest",
                        "de.donnerbart.example.NoTimingOneTest",
                        "de.donnerbart.example.NoTimingTwoTest",
                        "de.donnerbart.example.SlowTest",
                        "de.donnerbart.example.FastTest"));
    }

    @Test
    void split_withJUnit_withAverageTestTime_withTwoSplits() {
        updateNoTimingTests(testCasesWithTiming, 56.0723d);
        final var splits = splitTests(testCasesWithTiming, 2, FormatOption.LIST);
        assertThat(splits).containsExactly( //
                List.of("de.donnerbart.example.SlowestTest"),
                List.of("de.donnerbart.example.NoTimingOneTest",
                        "de.donnerbart.example.NoTimingTwoTest",
                        "de.donnerbart.example.SlowTest",
                        "de.donnerbart.example.FastTest"));
    }

    @Test
    void split_withJUnit_withAverageTestTime_withThreeSplits() {
        updateNoTimingTests(testCasesWithTiming, 56.0723d);
        final var splits = splitTests(testCasesWithTiming, 3, FormatOption.LIST);
        assertThat(splits).containsExactly( //
                List.of("de.donnerbart.example.SlowestTest"),
                List.of("de.donnerbart.example.NoTimingOneTest", "de.donnerbart.example.SlowTest"),
                List.of("de.donnerbart.example.NoTimingTwoTest", "de.donnerbart.example.FastTest"));
    }

    @Test
    void split_withJUnit_withAverageTestTime_withFourSplits() {
        updateNoTimingTests(testCasesWithTiming, 56.0723d);
        final var splits = splitTests(testCasesWithTiming, 4, FormatOption.LIST);
        assertThat(splits).containsExactly( //
                List.of("de.donnerbart.example.SlowestTest"),
                List.of("de.donnerbart.example.NoTimingOneTest"),
                List.of("de.donnerbart.example.NoTimingTwoTest"),
                List.of("de.donnerbart.example.SlowTest", "de.donnerbart.example.FastTest"));
    }

    @Test
    void split_withJUnit_withMinTestTime_withOneSplit() {
        updateNoTimingTests(testCasesWithTiming, 2.374d);
        final var splits = splitTests(testCasesWithTiming, 1, FormatOption.LIST);
        assertThat(splits).containsExactly( //
                List.of("de.donnerbart.example.SlowestTest",
                        "de.donnerbart.example.SlowTest",
                        "de.donnerbart.example.FastTest",
                        "de.donnerbart.example.NoTimingOneTest",
                        "de.donnerbart.example.NoTimingTwoTest"));
    }

    @Test
    void split_withJUnit_withMinTestTime_withTwoSplits() {
        updateNoTimingTests(testCasesWithTiming, 2.374d);
        final var splits = splitTests(testCasesWithTiming, 2, FormatOption.LIST);
        assertThat(splits).containsExactly( //
                List.of("de.donnerbart.example.SlowestTest"),
                List.of("de.donnerbart.example.SlowTest",
                        "de.donnerbart.example.FastTest",
                        "de.donnerbart.example.NoTimingOneTest",
                        "de.donnerbart.example.NoTimingTwoTest"));
    }

    @Test
    void split_withJUnit_withMinTestTime_withThreeSplits() {
        updateNoTimingTests(testCasesWithTiming, 2.374d);
        final var splits = splitTests(testCasesWithTiming, 3, FormatOption.LIST);
        assertThat(splits).containsExactly( //
                List.of("de.donnerbart.example.SlowestTest"),
                List.of("de.donnerbart.example.SlowTest"),
                List.of("de.donnerbart.example.FastTest",
                        "de.donnerbart.example.NoTimingOneTest",
                        "de.donnerbart.example.NoTimingTwoTest"));
    }

    @Test
    void split_withJUnit_withMinTestTime_withFourSplits() {
        updateNoTimingTests(testCasesWithTiming, 2.374d);
        final var splits = splitTests(testCasesWithTiming, 4, FormatOption.LIST);
        assertThat(splits).containsExactly( //
                List.of("de.donnerbart.example.SlowestTest"),
                List.of("de.donnerbart.example.SlowTest"),
                List.of("de.donnerbart.example.FastTest", "de.donnerbart.example.NoTimingTwoTest"),
                List.of("de.donnerbart.example.NoTimingOneTest"));
    }

    @Test
    void split_withJUnit_withMaxTestTime_withOneSplit() {
        updateNoTimingTests(testCasesWithTiming, 153.457d);
        final var splits = splitTests(testCasesWithTiming, 1, FormatOption.LIST);
        assertThat(splits).containsExactly( //
                List.of("de.donnerbart.example.NoTimingOneTest",
                        "de.donnerbart.example.NoTimingTwoTest",
                        "de.donnerbart.example.SlowestTest",
                        "de.donnerbart.example.SlowTest",
                        "de.donnerbart.example.FastTest"));
    }

    @Test
    void split_withJUnit_withMaxTestTime_withTwoSplits() {
        updateNoTimingTests(testCasesWithTiming, 153.457d);
        final var splits = splitTests(testCasesWithTiming, 2, FormatOption.LIST);
        assertThat(splits).containsExactly( //
                List.of("de.donnerbart.example.NoTimingOneTest", "de.donnerbart.example.SlowestTest"),
                List.of("de.donnerbart.example.NoTimingTwoTest",
                        "de.donnerbart.example.SlowTest",
                        "de.donnerbart.example.FastTest"));
    }

    @Test
    void split_withJUnit_withMaxTestTime_withThreeSplits() {
        updateNoTimingTests(testCasesWithTiming, 153.457d);
        final var splits = splitTests(testCasesWithTiming, 3, FormatOption.LIST);
        assertThat(splits).containsExactly( //
                List.of("de.donnerbart.example.NoTimingOneTest", "de.donnerbart.example.SlowTest"),
                List.of("de.donnerbart.example.NoTimingTwoTest", "de.donnerbart.example.FastTest"),
                List.of("de.donnerbart.example.SlowestTest"));
    }

    @Test
    void split_withJUnit_withMaxTestTime_withFourSplits() {
        updateNoTimingTests(testCasesWithTiming, 153.457d);
        final var splits = splitTests(testCasesWithTiming, 4, FormatOption.LIST);
        assertThat(splits).containsExactly( //
                List.of("de.donnerbart.example.NoTimingOneTest"),
                List.of("de.donnerbart.example.NoTimingTwoTest"),
                List.of("de.donnerbart.example.SlowestTest"),
                List.of("de.donnerbart.example.SlowTest", "de.donnerbart.example.FastTest"));
    }

    private static @NotNull List<List<String>> splitTests(
            final @NotNull Set<TestCase> testCases,
            final int splitTotal,
            final @NotNull FormatOption formatOption) {
        final var testSplit = new TestSplit(testCases, splitTotal, formatOption, true);
        final var splits = testSplit.split();
        final var result = new ArrayList<List<String>>(splitTotal);
        for (int index = 0; index < splitTotal; index++) {
            result.add(splits.get(index).sortedTests());
        }
        return result;
    }

    private static void updateNoTimingTests(final @NotNull Set<TestCase> testCases, final double time) {
        testCases.remove(new TestCase("de.donnerbart.example.NoTimingOneTest", 0d));
        testCases.add(new TestCase("de.donnerbart.example.NoTimingOneTest", time));
        testCases.remove(new TestCase("de.donnerbart.example.NoTimingTwoTest", 0d));
        testCases.add(new TestCase("de.donnerbart.example.NoTimingTwoTest", time));
    }
}
