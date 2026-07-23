package committee.nova.mods.avaritia_tweak.customization.render;

import committee.nova.mods.avaritia_tweak.customization.diff.ArtifactDiffer;
import committee.nova.mods.avaritia_tweak.customization.diff.WorkspaceDiff;
import committee.nova.mods.avaritia_tweak.customization.diff.WorkspaceDiffer;
import committee.nova.mods.avaritia_tweak.customization.model.WorkspaceSnapshot;
import committee.nova.mods.avaritia_tweak.customization.validation.ValidationReport;
import committee.nova.mods.avaritia_tweak.customization.validation.WorkspaceValidator;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class PreviewService {
    private final WorkspaceValidator validator;
    private final WorkspaceRenderService renderer;
    private final WorkspaceDiffer workspaceDiffer;
    private final ArtifactDiffer artifactDiffer;

    public PreviewService(WorkspaceValidator validator, WorkspaceRenderService renderer) {
        this(validator, renderer, new WorkspaceDiffer(), new ArtifactDiffer());
    }

    public PreviewService(WorkspaceValidator validator, WorkspaceRenderService renderer,
                          WorkspaceDiffer workspaceDiffer, ArtifactDiffer artifactDiffer) {
        this.validator = Objects.requireNonNull(validator, "validator");
        this.renderer = Objects.requireNonNull(renderer, "renderer");
        this.workspaceDiffer = Objects.requireNonNull(workspaceDiffer, "workspaceDiffer");
        this.artifactDiffer = Objects.requireNonNull(artifactDiffer, "artifactDiffer");
    }

    public PreviewResult preview(WorkspaceSnapshot committed, WorkspaceSnapshot draft) {
        ValidationReport validation = this.validator.validate(draft);
        WorkspaceDiff workspaceDiff = this.workspaceDiffer.diff(committed, draft);
        if (!validation.isValid()) {
            return new PreviewResult(validation, workspaceDiff,
                    Optional.empty(), Optional.empty(), List.of());
        }
        RenderPlan before = this.renderer.render(committed);
        RenderPlan after = this.renderer.render(draft);
        return new PreviewResult(validation, workspaceDiff, Optional.of(before), Optional.of(after),
                this.artifactDiffer.diff(before, after));
    }
}
