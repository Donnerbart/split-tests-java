package de.donnerbart.split;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static de.donnerbart.split.TestUtil.copyResourceToTarget;
import static java.nio.file.attribute.PosixFilePermission.OWNER_READ;
import static java.nio.file.attribute.PosixFilePermission.OWNER_WRITE;
import static org.assertj.core.api.Assertions.assertThat;

class TestSplitTest {

    private static final @NotNull Set<PosixFilePermission> PERMISSIONS = Set.of(OWNER_READ, OWNER_WRITE);

    @TempDir
    private @NotNull Path tmp;

    private final @NotNull AtomicReference<Integer> exitCode = new AtomicReference<>();

    @BeforeEach
    void setUp() throws Exception {
        final var projectFolder = tmp.resolve("example-project")
                .resolve("src")
                .resolve("main")
                .resolve("java")
                .resolve("de")
                .resolve("donnerbart")
                .resolve("example");
        Files.createDirectories(projectFolder);
        // ignored tests
        copyResourceToTarget(projectFolder, "tests/BaseTest.java", "BaseTest.java", PERMISSIONS);
        copyResourceToTarget(projectFolder, "tests/DisabledTest.java", "DisabledTest.java", PERMISSIONS);
        copyResourceToTarget(projectFolder, "tests/IgnoreTest.java", "IgnoreTest.java", PERMISSIONS);
        copyResourceToTarget(projectFolder, "tests/InterfaceTest.java", "InterfaceTest.java", PERMISSIONS);
        // valid tests
        copyResourceToTarget(projectFolder, "tests/FastTest.java", "FastTest.java", PERMISSIONS);
        copyResourceToTarget(projectFolder, "tests/NoTimingOneTest.java", "NoTimingOneTest.java", PERMISSIONS);
        copyResourceToTarget(projectFolder, "tests/NoTimingTwoTest.java", "NoTimingTwoTest.java", PERMISSIONS);
        copyResourceToTarget(projectFolder, "tests/SlowTest.java", "SlowTest.java", PERMISSIONS);
        copyResourceToTarget(projectFolder, "tests/SlowestTest.java", "SlowestTest.java", PERMISSIONS);

        final var reportFolder = tmp.resolve("junit-reports");
        copyResourceToTarget(reportFolder,
                "reports/TEST-de.donnerbart.example.DeletedTest.xml",
                "TEST-de.donnerbart.example.DeletedTest.xml",
                PERMISSIONS);
        copyResourceToTarget(reportFolder,
                "reports/TEST-de.donnerbart.example.FastTest.xml",
                "TEST-de.donnerbart.example.FastTest.xml",
                PERMISSIONS);
        copyResourceToTarget(reportFolder,
                "reports/TEST-de.donnerbart.example.SlowTest.xml",
                "TEST-de.donnerbart.example.SlowTest.xml",
                PERMISSIONS);
        copyResourceToTarget(reportFolder,
                "reports/TEST-de.donnerbart.example.SlowestTest.xml",
                "TEST-de.donnerbart.example.SlowestTest.xml",
                PERMISSIONS);
    }

    @Test
    void run_withoutJUnit_withOneSplit() throws Exception {
        final var splits = splitTests(1, false);
        assertThat(splits).hasSize(1).containsExactly( //
                List.of("de.donnerbart.example.FastTest",
                        "de.donnerbart.example.NoTimingOneTest",
                        "de.donnerbart.example.NoTimingTwoTest",
                        "de.donnerbart.example.SlowTest",
                        "de.donnerbart.example.SlowestTest"));
        assertThat(exitCode).hasNullValue();
    }

    @Test
    void run_withoutJUnit_withTwoSplits() throws Exception {
        final var splits = splitTests(2, false);
        assertThat(splits).hasSize(2).containsExactly( //
                List.of("de.donnerbart.example.FastTest",
                        "de.donnerbart.example.NoTimingTwoTest",
                        "de.donnerbart.example.SlowestTest"),
                List.of("de.donnerbart.example.NoTimingOneTest", "de.donnerbart.example.SlowTest"));
        assertThat(exitCode).hasNullValue();
    }

    @Test
    void run_withoutJUnit_withThreeSplits() throws Exception {
        final var splits = splitTests(3, false);
        assertThat(splits).hasSize(3).containsExactly( //
                List.of("de.donnerbart.example.FastTest", "de.donnerbart.example.SlowTest"),
                List.of("de.donnerbart.example.NoTimingOneTest", "de.donnerbart.example.SlowestTest"),
                List.of("de.donnerbart.example.NoTimingTwoTest"));
        assertThat(exitCode).hasNullValue();
    }

    @Test
    void run_withoutJUnit_withThreeSplits_withGradleFormat() throws Exception {
        final var splits = splitTests(3, false, "**/example-project/**/*Test.java", tmp, FormatOption.GRADLE);
        assertThat(splits).hasSize(3).containsExactly( //
                List.of("--tests de.donnerbart.example.FastTest", "--tests de.donnerbart.example.SlowTest"),
                List.of("--tests de.donnerbart.example.NoTimingOneTest", "--tests de.donnerbart.example.SlowestTest"),
                List.of("--tests de.donnerbart.example.NoTimingTwoTest"));
        assertThat(exitCode).hasNullValue();
    }

    @Test
    void run_withJUnit_withOneSplit() throws Exception {
        final var splits = splitTests(1, true);
        assertThat(splits).hasSize(1).containsExactly( //
                List.of("de.donnerbart.example.SlowestTest",
                        "de.donnerbart.example.SlowTest",
                        "de.donnerbart.example.FastTest",
                        "de.donnerbart.example.NoTimingOneTest",
                        "de.donnerbart.example.NoTimingTwoTest"));
        assertThat(exitCode).hasNullValue();
    }

