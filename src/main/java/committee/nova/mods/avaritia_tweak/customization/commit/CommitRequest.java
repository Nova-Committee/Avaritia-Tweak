package committee.nova.mods.avaritia_tweak.customization.commit;

import committee.nova.mods.avaritia_tweak.customization.model.WorkspaceSnapshot;
import committee.nova.mods.avaritia_tweak.customization.render.ArtifactPath;
import committee.nova.mods.avaritia_tweak.customization.render.RenderPlan;

import java.util.Collections;
import java.util.Objects;
import java.util.OptionalLong;
import java.util.SortedMap;
import java.util.TreeMap;

public record CommitRequest(long baseVersion, WorkspaceSnapshot snapshot, String message,
                            SortedMap<ArtifactPath, String> expectedArtifactSha256,
                            OptionalLong rollbackOf,
                            SortedMap<ArtifactPath, FileObservation> takeoverConfirmations) {
    public CommitRequest {
        if (baseVersion < 0) {
            throw new IllegalArgumentException("Base version cannot be negative");
        }
        Objects.requireNonNull(snapshot, "snapshot");
        Objects.requireNonNull(message, "message");
        expectedArtifactSha256 = immutable(expectedArtifactSha256);
        rollbackOf = Objects.requireNonNull(rollbackOf, "rollbackOf");
        takeoverConfirmations = immutableObservations(takeoverConfirmations);
    }

    public static CommitRequest previewed(long baseVersion, WorkspaceSnapshot snapshot,
                                          String message, RenderPlan plan) {
        TreeMap<ArtifactPath, String> hashes = new TreeMap<>();
        plan.artifacts().forEach((path, artifact) -> hashes.put(path, artifact.sha256()));
        return new CommitRequest(baseVersion, snapshot, message, hashes,
                OptionalLong.empty(), new TreeMap<>());
    }

    public CommitRequest withTakeover(SortedMap<ArtifactPath, FileObservation> confirmations) {
        return new CommitRequest(this.baseVersion, this.snapshot, this.message,
                this.expectedArtifactSha256, this.rollbackOf, confirmations);
    }

    private static SortedMap<ArtifactPath, String> immutable(SortedMap<ArtifactPath, String> source) {
        Objects.requireNonNull(source, "expectedArtifactSha256");
        TreeMap<ArtifactPath, String> copy = new TreeMap<>();
        source.forEach((path, hash) -> copy.put(Objects.requireNonNull(path, "artifact path"),
                Objects.requireNonNull(hash, "artifact hash")));
        return Collections.unmodifiableSortedMap(copy);
    }

    private static SortedMap<ArtifactPath, FileObservation> immutableObservations(
            SortedMap<ArtifactPath, FileObservation> source) {
        Objects.requireNonNull(source, "takeoverConfirmations");
        TreeMap<ArtifactPath, FileObservation> copy = new TreeMap<>();
        source.forEach((path, observation) -> copy.put(Objects.requireNonNull(path, "artifact path"),
                Objects.requireNonNull(observation, "file observation")));
        return Collections.unmodifiableSortedMap(copy);
    }
}
