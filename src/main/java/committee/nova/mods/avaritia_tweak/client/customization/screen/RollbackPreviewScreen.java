package committee.nova.mods.avaritia_tweak.client.customization.screen;

import committee.nova.mods.avaritia_tweak.client.customization.EditorController;
import committee.nova.mods.avaritia_tweak.client.customization.ClientCustomizationServices;
import committee.nova.mods.avaritia_tweak.customization.commit.CommitResult;
import committee.nova.mods.avaritia_tweak.customization.commit.FileObservation;
import committee.nova.mods.avaritia_tweak.customization.commit.ManagedPathResolver;
import committee.nova.mods.avaritia_tweak.customization.diff.ChangeType;
import committee.nova.mods.avaritia_tweak.customization.diff.WorkspaceDiff;
import committee.nova.mods.avaritia_tweak.customization.diff.WorkspaceDiffer;
import committee.nova.mods.avaritia_tweak.customization.history.Revision;
import committee.nova.mods.avaritia_tweak.customization.render.ArtifactPath;
import committee.nova.mods.avaritia_tweak.customization.render.RenderedArtifact;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

public final class RollbackPreviewScreen extends Screen {
    private final Screen previous;
    private final Screen returnAfterSuccess;
    private final EditorController controller;
    private final ManagedPathResolver pathResolver;
    private final Revision revision;
    private final List<RenderedArtifact> artifacts;
    private final WorkspaceDiff diff;
    private EditBox messageBox;
    private int artifactIndex;
    private int lineOffset;
    private boolean committing;
    private String message;
    private String status = "";

    public RollbackPreviewScreen(Screen previous, Screen returnAfterSuccess,
                                 EditorController controller, Revision revision) {
        super(Component.translatable("gui.avaritia_tweak.rollback.title", revision.version()));
        this.previous = previous;
        this.returnAfterSuccess = returnAfterSuccess;
        this.controller = controller;
        ClientCustomizationServices services = ClientCustomizationServices.get();
        this.pathResolver = new ManagedPathResolver(services.gameRoot(), services.stateRoot());
        this.revision = revision;
        this.message = "Rollback to version " + revision.version();
        this.artifacts = new ArrayList<>(revision.artifacts().artifacts().values());
        this.diff = new WorkspaceDiffer().diff(controller.committed(), revision.snapshot());
    }

    @Override
    protected void init() {
        int panelWidth = Math.min(460, this.width - 12);
        int panelHeight = Math.min(270, this.height - 12);
        int left = (this.width - panelWidth) / 2;
        int top = (this.height - panelHeight) / 2;
        int bottom = top + panelHeight - 28;
        this.messageBox = new EditBox(this.font, left + 8, bottom, Math.max(100, panelWidth - 190), 20,
                Component.translatable("gui.avaritia_tweak.commit_message"));
        this.messageBox.setMaxLength(256);
        this.messageBox.setValue(this.message);
        this.messageBox.setResponder(value -> this.message = value);
        this.addRenderableWidget(this.messageBox);
        this.addRenderableWidget(Button.builder(Component.literal("<"), button -> cycleArtifact(-1))
                .bounds(left + 8, top + 45, 30, 18).build());
        this.addRenderableWidget(Button.builder(Component.literal(">"), button -> cycleArtifact(1))
                .bounds(left + panelWidth - 38, top + 45, 30, 18).build());
        Button copy = this.addRenderableWidget(Button.builder(Component.translatable("gui.avaritia_tweak.copy"),
                button -> copyCurrent()).bounds(left + panelWidth - 176, bottom, 74, 20).build());
        copy.active = !this.artifacts.isEmpty();
        this.addRenderableWidget(Button.builder(Component.translatable("gui.avaritia_tweak.rollback.commit"),
                button -> rollback(new TreeMap<>())).bounds(left + panelWidth - 96, bottom, 88, 20).build());
    }

    private void cycleArtifact(int delta) {
        if (!this.artifacts.isEmpty()) {
            this.artifactIndex = Math.floorMod(this.artifactIndex + delta, this.artifacts.size());
            this.lineOffset = 0;
        }
    }

