package committee.nova.mods.avaritia_tweak.client.customization.screen;

import committee.nova.mods.avaritia_tweak.client.customization.ClientCustomizationServices;
import committee.nova.mods.avaritia_tweak.client.customization.EditorController;
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
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;

public final class RollbackPreviewScreen extends Screen {
    private static final int SIDEBAR_WIDTH = 188;

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
    private int statusColor = EditorTheme.TEXT_MUTED;

    public RollbackPreviewScreen(Screen previous, Screen returnAfterSuccess,
                                 EditorController controller, Revision revision) {
        super(Component.translatable("gui.avaritia_tweak.rollback.title", revision.version()));
        this.previous = previous;
        this.returnAfterSuccess = returnAfterSuccess;
        this.controller = controller;
        ClientCustomizationServices services = ClientCustomizationServices.get();
        this.pathResolver = new ManagedPathResolver(services.gameRoot(), services.stateRoot());
        this.revision = revision;
        this.message = Component.translatable("gui.avaritia_tweak.rollback.default_message",
                revision.version()).getString();
        this.artifacts = new ArrayList<>(revision.artifacts().artifacts().values());
        this.diff = new WorkspaceDiffer().diff(controller.committed(), revision.snapshot());
    }

    @Override
    protected void init() {
        Layout layout = layout();
        int buttonY = layout.top + layout.height - 42;
        int actionWidth = Math.min(118, Math.max(92, layout.width / 6));
        int copyX = layout.left + layout.width - actionWidth * 2 - 14;
        int messageWidth = Math.max(90, copyX - layout.left - 18);
        this.messageBox = new EditBox(this.font, layout.left + 8, buttonY,
                messageWidth, 20, Component.translatable("gui.avaritia_tweak.commit_message"));
        this.messageBox.setMaxLength(256);
        this.messageBox.setValue(this.message);
        this.messageBox.setResponder(value -> this.message = value);
        this.addRenderableWidget(this.messageBox);

        EditorButton copy = this.addRenderableWidget(EditorButton.builder(
                        Component.translatable("gui.avaritia_tweak.copy"), button -> copyCurrent())
                .bounds(copyX, buttonY, actionWidth - 4, 20)
                .style(EditorButton.Style.QUIET).build());
        copy.active = !this.artifacts.isEmpty();
        this.addRenderableWidget(EditorButton.builder(
                        Component.translatable("gui.avaritia_tweak.rollback.commit"),
                        button -> rollback(new TreeMap<>()))
                .bounds(copyX + actionWidth, buttonY, actionWidth, 20)
                .style(EditorButton.Style.DANGER).build());

        if (layout.wide) {
            addArtifactList(layout);
        } else {
            this.addRenderableWidget(EditorButton.builder(Component.literal("<"), button -> cycleArtifact(-1))
                    .bounds(layout.left + 8, layout.top + 50, 30, 18)
                    .style(EditorButton.Style.QUIET).build());
            this.addRenderableWidget(EditorButton.builder(Component.literal(">"), button -> cycleArtifact(1))
                    .bounds(layout.left + layout.width - 38, layout.top + 50, 30, 18)
                    .style(EditorButton.Style.QUIET).build());
        }
    }

    private void addArtifactList(Layout layout) {
        int listTop = layout.top + 76;
        int available = Math.max(1, (layout.codeBottom - listTop) / 20);
        int page = this.artifactIndex / available;
        int start = page * available;
        int end = Math.min(this.artifacts.size(), start + available);
        for (int index = start; index < end; index++) {
            RenderedArtifact artifact = this.artifacts.get(index);
            String label = "R  " + artifactName(artifact.logicalPath());
            int selectedIndex = index;
            this.addRenderableWidget(EditorButton.builder(Component.literal(label),
                            button -> selectArtifact(selectedIndex))
                    .bounds(layout.left + 8, listTop + (index - start) * 20,
                            SIDEBAR_WIDTH - 16, 18)
                    .style(EditorButton.Style.LIST).selected(index == this.artifactIndex).build());
        }
        int pages = Math.max(1, (this.artifacts.size() + available - 1) / available);
        if (pages > 1) {
            int pageY = layout.codeBottom - 19;
            this.addRenderableWidget(EditorButton.builder(Component.literal("<"),
                            button -> cycleArtifact(-available))
                    .bounds(layout.left + 8, pageY, 30, 17)
                    .style(EditorButton.Style.QUIET).build());
            this.addRenderableWidget(EditorButton.builder(Component.literal(">"),
                            button -> cycleArtifact(available))
                    .bounds(layout.left + SIDEBAR_WIDTH - 38, pageY, 30, 17)
                    .style(EditorButton.Style.QUIET).build());
        }
    }

