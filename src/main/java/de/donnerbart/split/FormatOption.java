package de.donnerbart.split;

import org.jetbrains.annotations.NotNull;

public enum FormatOption {

    LIST("list"),
    GRADLE("gradle");

    private final @NotNull String parameterValue;

    FormatOption(final @NotNull String parameterValue) {
        this.parameterValue = parameterValue;
    }

    @Override
    public String toString() {
        return parameterValue;
    }
}
