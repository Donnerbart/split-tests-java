package de.donnerbart.split;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;
import uk.org.webcompere.systemstubs.stream.SystemOut;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static de.donnerbart.split.TestUtil.copyResourceToTarget;
import static java.nio.file.attribute.PosixFilePermission.OWNER_READ;
import static java.nio.file.attribute.PosixFilePermission.OWNER_WRITE;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SystemStubsExtension.class)
class TestSplitTest {

    private static final @NotNull Set<PosixFilePermission> PERMISSIONS = Set.of(OWNER_READ, OWNER_WRITE);

    @SystemStub
    private @NotNull SystemOut systemOut;

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
        copyResourceToTarget(projectFolder, "tests/FastTest.java", "FastTest.java", PERMISSIONS);
        copyResourceToTarget(projectFolder, "tests/NoTimingTest.java", "NoTimingTest.java", PERMISSIONS);
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
    void run_withoutJUnit_firstSplit() throws Exception {
        final var testSplit = new TestSplit(0,
                2,
                "**/example-project/**/*Test.java",
                "**/example-project/**/*Abstract*.java",
                null,
                tmp,
                true,
                exitCode::set);
        testSplit.run();

        assertThat(systemOut.getLines()).hasSize(1);
        assertThat(systemOut.getText()).isEqualTo(
                "de.donnerbart.example.SlowestTest de.donnerbart.example.NoTimingTest");
        assertThat(exitCode).hasNullValue();
    }

    @Test
    void run_withoutJUnit_secondSplit() throws Exception {
        final var testSplit = new TestSplit(1,
                2,
                "**/example-project/**/*Test.java",
                "**/example-project/**/*Abstract*.java",
                null,
                tmp,
                true,
                exitCode::set);
        testSplit.run();

        assertThat(systemOut.getLines()).hasSize(1);
        assertThat(systemOut.getText()).isEqualTo("de.donnerbart.example.SlowTest de.donnerbart.example.FastTest");
        assertThat(exitCode).hasNullValue();
    }

    @Test
    void run_withJUnit_firstSplit() throws Exception {
        final var testSplit = new TestSplit(0,
                2,
                "**/example-project/**/*Test.java",
                "**/example-project/**/*Abstract*.java",
                "**/junit-reports/*.xml",
                tmp,
                true,
                exitCode::set);
        testSplit.run();

        assertThat(systemOut.getLines()).hasSize(1);
        assertThat(systemOut.getText()).isEqualTo("de.donnerbart.example.SlowestTest");
        assertThat(exitCode).hasNullValue();
    }

    @Test
    void run_withJUnit_secondSplit() throws Exception {
        final var testSplit = new TestSplit(1,
                2,
                "**/example-project/**/*Test.java",
                "**/example-project/**/*Abstract*.java",
                "**/junit-reports/*.xml",
                tmp,
                true,
                exitCode::set);
        testSplit.run();

        assertThat(systemOut.getLines()).hasSize(1);
        assertThat(systemOut.getText()).isEqualTo(
                "de.donnerbart.example.SlowTest de.donnerbart.example.FastTest de.donnerbart.example.NoTimingTest");
        assertThat(exitCode).hasNullValue();
    }

    @Test
    void run_noPackage() throws Exception {
        final var projectFolder = tmp.resolve("no-package-project").resolve("src").resolve("main").resolve("java");
        copyResourceToTarget(projectFolder, "tests/NoPackageTest.java", "NoPackageTest.java", PERMISSIONS);

        final var testSplit = new TestSplit(0,
                1,
                "**/no-package-project/**/*Test.java",
                null,
                null,
                projectFolder,
                true,
                exitCode::set);
        testSplit.run();

        assertThat(systemOut.getLinesNormalized()).isEmpty();
        assertThat(exitCode).hasValue(1);
    }

    @Test
    void run_noClassName() throws Exception {
        final var projectFolder = tmp.resolve("no-classname-project").resolve("src").resolve("main").resolve("java");
        copyResourceToTarget(projectFolder, "tests/NoClassNameTest.java", "NoClassNameTest.java", PERMISSIONS);

        final var testSplit = new TestSplit(0,
                1,
                "**/no-classname-project/**/*Test.java",
                null,
                null,
                projectFolder,
                true,
                exitCode::set);
        testSplit.run();

        assertThat(systemOut.getLinesNormalized()).isEmpty();
        assertThat(exitCode).hasValue(1);
    }
}
