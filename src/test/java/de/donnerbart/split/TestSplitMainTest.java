package de.donnerbart.split;

import com.beust.jcommander.JCommander;
import de.donnerbart.split.model.TestCase;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.time.Instant;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static de.donnerbart.split.TestUtil.copyResourceToTarget;
import static java.nio.file.attribute.PosixFilePermission.OWNER_READ;
import static java.nio.file.attribute.PosixFilePermission.OWNER_WRITE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

class TestSplitMainTest {

    private static final @NotNull Set<PosixFilePermission> PERMISSIONS = Set.of(OWNER_READ, OWNER_WRITE);

    private final @NotNull Arguments arguments = new Arguments();
    private final @NotNull JCommander jCommander = JCommander.newBuilder().addObject(arguments).build();
    private final @NotNull AtomicReference<Integer> exitCode = new AtomicReference<>();

    private @NotNull Path projectFolder;

    @TempDir
    private @NotNull Path tmp;

    @BeforeEach
    void setUp() throws Exception {
        projectFolder = tmp.resolve("example-project")
                .resolve("src")
                .resolve("main")
                .resolve("java")
                .resolve("de")
                .resolve("donnerbart")
                .resolve("example");
        Files.createDirectories(projectFolder);

        // tests
        copyResourceToTarget(projectFolder, "tests/FastTest.java", "FastTest.java", PERMISSIONS);
        copyResourceToTarget(projectFolder, "tests/SlowTest.java", "SlowTest.java", PERMISSIONS);
        copyResourceToTarget(projectFolder, "tests/SlowestTest.java", "SlowestTest.java", PERMISSIONS);

        final var reportFolder = tmp.resolve("junit-reports");
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
    void main_fullLifecycle() {
        assertThatNoException().isThrownBy(() -> TestSplitMain.main(new String[]{
                "-i", "0", "-t", "2", "-g", "**/example-project/**/*Test.java", "-w", tmp.toString(), "-d"}));
    }

    @Test
    void run() throws Exception {
        final var args = new String[]{
                "-i",
                "0",
                "-t",
                "2",
                "-g",
                "**/example-project/**/*Test.java",
                "-e",
                "**/example-project/**/*Abstract*.java",
                "-j",
                "**/junit-reports/*.xml",
                "-w",
                tmp.toString(),
                "-c",
                "-d"};
        final var splits = TestSplitMain.run(exitCode::set, args);
        assertThat(exitCode).hasNullValue();

        assertThat(splits.size()).isEqualTo(2);
        assertThat(splits.get(0).sortedTests()).hasSize(1) //
                .containsExactly("de.donnerbart.example.SlowestTest");
        assertThat(splits.get(1).sortedTests()).hasSize(2)
                .containsExactly("de.donnerbart.example.SlowTest", "de.donnerbart.example.FastTest");
    }

    @Test
    void init() throws Exception {
        TestSplitMain.init(exitCode::set, new String[]{"-i", "0", "-t", "1", "-g", "**/*Test.java"});
        assertThat(exitCode).hasNullValue();
    }

    @Test
    void init_withHelp() throws Exception {
        TestSplitMain.init(exitCode::set, new String[]{"-h"});
        assertThat(exitCode).hasValue(0);
    }

    @Test
    void init_withInvalidArguments() throws Exception {
        TestSplitMain.init(exitCode::set, new String[]{"-i", "0", "-t", "0", "-g", "**/*Test.java"});
        assertThat(exitCode).hasValue(1);
    }

    @Test
    void validateArguments() {
        jCommander.parse("-i", "0", "-t", "1", "-g", "**/*Test.java", "-w", tmp.toAbsolutePath().toString());
        assertThat(TestSplitMain.validateArguments(arguments)).isTrue();
    }

    @Test
    void validateArguments_withZeroSplitTotal() {
        jCommander.parse("-i", "0", "-t", "0", "-g", "**/*Test.java", "-w", tmp.toAbsolutePath().toString());
        assertThat(TestSplitMain.validateArguments(arguments)).isFalse();
    }

    @Test
    void validateArguments_withNegativeSplitTotal() {
        jCommander.parse("-i", "0", "-t", "-1", "-g", "**/*Test.java", "-w", tmp.toAbsolutePath().toString());
        assertThat(TestSplitMain.validateArguments(arguments)).isFalse();
    }

    @Test
    void validateArguments_withTooSmallSplitIndex() {
        jCommander.parse("-i", "1", "-t", "1", "-g", "**/*Test.java", "-w", tmp.toAbsolutePath().toString());
        assertThat(TestSplitMain.validateArguments(arguments)).isFalse();
    }

    @Test
    void validateArguments_withInvalidWorkingDirectory() {
        jCommander.parse("-i", "0", "-t", "1", "-g", "**/*Test.java", "-w", tmp.resolve("does-not-exist").toString());
        assertThat(TestSplitMain.validateArguments(arguments)).isFalse();
    }

    @Test
    void calculateOptimalTotalSplit() throws Exception {
        copyResourceToTarget(projectFolder, "tests/NoTimingOneTest.java", "NoTimingOneTest.java", PERMISSIONS);
        copyResourceToTarget(projectFolder, "tests/NoTimingTwoTest.java", "NoTimingTwoTest.java", PERMISSIONS);

        jCommander.parse("-i", "0", "-t", "2", "-g", "**/*Test.java", "-j", "**/junit-reports/*.xml");
        assertThat(TestSplitMain.calculateOptimalTotalSplit(arguments, getTestCases())).isEqualTo(2);
    }

    @Test
    void calculateOptimalTotalSplit_withMaxCalculations() throws Exception {
        copyResourceToTarget(projectFolder, "tests/NoTimingOneTest.java", "NoTimingOneTest.java", PERMISSIONS);
        copyResourceToTarget(projectFolder, "tests/NoTimingTwoTest.java", "NoTimingTwoTest.java", PERMISSIONS);

        jCommander.parse("-i",
                "0",
                "-t",
                "4",
                "-g",
                "**/*Test.java",
                "-j",
                "**/junit-reports/*.xml",
                "-n",
                "max",
                "-m",
                "5");
        assertThat(TestSplitMain.calculateOptimalTotalSplit(arguments, getTestCases())).isEqualTo(4);
    }

    @Test
    void calculateOptimalTotalSplit_withTooLowMaxCalculations() throws Exception {
        copyResourceToTarget(projectFolder, "tests/NoTimingOneTest.java", "NoTimingOneTest.java", PERMISSIONS);
        copyResourceToTarget(projectFolder, "tests/NoTimingTwoTest.java", "NoTimingTwoTest.java", PERMISSIONS);

        jCommander.parse("-i",
                "0",
                "-t",
                "1",
                "-g",
                "**/*Test.java",
                "-j",
                "**/junit-reports/*.xml",
                "-n",
                "max",
                "-m",
                "4");
        assertThat(TestSplitMain.calculateOptimalTotalSplit(arguments, getTestCases())).isEqualTo(0);
    }

    @Test
    void calculateOptimalTotalSplit_withTotalSplitMismatch() throws Exception {
        jCommander.parse("-i", "0", "-t", "1", "-g", "**/*Test.java", "-j", "**/junit-reports/*.xml");
        assertThat(TestSplitMain.calculateOptimalTotalSplit(arguments, getTestCases())).isEqualTo(2);
    }

    @Test
    void calculateOptimalTotalSplit_withoutJUnitGlob() throws Exception {
        jCommander.parse("-i", "0", "-t", "1", "-g", "**/*Test.java");
        assertThat(TestSplitMain.calculateOptimalTotalSplit(arguments, getTestCases())).isEqualTo(0);
    }

    @Test
    void calculateOptimalTotalSplit_withInvalidSplitIndex() throws Exception {
        jCommander.parse("-i", "1", "-t", "1", "-g", "**/*Test.java", "-j", "**/junit-reports/*.xml");
        assertThat(TestSplitMain.calculateOptimalTotalSplit(arguments, getTestCases())).isEqualTo(0);
    }

    @Test
    void readProperties() throws Exception {
        final var properties = TestSplitMain.readProperties("split-tests-java-test.properties");
        assertThat(properties).isNotNull();
        assertThat(properties.getProperty("git.commit.time")).isEqualTo("2023-01-01T00:01:02+0000");
        assertThat(properties.getProperty("git.commit.id")).isEqualTo("4c204731e327bc2e06d2a1b02f46e4195c210d0e");
        assertThat(properties.getProperty("git.commit.id.abbrev")).isEqualTo("4c20473");
        assertThat(properties.getProperty("git.branch")).isEqualTo("master");
        assertThat(properties.getProperty("version")).isEqualTo("1.2.3-SNAPSHOT");
    }

    @Test
    void readProperties_whenFileNotFound() throws Exception {
        final var properties = TestSplitMain.readProperties("not-found.properties");
        assertThat(properties).isNotNull();
        assertThat(properties).isEmpty();
    }

    @Test
    void getBuiltTime() {
        assertThat(TestSplitMain.getBuiltTime("2023-01-01T00:01:02+0000")).isEqualTo("2023-01-01T00:01:02+0000");
    }

    @Test
    void getBuiltTime_whenTimeNotParseable() {
        assertThat(TestSplitMain.getBuiltTime("")).isEqualTo(Date.from(Instant.EPOCH));
    }

    private @NotNull Set<TestCase> getTestCases() throws Exception {
        return new TestLoader(arguments.glob,
                arguments.excludeGlob,
                arguments.junitGlob,
                arguments.newTestTimeOption,
                tmp,
                exitCode::set).load();
    }
}
