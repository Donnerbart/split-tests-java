package de.donnerbart.split;

import com.beust.jcommander.JCommander;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class TestSplitMainTest {

    private final @NotNull Arguments arguments = new Arguments();
    private final @NotNull JCommander jCommander = JCommander.newBuilder().addObject(arguments).build();

    @TempDir
    private @NotNull Path tmp;

    @Test
    void validateArguments() {
        jCommander.parse("-g", "**/*Test.java", "-t", "1", "-i", "0");
        assertThat(TestSplitMain.validateArguments(arguments, tmp)).isTrue();
    }

    @Test
    void validateArguments_withZeroSplitTotal() {
        jCommander.parse("-g", "**/*Test.java", "-t", "0", "-i", "0");
        assertThat(TestSplitMain.validateArguments(arguments, tmp)).isFalse();
    }

    @Test
    void validateArguments_withNegativeSplitTotal() {
        jCommander.parse("-g", "**/*Test.java", "-t", "-1", "-i", "0");
        assertThat(TestSplitMain.validateArguments(arguments, tmp)).isFalse();
    }

    @Test
    void validateArguments_withTooSmallSplitIndex() {
        jCommander.parse("-g", "**/*Test.java", "-t", "1", "-i", "1");
        assertThat(TestSplitMain.validateArguments(arguments, tmp)).isFalse();
    }

    @Test
    void validateArguments_withInvalidWorkingDirectory() {
        jCommander.parse("-g", "**/*Test.java", "-t", "1", "-i", "0");
        assertThat(TestSplitMain.validateArguments(arguments, tmp.resolve("does-not-exist"))).isFalse();
    }
}
