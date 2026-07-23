package committee.nova.mods.avaritia_tweak.customization.history;

import committee.nova.mods.avaritia_tweak.customization.model.WorkspaceSnapshot;
import committee.nova.mods.avaritia_tweak.customization.render.RenderPlan;

import java.util.Objects;

public record WorkspaceHead(long version, WorkspaceSnapshot snapshot, RenderPlan artifacts) {
    public WorkspaceHead {
        if (version < 0) {
            throw new IllegalArgumentException("Workspace version cannot be negative");
        }
        Objects.requireNonNull(snapshot, "snapshot");
        Objects.requireNonNull(artifacts, "artifacts");
    }

    public static WorkspaceHead initial() {
        return new WorkspaceHead(0, WorkspaceSnapshot.empty(), RenderPlan.of(java.util.List.of()));
    }
}
