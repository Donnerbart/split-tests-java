package de.donnerbart.split.model;

import de.donnerbart.split.FormatOption;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

public class Splits {

    private final @NotNull List<Split> splits;

    public Splits(final int splitTotal, final @NotNull FormatOption formatOption) {
        this.splits = new ArrayList<>(splitTotal);
        for (int i = 0; i < splitTotal; i++) {
            splits.add(new Split(formatOption, i));
        }
    }

    public @NotNull Split add(final @NotNull TestCase testCase) {
        final var split = splits.stream().sorted().findFirst().orElseThrow();
        split.add(testCase);
        return split;
    }

    public @NotNull Split get(final int index) {
        return splits.get(index);
    }

    public @NotNull Split getFastest() {
        return splits.stream().min(Comparator.naturalOrder()).orElseThrow();
    }

    public @NotNull Split getSlowest() {
        return splits.stream().max(Comparator.naturalOrder()).orElseThrow();
    }

    public void forEach(final @NotNull Consumer<Split> consumer) {
        splits.stream().sorted(Comparator.reverseOrder()).forEach(consumer);
    }

    public int size() {
        return splits.size();
    }
}
