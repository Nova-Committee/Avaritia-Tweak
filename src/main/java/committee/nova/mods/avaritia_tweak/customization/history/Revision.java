package committee.nova.mods.avaritia_tweak.customization.history;

import committee.nova.mods.avaritia_tweak.customization.model.WorkspaceSnapshot;
import committee.nova.mods.avaritia_tweak.customization.render.ArtifactPath;
import committee.nova.mods.avaritia_tweak.customization.render.RenderPlan;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.OptionalLong;

public record Revision(long version, long parentVersion, Instant committedAt, String message,
                       RevisionSummary summary, OptionalLong rollbackOf,
                       WorkspaceSnapshot snapshot, String snapshotSha256,
                       RenderPlan artifacts, List<ArtifactPath> deletedPaths) {
    public Revision {
        if (version <= 0 || parentVersion < 0 || version != parentVersion + 1) {
            throw new IllegalArgumentException("Revision versions must be positive and monotonic");
        }
        Objects.requireNonNull(committedAt, "committedAt");
        Objects.requireNonNull(message, "message");
        if (message.isBlank()) {
            throw new IllegalArgumentException("Revision message cannot be blank");
        }
        Objects.requireNonNull(summary, "summary");
        rollbackOf = Objects.requireNonNull(rollbackOf, "rollbackOf");
        if (rollbackOf.isPresent() && (rollbackOf.getAsLong() <= 0 || rollbackOf.getAsLong() > parentVersion)) {
            throw new IllegalArgumentException("Rollback source must be an existing earlier revision");
        }
        Objects.requireNonNull(snapshot, "snapshot");
        Objects.requireNonNull(snapshotSha256, "snapshotSha256");
        if (!snapshotSha256.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("Invalid snapshot SHA-256");
        }
        Objects.requireNonNull(artifacts, "artifacts");
        deletedPaths = List.copyOf(Objects.requireNonNull(deletedPaths, "deletedPaths"));
    }
}
