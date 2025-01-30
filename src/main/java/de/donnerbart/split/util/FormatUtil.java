package de.donnerbart.split.util;

import org.jetbrains.annotations.NotNull;

public class FormatUtil {

    private FormatUtil() {
    }

    public static @NotNull String formatTime(final double time) {
        final var minutes = (int) Math.floor(time / 60d);
        final var seconds = Math.round(time - (minutes * 60));
        return String.format("%02dm%02ds", minutes, seconds);
    }
}
