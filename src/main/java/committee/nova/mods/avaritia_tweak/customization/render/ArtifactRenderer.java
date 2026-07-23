package committee.nova.mods.avaritia_tweak.customization.render;

import committee.nova.mods.avaritia_tweak.customization.model.OutputTarget;
import committee.nova.mods.avaritia_tweak.customization.model.WorkspaceSnapshot;

import java.util.List;
import java.util.Set;

public interface ArtifactRenderer {
    Set<OutputTarget> supportedTargets();

    List<RenderedArtifact> render(WorkspaceSnapshot snapshot);
}
