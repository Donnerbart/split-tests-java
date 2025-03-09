package de.donnerbart.split;

import de.donnerbart.split.model.TestCase;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static de.donnerbart.split.TestUtil.copyResourceToTarget;
import static java.nio.file.attribute.PosixFilePermission.OWNER_READ;
import static java.nio.file.attribute.PosixFilePermission.OWNER_WRITE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.byLessThan;

class TestLoaderTest {

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
        copyResourceToTarget(projectFolder, "tests/AbstractTest.java", "AbstractTest.java", PERMISSIONS);
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
    void load_withoutJUnit() throws Exception {
        final var testCases = loadTests(false, NewTestTimeOption.ZERO);
        assertThat(testCases).satisfiesExactlyInAnyOrder( //
                testCase -> assertTestCase(testCase, new TestCase("de.donnerbart.example.FastTest", 0d)),
                testCase -> assertTestCase(testCase, new TestCase("de.donnerbart.example.SlowTest", 0d)),
                testCase -> assertTestCase(testCase, new TestCase("de.donnerbart.example.SlowestTest", 0d)),
                testCase -> assertTestCase(testCase, new TestCase("de.donnerbart.example.NoTimingOneTest", 0d)),
                testCase -> assertTestCase(testCase, new TestCase("de.donnerbart.example.NoTimingTwoTest", 0d)));
        assertThat(exitCode).hasNullValue();
    }

    @Test
    void load_withJUnit() throws Exception {
        final var testCases = loadTests(true, NewTestTimeOption.ZERO);
        assertThat(testCases).satisfiesExactlyInAnyOrder( //
                testCase -> assertTestCase(testCase, new TestCase("de.donnerbart.example.FastTest", 2.374d)),
                testCase -> assertTestCase(testCase, new TestCase("de.donnerbart.example.SlowTest", 12.386d)),
                testCase -> assertTestCase(testCase, new TestCase("de.donnerbart.example.SlowestTest", 153.457d)),
                testCase -> assertTestCase(testCase, new TestCase("de.donnerbart.example.NoTimingOneTest", 0d)),
                testCase -> assertTestCase(testCase, new TestCase("de.donnerbart.example.NoTimingTwoTest", 0d)));
        assertThat(exitCode).hasNullValue();
    }

    @Test
    void load_withJUnit_withAverageTestTime() throws Exception {
        final var testCases = loadTests(true, NewTestTimeOption.AVERAGE);
        assertThat(testCases).satisfiesExactlyInAnyOrder( //
                testCase -> assertTestCase(testCase, new TestCase("de.donnerbart.example.FastTest", 2.374d)),
                testCase -> assertTestCase(testCase, new TestCase("de.donnerbart.example.SlowTest", 12.386d)),
                testCase -> assertTestCase(testCase, new TestCase("de.donnerbart.example.SlowestTest", 153.457d)),
                testCase -> assertTestCase(testCase, new TestCase("de.donnerbart.example.NoTimingOneTest", 56.0723d)),
                testCase -> assertTestCase(testCase, new TestCase("de.donnerbart.example.NoTimingTwoTest", 56.0723d)));
        assertThat(exitCode).hasNullValue();
    }

    @Test
    void load_withJUnit_withMinTestTime() throws Exception {
        final var testCases = loadTests(true, NewTestTimeOption.MIN);
        assertThat(testCases).satisfiesExactlyInAnyOrder( //
                testCase -> assertTestCase(testCase, new TestCase("de.donnerbart.example.FastTest", 2.374d)),
                testCase -> assertTestCase(testCase, new TestCase("de.donnerbart.example.SlowTest", 12.386d)),
                testCase -> assertTestCase(testCase, new TestCase("de.donnerbart.example.SlowestTest", 153.457d)),
                testCase -> assertTestCase(testCase, new TestCase("de.donnerbart.example.NoTimingOneTest", 2.374d)),
                testCase -> assertTestCase(testCase, new TestCase("de.donnerbart.example.NoTimingTwoTest", 2.374d)));
        assertThat(exitCode).hasNullValue();
    }

    @Test
    void load_withJUnit_withMaxTestTime() throws Exception {
        final var testCases = loadTests(true, NewTestTimeOption.MAX);
        assertThat(testCases).satisfiesExactlyInAnyOrder( //
                testCase -> assertTestCase(testCase, new TestCase("de.donnerbart.example.FastTest", 2.374d)),
                testCase -> assertTestCase(testCase, new TestCase("de.donnerbart.example.SlowTest", 12.386d)),
                testCase -> assertTestCase(testCase, new TestCase("de.donnerbart.example.SlowestTest", 153.457d)),
                testCase -> assertTestCase(testCase, new TestCase("de.donnerbart.example.NoTimingOneTest", 153.457d)),
                testCase -> assertTestCase(testCase, new TestCase("de.donnerbart.example.NoTimingTwoTest", 153.457d)));
        assertThat(exitCode).hasNullValue();
    }

