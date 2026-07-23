package committee.nova.mods.avaritia_tweak.client.customization.persistence;

import java.nio.file.Path;
import java.util.Objects;

public sealed interface DraftSaveResult permits DraftSaveResult.Success, DraftSaveResult.Failure {
    record Success(Path path) implements DraftSaveResult {
        public Success {
            Objects.requireNonNull(path, "path");
        }
    }

    record Failure(Path path, String message) implements DraftSaveResult {
        public Failure {
            Objects.requireNonNull(path, "path");
            Objects.requireNonNull(message, "message");
        }
    }
}
