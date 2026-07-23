package committee.nova.mods.avaritia_tweak.customization.history;

import committee.nova.mods.avaritia_tweak.customization.model.WorkspaceSnapshot;

import java.util.Objects;

public sealed interface WorkspaceDecodeResult
        permits WorkspaceDecodeResult.Success, WorkspaceDecodeResult.Failure {
    record Success(WorkspaceSnapshot snapshot) implements WorkspaceDecodeResult {
        public Success {
            Objects.requireNonNull(snapshot, "snapshot");
        }
    }

    record Failure(String code, String fieldPath, String message) implements WorkspaceDecodeResult {
        public Failure {
            Objects.requireNonNull(code, "code");
            Objects.requireNonNull(fieldPath, "fieldPath");
            Objects.requireNonNull(message, "message");
        }
    }
}