    @Test
    void load_whitespaceClassDefinition() throws Exception {
        final var projectFolder =
                tmp.resolve("multiline-class-definition-project").resolve("src").resolve("main").resolve("java");
        copyResourceToTarget(projectFolder,
                "tests/WhitespaceClassDefinitionTest.java",
                "WhitespaceClassDefinitionTest.java",
                PERMISSIONS);

        final var testLoader = new TestLoader("**/multiline-class-definition-project/**/*Test.java",
                null,
                null,
                NewTestTimeOption.ZERO,
                tmp,
                exitCode::set);
        final var testCases = testLoader.load();
        assertThat(testCases).singleElement().satisfies(testCase -> assertTestCase(testCase, //
                new TestCase("de.donnerbart.example.WhitespaceClassDefinitionTest", 0d)));
        assertThat(exitCode).hasNullValue();
    }

    @Test
    void load_thirdPartyLibrary() throws Exception {
        final var projectFolder =
                tmp.resolve("third-party-library-project").resolve("src").resolve("main").resolve("java");
        copyResourceToTarget(projectFolder,
                "tests/ThirdPartyLibraryTest.java",
                "ThirdPartyLibraryTest.java",
                PERMISSIONS);

        final var testCases =
                loadTests(false, NewTestTimeOption.ZERO, "**/third-party-library-project/**/*Test.java", projectFolder);
        assertThat(testCases).singleElement().satisfies(testCase -> assertTestCase(testCase, //
                new TestCase("de.donnerbart.example.ThirdPartyLibraryTest", 0d)));
        assertThat(exitCode).hasNullValue();
    }

    @Test
    void load_noPackage() throws Exception {
        final var projectFolder = tmp.resolve("no-package-project").resolve("src").resolve("main").resolve("java");
        copyResourceToTarget(projectFolder, "tests/NoPackageTest.java", "NoPackageTest.java", PERMISSIONS);

        final var testCases =
                loadTests(false, NewTestTimeOption.ZERO, "**/no-package-project/**/*Test.java", projectFolder);
        assertThat(testCases).singleElement().satisfies(testCase -> assertTestCase(testCase, //
                new TestCase("NoPackageTest", 0d)));
        assertThat(exitCode).hasNullValue();
    }

    @ParameterizedTest
    @EnumSource(NewTestTimeOption.class)
    void load_noTests(final @NotNull NewTestTimeOption newTestTimeOption) throws Exception {
        final var projectFolder = tmp.resolve("no-tests-project").resolve("src").resolve("main").resolve("java");
        Files.createDirectories(projectFolder);

        final var testCases = loadTests(false, newTestTimeOption, "**/no-tests-project/**/*Test.java", projectFolder);
        assertThat(testCases).isEmpty();
        assertThat(exitCode).hasValue(1);
    }

    @Test
    void load_noClassName() throws Exception {
        final var projectFolder = tmp.resolve("no-classname-project").resolve("src").resolve("main").resolve("java");
        copyResourceToTarget(projectFolder, "tests/NoClassNameTest.java", "NoClassNameTest.java", PERMISSIONS);

        final var testCases =
                loadTests(false, NewTestTimeOption.ZERO, "**/no-classname-project/**/*Test.java", projectFolder);
        assertThat(testCases).isEmpty();
        assertThat(exitCode).hasValue(1);
    }

    private @NotNull Set<TestCase> loadTests(
            final boolean withJUnit,
            final @NotNull NewTestTimeOption newTestTimeOption) throws Exception {
        return loadTests(withJUnit, newTestTimeOption, "**/example-project/**/*Test.java", tmp);
    }

    private @NotNull Set<TestCase> loadTests(
            final boolean withJUnit,
            final @NotNull NewTestTimeOption newTestTimeOption,
            final @NotNull String glob,
            final @NotNull Path workingDir) throws Exception {
        final var testLoader = new TestLoader(glob,
                "**/example-project/**/*Abstract*.java",
                withJUnit ? "**/junit-reports/*.xml" : null,
                newTestTimeOption,
                workingDir,
                exitCode::set);
        return testLoader.load();
    }

    private static void assertTestCase(final @NotNull TestCase actual, final @NotNull TestCase expected) {
        assertThat(actual.name()).isEqualTo(expected.name());
        assertThat(actual.time()).isEqualTo(expected.time(), byLessThan(0.0001d));
    }
}
