package committee.nova.mods.avaritia_tweak.customization.commit;

import java.util.Objects;
import java.util.Optional;

public record FileObservation(boolean exists, Optional<String> sha256) {
    public FileObservation {
        sha256 = Objects.requireNonNull(sha256, "sha256");
        if (exists != sha256.isPresent()) {
            throw new IllegalArgumentException("Existing files require a checksum and missing files cannot have one");
        }
    }

    public static FileObservation missing() {
        return new FileObservation(false, Optional.empty());
    }

    public static FileObservation existing(String sha256) {
        return new FileObservation(true, Optional.of(sha256));
    }
}
