package de.donnerbart.split;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

record TestCase(@NotNull String name, double time) implements Comparable<TestCase> {

    @Override
    public int compareTo(final @NotNull TestCase o) {
        final var compareTime = Double.compare(time, o.time);
        if (compareTime != 0) {
            return compareTime;
        }
        return o.name.compareTo(name);
    }

    @Override
    public boolean equals(final @Nullable Object o) {
        if (!(o instanceof final TestCase testCase)) {
            return false;
        }
        return Objects.equals(name, testCase.name);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name);
    }
}
