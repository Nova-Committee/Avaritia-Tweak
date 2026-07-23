package committee.nova.mods.avaritia_tweak.customization.render;

import committee.nova.mods.avaritia_tweak.customization.model.WorkspaceSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class WorkspaceRenderService {
    private final List<ArtifactRenderer> renderers;

    public WorkspaceRenderService(List<ArtifactRenderer> renderers) {
        this.renderers = List.copyOf(Objects.requireNonNull(renderers, "renderers"));
    }

    public static WorkspaceRenderService standard() {
        return new WorkspaceRenderService(List.of(
                new KubeJsRenderer(),
                new CraftTweakerRenderer(),
                new SingularityDatapackRenderer()));
    }

    public RenderPlan render(WorkspaceSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        List<RenderedArtifact> artifacts = new ArrayList<>();
        this.renderers.forEach(renderer -> artifacts.addAll(renderer.render(snapshot)));
        return RenderPlan.of(artifacts);
    }
}
