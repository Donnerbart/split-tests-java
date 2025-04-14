package de.donnerbart.split;

import org.jetbrains.annotations.NotNull;

public enum NewTestTimeOption {

    ZERO("zero"),
    AVERAGE("average"),
    MIN("min"),
    MAX("max");

    private final @NotNull String parameterValue;

    NewTestTimeOption(final @NotNull String parameterValue) {
        this.parameterValue = parameterValue;
    }

    @Override
    public @NotNull String toString() {
        return parameterValue;
    }
}
