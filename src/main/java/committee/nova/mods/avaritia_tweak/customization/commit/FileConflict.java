package committee.nova.mods.avaritia_tweak.customization.commit;

import committee.nova.mods.avaritia_tweak.customization.render.ArtifactPath;

import java.nio.file.Path;
import java.util.Objects;

public record FileConflict(ArtifactPath logicalPath, Path actualPath, ConflictReason reason,
                           FileObservation expected, FileObservation actual) {
    public FileConflict {
        Objects.requireNonNull(logicalPath, "logicalPath");
        actualPath = Objects.requireNonNull(actualPath, "actualPath").toAbsolutePath().normalize();
        Objects.requireNonNull(reason, "reason");
        Objects.requireNonNull(expected, "expected");
        Objects.requireNonNull(actual, "actual");
    }
}
