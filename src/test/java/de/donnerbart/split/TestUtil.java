package de.donnerbart.split;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class TestUtil {

    private TestUtil() {
    }

    static @NotNull Path copyResourceToTarget(
            final @NotNull Path targetDir,
            final @NotNull String resourceFileName,
            final @NotNull String tmpFileName,
            final @NotNull Set<PosixFilePermission> permissions) {
        try {
            final var resource = TestUtil.class.getResource("/" + resourceFileName);
            assertThat(resource).isNotNull();

            final var path = targetDir.resolve(tmpFileName);
            Files.deleteIfExists(path);

            final var parent = path.getParent();
            if (!Files.exists(parent)) {
                assertThat(Files.createDirectories(parent)).exists();
            }

            Files.copy(Path.of(resource.toURI()), path);
            if (!permissions.isEmpty()) {
                Files.setPosixFilePermissions(path, permissions);
            }
            return path.toAbsolutePath();
        } catch (final Exception e) {
            throw new AssertionError("Could not copy resource file to target", e);
        }
    }
}
