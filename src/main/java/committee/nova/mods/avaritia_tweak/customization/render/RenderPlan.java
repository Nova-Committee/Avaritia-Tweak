package committee.nova.mods.avaritia_tweak.customization.render;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;

public record RenderPlan(SortedMap<ArtifactPath, RenderedArtifact> artifacts) {
    public RenderPlan {
        Objects.requireNonNull(artifacts, "artifacts");
        TreeMap<ArtifactPath, RenderedArtifact> copy = new TreeMap<>();
        artifacts.forEach((path, artifact) -> {
            Objects.requireNonNull(path, "artifact path");
            Objects.requireNonNull(artifact, "artifact");
            if (!path.equals(artifact.logicalPath())) {
                throw new IllegalArgumentException("Artifact map key does not match artifact path: " + path);
            }
            if (copy.put(path, artifact) != null) {
                throw new IllegalArgumentException("Duplicate artifact path: " + path);
            }
        });
        artifacts = Collections.unmodifiableSortedMap(copy);
    }

    public static RenderPlan of(Collection<RenderedArtifact> artifacts) {
        TreeMap<ArtifactPath, RenderedArtifact> indexed = new TreeMap<>();
        for (RenderedArtifact artifact : artifacts) {
            if (indexed.put(artifact.logicalPath(), artifact) != null) {
                throw new IllegalArgumentException("Duplicate artifact path: " + artifact.logicalPath());
            }
        }
        return new RenderPlan(indexed);
    }
}
