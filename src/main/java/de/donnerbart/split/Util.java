package de.donnerbart.split;

import org.jetbrains.annotations.NotNull;

public class Util {

    private Util() {
    }

    static @NotNull String formatIndex(final int index) {
        return String.format("%02d", index);
    }

    static @NotNull String formatTime(final double time) {
        final var minutes = (int) Math.floor(time / 60d);
        final var seconds = Math.round(time - (minutes * 60));
        return String.format("%02dm%02ds", minutes, seconds);
    }
}