    private void copyCurrent() {
        if (!this.artifacts.isEmpty()) {
            Minecraft.getInstance().keyboardHandler.setClipboard(current().text());
            this.status = "Copied complete historical artifact";
        }
    }

    private void rollback(SortedMap<ArtifactPath, FileObservation> confirmations) {
        if (this.committing) {
            return;
        }
        this.committing = true;
        this.message = this.messageBox.getValue();
        this.controller.rollback(this.revision, this.message, confirmations)
                .whenComplete((result, throwable) -> Minecraft.getInstance().execute(() -> {
                    this.committing = false;
                    if (throwable != null) {
                        this.status = "Rollback failed: " + CommitResultMessages.safeMessage(throwable);
                    } else if (result instanceof CommitResult.Success success) {
                        this.status = "Created version " + success.revision().version();
                        if (this.returnAfterSuccess instanceof CustomizationEditorScreen editor) {
                            editor.refreshFromController();
                        }
                        Minecraft.getInstance().setScreen(this.returnAfterSuccess);
                    } else if (result instanceof CommitResult.ExternalFileConflict conflict) {
                        Minecraft.getInstance().setScreen(new ConflictScreen(this, conflict.conflicts(),
                                takeover -> rollback(takeover)));
                    } else {
                        this.status = CommitResultMessages.describe(result);
                    }
                }));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int lines = currentLines().size();
        this.lineOffset = Math.max(0, Math.min(Math.max(0, lines - 12),
                this.lineOffset + (delta < 0 ? 3 : -3)));
        return true;
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        int panelWidth = Math.min(460, this.width - 12);
        int panelHeight = Math.min(270, this.height - 12);
        int left = (this.width - panelWidth) / 2;
        int top = (this.height - panelHeight) / 2;
        graphics.fill(left, top, left + panelWidth, top + panelHeight, 0xf0171a21);
        graphics.drawString(this.font, this.title, left + 8, top + 8, 0xfff0c66a, false);
        graphics.drawString(this.font, "+" + this.diff.count(ChangeType.ADDED)
                + "  ~" + this.diff.count(ChangeType.MODIFIED)
                + "  -" + this.diff.count(ChangeType.REMOVED), left + 8, top + 24,
                0xffaeb6c6, false);
        if (this.artifacts.isEmpty()) {
            graphics.drawCenteredString(this.font,
                    Component.translatable("gui.avaritia_tweak.rollback.empty_artifacts"),
                    left + panelWidth / 2, top + 92, 0xffffc76b);
        } else {
            RenderedArtifact artifact = current();
            graphics.drawCenteredString(this.font, artifact.logicalPath().value(),
                    left + panelWidth / 2, top + 49, 0xffd7dbe5);
            graphics.drawString(this.font, ScreenText.ellipsize(this.pathResolver.resolveArtifact(
                            artifact.logicalPath()).toString(), Math.max(18, panelWidth / 6)),
                    left + 8, top + 62, 0xff8d96a8, false);
            graphics.drawString(this.font, (this.artifactIndex + 1) + "/" + this.artifacts.size()
                    + "  sha256 " + artifact.sha256().substring(0, 12), left + 8, top + 74,
                    0xff8d96a8, false);
            List<String> lines = currentLines();
            int y = top + 88;
            for (int index = this.lineOffset; index < Math.min(lines.size(), this.lineOffset + 12); index++) {
                graphics.drawString(this.font,
                        ScreenText.ellipsize(lines.get(index), Math.max(18, panelWidth / 6)),
                        left + 8, y, 0xffc8ceda, false);
                y += 10;
            }
        }
        if (!this.status.isEmpty()) {
            graphics.drawString(this.font,
                    ScreenText.ellipsize(this.status, Math.max(18, panelWidth / 6)),
                    left + 8, top + panelHeight - 38,
                    this.status.startsWith("Rollback failed") ? 0xffff6b6b : 0xff78d6a3, false);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(this.previous);
    }

    private RenderedArtifact current() {
        return this.artifacts.get(this.artifactIndex);
    }

    private List<String> currentLines() {
        return this.artifacts.isEmpty() ? List.of() : List.of(current().text().split("\\n", -1));
    }

}
