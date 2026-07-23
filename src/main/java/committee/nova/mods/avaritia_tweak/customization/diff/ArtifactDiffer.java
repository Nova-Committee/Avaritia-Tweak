package committee.nova.mods.avaritia_tweak.customization.diff;

import committee.nova.mods.avaritia_tweak.customization.render.ArtifactPath;
import committee.nova.mods.avaritia_tweak.customization.render.RenderPlan;
import committee.nova.mods.avaritia_tweak.customization.render.RenderedArtifact;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeSet;

public final class ArtifactDiffer {
    public List<ArtifactChange> diff(RenderPlan before, RenderPlan after) {
        Objects.requireNonNull(before, "before");
        Objects.requireNonNull(after, "after");
        TreeSet<ArtifactPath> paths = new TreeSet<>(before.artifacts().keySet());
        paths.addAll(after.artifacts().keySet());
        List<ArtifactChange> changes = new ArrayList<>();
        for (ArtifactPath path : paths) {
            RenderedArtifact oldArtifact = before.artifacts().get(path);
            RenderedArtifact newArtifact = after.artifacts().get(path);
            if (oldArtifact == null) {
                changes.add(new ArtifactChange(ChangeType.ADDED, path, Optional.empty(),
                        Optional.of(newArtifact.sha256())));
            } else if (newArtifact == null) {
                changes.add(new ArtifactChange(ChangeType.REMOVED, path,
                        Optional.of(oldArtifact.sha256()), Optional.empty()));
            } else if (!oldArtifact.sha256().equals(newArtifact.sha256())) {
                changes.add(new ArtifactChange(ChangeType.MODIFIED, path,
                        Optional.of(oldArtifact.sha256()), Optional.of(newArtifact.sha256())));
            }
        }
        return List.copyOf(changes);
    }
}
