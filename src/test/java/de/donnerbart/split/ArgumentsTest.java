package de.donnerbart.split;

import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ArgumentsTest {

    @Test
    void formatOptionConvert() {
        final var converter = new Arguments.FormatOptionConverter();
        assertThat(converter.convert("list")).isEqualTo(FormatOption.LIST);
        assertThat(converter.convert("gradle")).isEqualTo(FormatOption.GRADLE);
        assertThatThrownBy(() -> converter.convert("unknown")).isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void newTestTimeOptionConvert() {
        final var converter = new Arguments.NewTestTimeOptionConverter();
        assertThat(converter.convert("zero")).isEqualTo(NewTestTimeOption.ZERO);
        assertThat(converter.convert("average")).isEqualTo(NewTestTimeOption.AVERAGE);
        assertThat(converter.convert("min")).isEqualTo(NewTestTimeOption.MIN);
        assertThat(converter.convert("max")).isEqualTo(NewTestTimeOption.MAX);
        assertThatThrownBy(() -> converter.convert("unknown")).isInstanceOf(NoSuchElementException.class);
    }
}
