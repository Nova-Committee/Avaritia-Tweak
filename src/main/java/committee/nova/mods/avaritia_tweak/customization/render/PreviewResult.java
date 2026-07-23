package committee.nova.mods.avaritia_tweak.customization.render;

import committee.nova.mods.avaritia_tweak.customization.diff.ArtifactChange;
import committee.nova.mods.avaritia_tweak.customization.diff.WorkspaceDiff;
import committee.nova.mods.avaritia_tweak.customization.validation.ValidationReport;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record PreviewResult(ValidationReport validation, WorkspaceDiff workspaceDiff,
                            Optional<RenderPlan> baselinePlan, Optional<RenderPlan> renderPlan,
                            List<ArtifactChange> artifactChanges) {
    public PreviewResult {
        Objects.requireNonNull(validation, "validation");
        Objects.requireNonNull(workspaceDiff, "workspaceDiff");
        baselinePlan = Objects.requireNonNull(baselinePlan, "baselinePlan");
        renderPlan = Objects.requireNonNull(renderPlan, "renderPlan");
        artifactChanges = List.copyOf(Objects.requireNonNull(artifactChanges, "artifactChanges"));
    }
}
