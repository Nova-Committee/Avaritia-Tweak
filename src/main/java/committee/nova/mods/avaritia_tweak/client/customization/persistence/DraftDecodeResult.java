package committee.nova.mods.avaritia_tweak.client.customization.persistence;

import java.util.Objects;

public sealed interface DraftDecodeResult permits DraftDecodeResult.Success, DraftDecodeResult.Failure {
    record Success(DraftState draft) implements DraftDecodeResult {
        public Success {
            Objects.requireNonNull(draft, "draft");
        }
    }

    record Failure(String code, String fieldPath, String message) implements DraftDecodeResult {
        public Failure {
            Objects.requireNonNull(code, "code");
            Objects.requireNonNull(fieldPath, "fieldPath");
            Objects.requireNonNull(message, "message");
        }
    }
}
