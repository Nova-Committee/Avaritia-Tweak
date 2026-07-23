package committee.nova.mods.avaritia_tweak.client.customization.screen;

import committee.nova.mods.avaritia_tweak.client.customization.EditorController;
import committee.nova.mods.avaritia_tweak.client.customization.ClientCustomizationServices;
import committee.nova.mods.avaritia_tweak.customization.commit.CommitResult;
import committee.nova.mods.avaritia_tweak.customization.commit.FileObservation;
import committee.nova.mods.avaritia_tweak.customization.commit.ManagedPathResolver;
import committee.nova.mods.avaritia_tweak.customization.diff.ArtifactChange;
import committee.nova.mods.avaritia_tweak.customization.diff.ChangeType;
import committee.nova.mods.avaritia_tweak.customization.render.ArtifactPath;
import committee.nova.mods.avaritia_tweak.customization.render.PreviewResult;
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
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;

public final class PreviewScreen extends Screen {
    private final Screen previous;
    private final EditorController controller;
    private final ManagedPathResolver pathResolver;
    private final List<PreviewArtifact> artifacts;
    private int artifactIndex;
    private int lineOffset;
    private EditBox messageBox;
    private String message = "";
    private String status = "";
    private boolean committing;

    public PreviewScreen(Screen previous, EditorController controller, PreviewResult preview) {
        super(Component.translatable("gui.avaritia_tweak.preview.title"));
        this.previous = previous;
        this.controller = controller;
        ClientCustomizationServices services = ClientCustomizationServices.get();
        this.pathResolver = new ManagedPathResolver(services.gameRoot(), services.stateRoot());
        this.artifacts = buildArtifacts(preview);
    }

    @Override
    protected void init() {
        int panelWidth = Math.min(440, this.width - 12);
        int panelHeight = Math.min(250, this.height - 12);
        int left = (this.width - panelWidth) / 2;
        int top = (this.height - panelHeight) / 2;
        int bottom = top + panelHeight - 32;
        int messageWidth = Math.max(80, panelWidth - 190);
        this.messageBox = new EditBox(this.font, left + 8, bottom, messageWidth, 20,
                Component.translatable("gui.avaritia_tweak.commit_message"));
        this.messageBox.setMaxLength(256);
        this.messageBox.setValue(this.message);
        this.messageBox.setResponder(value -> this.message = value);
        this.addRenderableWidget(this.messageBox);
        this.addRenderableWidget(Button.builder(Component.literal("<"), button -> cycleArtifact(-1))
                .bounds(left + 8, top + 30, 30, 18).build());
        this.addRenderableWidget(Button.builder(Component.literal(">"), button -> cycleArtifact(1))
                .bounds(left + panelWidth - 38, top + 30, 30, 18).build());
        Button copy = this.addRenderableWidget(Button.builder(
                Component.translatable("gui.avaritia_tweak.copy"), button -> copyCurrent())
                .bounds(left + panelWidth - 176, bottom, 74, 20).build());
        copy.active = !this.artifacts.isEmpty();
        this.addRenderableWidget(Button.builder(Component.translatable("gui.avaritia_tweak.commit_local"),
                button -> commit(new TreeMap<>()))
                .bounds(left + panelWidth - 96, bottom, 88, 20).build());
    }

    private void cycleArtifact(int delta) {
        if (this.artifacts.isEmpty()) {
            return;
        }
        this.artifactIndex = Math.floorMod(this.artifactIndex + delta, this.artifacts.size());
        this.lineOffset = 0;
    }

    private void copyCurrent() {
        if (!this.artifacts.isEmpty()) {
            Minecraft.getInstance().keyboardHandler.setClipboard(current().artifact().text());
            this.status = "Copied complete artifact";
        }
    }

