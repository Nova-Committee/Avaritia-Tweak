package committee.nova.mods.avaritia_tweak.customization.history;

import java.util.Objects;

public sealed interface RevisionDecodeResult
        permits RevisionDecodeResult.Success, RevisionDecodeResult.Failure {
    record Success(Revision revision) implements RevisionDecodeResult {
        public Success {
            Objects.requireNonNull(revision, "revision");
        }
    }

    record Failure(String code, String fieldPath, String message) implements RevisionDecodeResult {
        public Failure {
            Objects.requireNonNull(code, "code");
            Objects.requireNonNull(fieldPath, "fieldPath");
            Objects.requireNonNull(message, "message");
        }
    }
}
