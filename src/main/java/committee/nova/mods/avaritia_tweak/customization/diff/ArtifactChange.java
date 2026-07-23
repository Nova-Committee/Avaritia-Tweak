package committee.nova.mods.avaritia_tweak.customization.diff;

import committee.nova.mods.avaritia_tweak.customization.render.ArtifactPath;

import java.util.Objects;
import java.util.Optional;

public record ArtifactChange(ChangeType type, ArtifactPath path,
                             Optional<String> beforeSha256, Optional<String> afterSha256) {
    public ArtifactChange {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(path, "path");
        beforeSha256 = Objects.requireNonNull(beforeSha256, "beforeSha256");
        afterSha256 = Objects.requireNonNull(afterSha256, "afterSha256");
    }
}