    private void selectArtifact(int index) {
        this.artifactIndex = Math.max(0, Math.min(this.artifacts.size() - 1, index));
        this.lineOffset = 0;
        rebuild();
    }

    private void cycleArtifact(int delta) {
        if (this.artifacts.isEmpty()) {
            return;
        }
        this.artifactIndex = Math.floorMod(this.artifactIndex + delta, this.artifacts.size());
        this.lineOffset = 0;
        rebuild();
    }

    private void copyCurrent() {
        if (!this.artifacts.isEmpty()) {
            Minecraft.getInstance().keyboardHandler.setClipboard(current().text());
            this.status = Component.translatable("gui.avaritia_tweak.rollback.copied").getString();
            this.statusColor = EditorTheme.SUCCESS;
        }
    }

    private void rollback(SortedMap<ArtifactPath, FileObservation> confirmations) {
        if (this.committing) {
            return;
        }
        this.committing = true;
        this.message = this.messageBox.getValue();
        this.status = Component.translatable("gui.avaritia_tweak.rollback.in_progress").getString();
        this.statusColor = EditorTheme.WARNING;
        this.controller.rollback(this.revision, this.message, confirmations)
                .whenComplete((result, throwable) -> Minecraft.getInstance().execute(() -> {
                    this.committing = false;
                    if (throwable != null) {
                        this.status = Component.translatable("gui.avaritia_tweak.rollback.failed",
                                CommitResultMessages.safeMessage(throwable)).getString();
                        this.statusColor = EditorTheme.ERROR;
                    } else if (result instanceof CommitResult.Success success) {
                        this.status = Component.translatable("gui.avaritia_tweak.rollback.success",
                                success.revision().version()).getString();
                        this.statusColor = EditorTheme.SUCCESS;
                        if (this.returnAfterSuccess instanceof CustomizationEditorScreen editor) {
                            editor.refreshFromController();
                        }
                        Minecraft.getInstance().setScreen(this.returnAfterSuccess);
                    } else if (result instanceof CommitResult.ExternalFileConflict conflict) {
                        Minecraft.getInstance().setScreen(new ConflictScreen(this, conflict.conflicts(),
                                takeover -> rollback(takeover)));
                    } else {
                        this.status = CommitResultMessages.describe(result);
                        this.statusColor = EditorTheme.ERROR;
                    }
                }));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double delta) {
        if (this.artifacts.isEmpty()) {
            return false;
        }
        Layout layout = layout();
        if (layout.wide && mouseX < layout.codeX) {
            cycleArtifact(delta < 0 ? 1 : -1);
            return true;
        }
        int visible = EditorTheme.visibleCodeLines(layout.codeBottom - layout.codeTop);
        int maximum = Math.max(0, currentLines().size() - visible);
        this.lineOffset = Math.max(0, Math.min(maximum,
                this.lineOffset + (delta < 0 ? 3 : -3)));
        return true;
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Layout layout = layout();
        EditorTheme.renderBackdrop(graphics, this.width, this.height);
        EditorTheme.renderWindow(graphics, layout.left, layout.top, layout.width, layout.height,
                EditorTheme.AVARITIA_RED);
        graphics.drawString(this.font, this.title, layout.left + 10, layout.top + 12,
                EditorTheme.AVARITIA_GOLD, false);
        String target = Component.translatable("gui.avaritia_tweak.rollback.target",
                this.revision.version()).getString();
        EditorTheme.renderBadge(graphics, this.font,
                layout.left + layout.width - this.font.width(target) - 22,
                layout.top + 9, target, EditorTheme.AVARITIA_RED);
        EditorTheme.renderChangeSummary(graphics, this.font, layout.left + 8, layout.top + 34,
                this.diff.count(ChangeType.ADDED), this.diff.count(ChangeType.MODIFIED),
                this.diff.count(ChangeType.REMOVED));

        if (layout.wide) {
            EditorTheme.renderSectionHeader(graphics, this.font, layout.left + 2, layout.top + 56,
                    SIDEBAR_WIDTH - 2, Component.translatable("gui.avaritia_tweak.preview.artifacts"),
                    Integer.toString(this.artifacts.size()), EditorTheme.AVARITIA_RED);
            graphics.fill(layout.codeX - 5, layout.top + 56, layout.codeX - 4,
                    layout.codeBottom, EditorTheme.BORDER);
        }
        renderArtifact(graphics, layout);
        graphics.drawString(this.font, Component.translatable("gui.avaritia_tweak.commit_message"),
                layout.left + 8, layout.top + layout.height - 53, EditorTheme.TEXT_MUTED, false);
        if (!this.status.isEmpty()) {
            graphics.drawString(this.font, ScreenText.fit(this.font, this.status, layout.width - 16),
                    layout.left + 8, layout.top + layout.height - 13, this.statusColor, false);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderArtifact(GuiGraphics graphics, Layout layout) {
        if (this.artifacts.isEmpty()) {
            graphics.drawCenteredString(this.font,
                    Component.translatable("gui.avaritia_tweak.rollback.empty_artifacts"),
                    layout.codeX + layout.codeWidth / 2, layout.codeTop + 40, EditorTheme.WARNING);
            return;
        }
        RenderedArtifact artifact = current();
        String logicalPath = artifact.logicalPath().value();
        String state = Component.translatable("gui.avaritia_tweak.rollback.historical").getString();
        int headerX = layout.wide ? layout.codeX : layout.codeX + 34;
        int headerWidth = layout.wide ? layout.codeWidth : layout.codeWidth - 68;
        String fittedState = ScreenText.fit(this.font, state,
                Math.max(24, Math.min(120, headerWidth / 2 - 8)));
        int badgeWidth = this.font.width(fittedState) + 8;
        graphics.drawString(this.font,
                ScreenText.fit(this.font, logicalPath, Math.max(0, headerWidth - badgeWidth - 6)),
                headerX, layout.headerY, EditorTheme.TEXT, false);
        EditorTheme.renderBadge(graphics, this.font,
                headerX + headerWidth - badgeWidth,
                layout.headerY - 3, fittedState, EditorTheme.AVARITIA_RED);
        String destination = this.pathResolver.resolveArtifact(artifact.logicalPath()).toString();
        graphics.drawString(this.font, ScreenText.fit(this.font, destination, layout.codeWidth - 4),
                layout.codeX, layout.headerY + 13, EditorTheme.TEXT_FAINT, false);
        String metadata = (this.artifactIndex + 1) + "/" + this.artifacts.size()
                + "  |  sha256 " + artifact.sha256().substring(0, 12);
        graphics.drawString(this.font, ScreenText.fit(this.font, metadata, layout.codeWidth - 4),
                layout.codeX, layout.headerY + 24, EditorTheme.AVARITIA_RED, false);
        EditorTheme.renderCodeViewport(graphics, this.font, layout.codeX, layout.codeTop,
                layout.codeWidth, layout.codeBottom - layout.codeTop, currentLines(), this.lineOffset,
                logicalPath, Optional.empty());
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(this.previous);
    }

    private void rebuild() {
        this.clearWidgets();
        this.init();
    }

    private RenderedArtifact current() {
        return this.artifacts.get(this.artifactIndex);
    }

    private List<String> currentLines() {
        return this.artifacts.isEmpty() ? List.of() : List.of(current().text().split("\\n", -1));
    }

    private Layout layout() {
        EditorUiScale.Frame frame = EditorUiScale.fit(this.width, this.height, 720, 390);
        int panelWidth = frame.width();
        int panelHeight = frame.height();
        int left = frame.left();
        int top = frame.top();
        boolean wide = panelWidth >= 560;
        int codeX = wide ? left + SIDEBAR_WIDTH + 8 : left + 8;
        int codeWidth = wide ? panelWidth - SIDEBAR_WIDTH - 16 : panelWidth - 16;
        int headerY = wide ? top + 64 : top + 58;
        int codeTop = wide ? top + 96 : top + 94;
        int codeBottom = top + panelHeight - 60;
        return new Layout(left, top, panelWidth, panelHeight, wide,
                codeX, codeWidth, headerY, codeTop, codeBottom);
    }

    private static String artifactName(ArtifactPath path) {
        String value = path.value();
        int separator = value.lastIndexOf('/');
        return separator < 0 ? value : value.substring(separator + 1);
    }

    private record Layout(int left, int top, int width, int height, boolean wide,
                          int codeX, int codeWidth, int headerY, int codeTop, int codeBottom) {
    }
}