    private void commit(SortedMap<ArtifactPath, FileObservation> confirmations) {
        if (this.committing) {
            return;
        }
        this.committing = true;
        this.message = this.messageBox.getValue();
        this.controller.commit(this.message, confirmations).whenComplete((result, throwable) ->
                Minecraft.getInstance().execute(() -> {
                    this.committing = false;
                    if (throwable != null) {
                        this.status = "Commit failed: " + CommitResultMessages.safeMessage(throwable);
                    } else if (result instanceof CommitResult.Success success) {
                        this.status = "Committed local version " + success.revision().version();
                        refreshEditor(this.previous);
                        Minecraft.getInstance().setScreen(this.previous);
                    } else if (result instanceof CommitResult.ExternalFileConflict conflict) {
                        Minecraft.getInstance().setScreen(new ConflictScreen(this, conflict.conflicts(),
                                takeover -> commit(takeover)));
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
        int panelWidth = Math.min(440, this.width - 12);
        int panelHeight = Math.min(250, this.height - 12);
        int left = (this.width - panelWidth) / 2;
        int top = (this.height - panelHeight) / 2;
        graphics.fill(left, top, left + panelWidth, top + panelHeight, 0xf0171a21);
        graphics.drawString(this.font, this.title, left + 8, top + 8, 0xfff0c66a, false);
        if (!this.artifacts.isEmpty()) {
            PreviewArtifact current = current();
            RenderedArtifact artifact = current.artifact();
            graphics.drawCenteredString(this.font, artifact.logicalPath().value(),
                    left + panelWidth / 2, top + 34, 0xffd7dbe5);
            graphics.drawString(this.font, ScreenText.ellipsize(this.pathResolver.resolveArtifact(
                            artifact.logicalPath()).toString(), Math.max(18, panelWidth / 6)),
                    left + 8, top + 48, 0xff8d96a8, false);
            String state = current.change().map(Enum::name).orElse("UNCHANGED")
                    + (current.previousContent() ? " (previous content)" : "");
            graphics.drawString(this.font, (this.artifactIndex + 1) + "/" + this.artifacts.size()
                    + "  " + state + "  sha256 " + artifact.sha256().substring(0, 12),
                    left + 8, top + 60, changeColor(current.change()), false);
            List<String> lines = currentLines();
            int y = top + 74;
            for (int index = this.lineOffset; index < Math.min(lines.size(), this.lineOffset + 12); index++) {
                graphics.drawString(this.font,
                        ScreenText.ellipsize(lines.get(index), Math.max(18, panelWidth / 6)),
                        left + 8, y, 0xffc8ceda, false);
                y += 10;
            }
        }
        graphics.drawString(this.font, Component.translatable("gui.avaritia_tweak.commit_message"),
                left + 8, top + panelHeight - 42, 0xff8d96a8, false);
        if (!this.status.isEmpty()) {
            graphics.drawString(this.font,
                    ScreenText.ellipsize(this.status, Math.max(18, panelWidth / 6)),
                    left + 8, top + panelHeight - 10,
                    this.status.startsWith("Commit failed") ? 0xffff6b6b : 0xff78d6a3, false);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(this.previous);
    }

    private PreviewArtifact current() {
        return this.artifacts.get(this.artifactIndex);
    }

    private List<String> currentLines() {
        return this.artifacts.isEmpty()
                ? List.of()
                : List.of(current().artifact().text().split("\\n", -1));
    }

    private static List<PreviewArtifact> buildArtifacts(PreviewResult preview) {
        TreeMap<ArtifactPath, PreviewArtifact> indexed = new TreeMap<>();
        preview.renderPlan().orElseThrow().artifacts().forEach((path, artifact) ->
                indexed.put(path, new PreviewArtifact(artifact, Optional.empty(), false)));
        for (ArtifactChange change : preview.artifactChanges()) {
            if (change.type() == ChangeType.REMOVED) {
                RenderedArtifact previous = preview.baselinePlan().orElseThrow()
                        .artifacts().get(change.path());
                if (previous != null) {
                    indexed.put(change.path(), new PreviewArtifact(previous,
                            Optional.of(ChangeType.REMOVED), true));
                }
            } else {
                PreviewArtifact artifact = indexed.get(change.path());
                if (artifact != null) {
                    indexed.put(change.path(), new PreviewArtifact(artifact.artifact(),
                            Optional.of(change.type()), false));
                }
            }
        }
        return new ArrayList<>(indexed.values());
    }

    private static int changeColor(Optional<ChangeType> change) {
        if (change.isEmpty()) {
            return 0xff8d96a8;
        }
        return switch (change.get()) {
            case ADDED -> 0xff78d6a3;
            case MODIFIED -> 0xffffc76b;
            case REMOVED -> 0xffff6b6b;
        };
    }

    private static void refreshEditor(Screen screen) {
        if (screen instanceof CustomizationEditorScreen editor) {
            editor.refreshFromController();
        }
    }

    private record PreviewArtifact(RenderedArtifact artifact, Optional<ChangeType> change,
                                   boolean previousContent) {
    }
}
