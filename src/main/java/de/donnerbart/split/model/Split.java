package de.donnerbart.split.model;

import de.donnerbart.split.FormatOption;
import de.donnerbart.split.util.FormatUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class Split implements Comparable<Split> {

    private final @NotNull Set<TestCase> tests = new HashSet<>();
    private final @NotNull FormatOption formatOption;
    private final int index;

    private double totalRecordedTime;

    public Split(final @NotNull FormatOption formatOption, final int index) {
        this.formatOption = formatOption;
        this.index = index;
    }

    public void add(final @NotNull TestCase testCase) {
        tests.add(testCase);
        totalRecordedTime += testCase.time();
    }

    public int index() {
        return index;
    }

    public @NotNull String formatIndex() {
        return String.format("%02d", index);
    }

    public @NotNull Set<TestCase> tests() {
        return tests;
    }

    public @NotNull List<String> sortedTests() {
        return tests.stream() //
                .sorted(Comparator.reverseOrder()) //
                .map(TestCase::name) //
                .map(test -> switch (formatOption) {
                    case LIST -> test;
                    case GRADLE -> "--tests " + test;
                }).collect(Collectors.toList());
    }

    public double totalRecordedTime() {
        return totalRecordedTime;
    }

    @Override
    public int compareTo(final @NotNull Split o) {
        final var compareTime = Double.compare(totalRecordedTime, o.totalRecordedTime);
        if (compareTime != 0) {
            return compareTime;
        }
        final var compareTestCount = Double.compare(tests.size(), o.tests.size());
        if (compareTestCount != 0) {
            return compareTestCount;
        }
        return Double.compare(index, o.index);
    }

    @Override
    public boolean equals(final @Nullable Object o) {
        if (!(o instanceof final Split split)) {
            return false;
        }
        return index == split.index;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(index);
    }

    @Override
    public @NotNull String toString() {
        return "Split{" +
                "index=" +
                formatIndex() +
                ", totalRecordedTime=" +
                FormatUtil.formatTime(totalRecordedTime) +
                ", testCount=" +
                tests.size() +
                ", tests=" +
                tests.stream().map(TestCase::name).collect(Collectors.joining(", ")) +
                '}';
    }
}