    @Test
    void run_withJUnit_withTwoSplits() throws Exception {
        final var splits = splitTests(2, true);
        assertThat(splits).hasSize(2).containsExactly( //
                List.of("de.donnerbart.example.SlowestTest"),
                List.of("de.donnerbart.example.SlowTest",
                        "de.donnerbart.example.FastTest",
                        "de.donnerbart.example.NoTimingOneTest",
                        "de.donnerbart.example.NoTimingTwoTest"));
        assertThat(exitCode).hasNullValue();
    }

    @Test
    void run_withJUnit_withThreeSplits() throws Exception {
        final var splits = splitTests(3, true);
        assertThat(splits).hasSize(3).containsExactly( //
                List.of("de.donnerbart.example.SlowestTest"),
                List.of("de.donnerbart.example.SlowTest"),
                List.of("de.donnerbart.example.FastTest",
                        "de.donnerbart.example.NoTimingOneTest",
                        "de.donnerbart.example.NoTimingTwoTest"));
        assertThat(exitCode).hasNullValue();
    }

    @Test
    void run_withJUnit_withFourSplits() throws Exception {
        final var splits = splitTests(4, true);
        assertThat(splits).hasSize(4).containsExactly( //
                List.of("de.donnerbart.example.SlowestTest"),
                List.of("de.donnerbart.example.SlowTest"),
                List.of("de.donnerbart.example.FastTest"),
                List.of("de.donnerbart.example.NoTimingOneTest", "de.donnerbart.example.NoTimingTwoTest"));
        assertThat(exitCode).hasNullValue();
    }

    @Test
    void run_whitespaceClassDefinition() throws Exception {
        final var projectFolder =
                tmp.resolve("multiline-class-definition-project").resolve("src").resolve("main").resolve("java");
        copyResourceToTarget(projectFolder,
                "tests/WhitespaceClassDefinitionTest.java",
                "WhitespaceClassDefinitionTest.java",
                PERMISSIONS);

        final var split = new TestSplit(0,
                1,
                "**/multiline-class-definition-project/**/*Test.java",
                null,
                null,
                FormatOption.LIST,
                projectFolder,
                true,
                exitCode::set);

        assertThat(split.run()).containsExactly("de.donnerbart.example.WhitespaceClassDefinitionTest");
        assertThat(exitCode).hasNullValue();
    }

    @Test
    void run_thirdPartyLibrary() throws Exception {
        final var projectFolder =
                tmp.resolve("third-party-library-project").resolve("src").resolve("main").resolve("java");
        copyResourceToTarget(projectFolder,
                "tests/ThirdPartyLibraryTest.java",
                "ThirdPartyLibraryTest.java",
                PERMISSIONS);

        final var splits =
                splitTests(1, false, "**/third-party-library-project/**/*Test.java", projectFolder, FormatOption.LIST);
        assertThat(splits).hasSize(1).containsExactly(List.of("de.donnerbart.example.ThirdPartyLibraryTest"));
        assertThat(exitCode).hasNullValue();
    }

    @Test
    void run_noPackage() throws Exception {
        final var projectFolder = tmp.resolve("no-package-project").resolve("src").resolve("main").resolve("java");
        copyResourceToTarget(projectFolder, "tests/NoPackageTest.java", "NoPackageTest.java", PERMISSIONS);

        final var splits =
                splitTests(1, false, "**/no-package-project/**/*Test.java", projectFolder, FormatOption.LIST);
        assertThat(splits).hasSize(1).containsExactly(List.of("NoPackageTest"));
        assertThat(exitCode).hasNullValue();
    }

    @Test
    void run_noTests() throws Exception {
        final var projectFolder = tmp.resolve("no-tests-project").resolve("src").resolve("main").resolve("java");
        Files.createDirectories(projectFolder);

        final var splits = splitTests(1, false, "**/no-tests-project/**/*Test.java", projectFolder, FormatOption.LIST);
        assertThat(splits).hasSize(1).containsExactly(List.of());
        assertThat(exitCode).hasValue(1);
    }

    @Test
    void run_noClassName() throws Exception {
        final var projectFolder = tmp.resolve("no-classname-project").resolve("src").resolve("main").resolve("java");
        copyResourceToTarget(projectFolder, "tests/NoClassNameTest.java", "NoClassNameTest.java", PERMISSIONS);

        final var splits =
                splitTests(1, false, "**/no-classname-project/**/*Test.java", projectFolder, FormatOption.LIST);
        assertThat(splits).hasSize(1).containsExactly(List.of());
        assertThat(exitCode).hasValue(1);
    }

    private @NotNull List<List<String>> splitTests(final int splitTotal, final boolean withJUnit) throws Exception {
        return splitTests(splitTotal, withJUnit, "**/example-project/**/*Test.java", tmp, FormatOption.LIST);
    }

    private @NotNull List<List<String>> splitTests(
            final int splitTotal,
            final boolean withJUnit,
            final @NotNull String glob,
            final @NotNull Path workingDir,
            final @NotNull FormatOption format) throws Exception {
        final var splits = new ArrayList<List<String>>();
        for (int index = 0; index < splitTotal; index++) {
            final var split = new TestSplit(index,
                    splitTotal,
                    glob,
                    "**/example-project/**/*Abstract*.java",
                    withJUnit ? "**/junit-reports/*.xml" : null,
                    format,
                    workingDir,
                    true,
                    exitCode::set);
            splits.add(split.run());
        }
        return splits;
    }
}
