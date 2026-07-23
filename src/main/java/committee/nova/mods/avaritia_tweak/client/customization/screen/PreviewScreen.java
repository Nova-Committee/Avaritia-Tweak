package committee.nova.mods.avaritia_tweak.client.customization.screen;

import committee.nova.mods.avaritia_tweak.client.customization.ClientCustomizationServices;
import committee.nova.mods.avaritia_tweak.client.customization.EditorController;
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
    private static final int SIDEBAR_WIDTH = 188;

    private final Screen previous;
    private final EditorController controller;
    private final ManagedPathResolver pathResolver;
    private final List<PreviewArtifact> artifacts;
    private int artifactIndex;
    private int lineOffset;
    private EditBox messageBox;
    private String message = "";
    private String status = "";
    private int statusColor = EditorTheme.TEXT_MUTED;
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
        Layout layout = layout();
        int buttonY = layout.top + layout.height - 42;
        int actionWidth = Math.min(92, Math.max(72, layout.width / 7));
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
        this.addRenderableWidget(EditorButton.builder(Component.translatable("gui.avaritia_tweak.commit_local"),
                        button -> commit(new TreeMap<>()))
                .bounds(copyX + actionWidth, buttonY, actionWidth, 20)
                .style(EditorButton.Style.PRIMARY).build());

        if (layout.wide) {
            addArtifactList(layout);
        } else {
            this.addRenderableWidget(EditorButton.builder(Component.literal("<"), button -> cycleArtifact(-1))
                    .bounds(layout.left + 8, layout.top + 31, 30, 18)
                    .style(EditorButton.Style.QUIET).build());
            this.addRenderableWidget(EditorButton.builder(Component.literal(">"), button -> cycleArtifact(1))
                    .bounds(layout.left + layout.width - 38, layout.top + 31, 30, 18)
                    .style(EditorButton.Style.QUIET).build());
        }
    }

    private void addArtifactList(Layout layout) {
        int listTop = layout.top + 52;
        int available = Math.max(1, (layout.codeBottom - listTop) / 20);
        int page = this.artifactIndex / available;
        int start = page * available;
        int end = Math.min(this.artifacts.size(), start + available);
        for (int index = start; index < end; index++) {
            PreviewArtifact artifact = this.artifacts.get(index);
            String marker = artifact.change().map(EditorTheme::changeMark).orElse(" ");
            String label = marker + "  " + artifactName(artifact.artifact().logicalPath());
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
            this.addRenderableWidget(EditorButton.builder(Component.literal("<"), button -> cycleArtifact(-available))
                    .bounds(layout.left + 8, pageY, 30, 17).style(EditorButton.Style.QUIET).build());
            this.addRenderableWidget(EditorButton.builder(Component.literal(">"), button -> cycleArtifact(available))
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
            Minecraft.getInstance().keyboardHandler.setClipboard(current().artifact().text());
            this.status = Component.translatable("gui.avaritia_tweak.preview.copied").getString();
            this.statusColor = EditorTheme.SUCCESS;
        }
    }

    private void commit(SortedMap<ArtifactPath, FileObservation> confirmations) {
        if (this.committing) {
            return;
        }
        this.committing = true;
        this.message = this.messageBox.getValue();
        this.status = Component.translatable("gui.avaritia_tweak.commit.in_progress").getString();
        this.statusColor = EditorTheme.WARNING;
        this.controller.commit(this.message, confirmations).whenComplete((result, throwable) ->
                Minecraft.getInstance().execute(() -> {
                    this.committing = false;
                    if (throwable != null) {
                        this.status = Component.translatable("gui.avaritia_tweak.commit.failed",
                                CommitResultMessages.safeMessage(throwable)).getString();
                        this.statusColor = EditorTheme.ERROR;
                    } else if (result instanceof CommitResult.Success success) {
                        this.status = Component.translatable("gui.avaritia_tweak.commit.success",
                                success.revision().version()).getString();
                        this.statusColor = EditorTheme.SUCCESS;
                        refreshEditor(this.previous);
                        Minecraft.getInstance().setScreen(this.previous);
                    } else if (result instanceof CommitResult.ExternalFileConflict conflict) {
                        Minecraft.getInstance().setScreen(new ConflictScreen(this, conflict.conflicts(),
                                takeover -> commit(takeover)));
                    } else {
                        this.status = CommitResultMessages.describe(result);
                        this.statusColor = EditorTheme.ERROR;
                    }
                }));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
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
        String readOnly = Component.translatable("gui.avaritia_tweak.preview.read_only").getString();
        EditorTheme.renderBadge(graphics, this.font,
                layout.left + layout.width - this.font.width(readOnly) - 22,
                layout.top + 9, readOnly, EditorTheme.AVARITIA_CYAN);

        if (layout.wide) {
            EditorTheme.renderSectionHeader(graphics, this.font, layout.left + 2, layout.top + 32,
                    SIDEBAR_WIDTH - 2, Component.translatable("gui.avaritia_tweak.preview.artifacts"),
                    Integer.toString(this.artifacts.size()), EditorTheme.AVARITIA_CYAN);
            graphics.fill(layout.codeX - 5, layout.top + 32, layout.codeX - 4,
                    layout.codeBottom, EditorTheme.BORDER);
        }
        renderArtifact(graphics, layout);
        graphics.drawString(this.font, Component.translatable("gui.avaritia_tweak.commit_message"),
                layout.left + 8, layout.top + layout.height - 53, EditorTheme.TEXT_MUTED, false);
        if (!this.status.isEmpty()) {
            graphics.drawString(this.font,
                    ScreenText.fit(this.font, this.status, layout.width - 16),
                    layout.left + 8, layout.top + layout.height - 13, this.statusColor, false);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderArtifact(GuiGraphics graphics, Layout layout) {
        if (this.artifacts.isEmpty()) {
            graphics.drawCenteredString(this.font,
                    Component.translatable("gui.avaritia_tweak.preview.empty"),
                    layout.codeX + layout.codeWidth / 2, layout.codeTop + 40, EditorTheme.WARNING);
            return;
        }
        PreviewArtifact current = current();
        RenderedArtifact artifact = current.artifact();
        String logicalPath = artifact.logicalPath().value();
        graphics.drawString(this.font, ScreenText.fit(this.font, logicalPath, layout.codeWidth - 90),
                layout.codeX, layout.headerY, EditorTheme.TEXT, false);
        String state = current.change().map(type -> Component.translatable(
                "gui.avaritia_tweak.change." + type.name().toLowerCase(java.util.Locale.ROOT)).getString())
                .orElse(Component.translatable("gui.avaritia_tweak.change.unchanged").getString());
        int stateColor = current.change().map(EditorTheme::changeColor).orElse(EditorTheme.TEXT_MUTED);
        EditorTheme.renderBadge(graphics, this.font,
                layout.codeX + layout.codeWidth - this.font.width(state) - 10,
                layout.headerY - 3, state, stateColor);
        String destination = this.pathResolver.resolveArtifact(artifact.logicalPath()).toString();
        graphics.drawString(this.font, ScreenText.fit(this.font, destination, layout.codeWidth - 4),
                layout.codeX, layout.headerY + 13, EditorTheme.TEXT_FAINT, false);
        String metadata = (this.artifactIndex + 1) + "/" + this.artifacts.size()
                + "  •  sha256 " + artifact.sha256().substring(0, 12)
                + (current.previousContent() ? "  •  previous" : "");
        graphics.drawString(this.font, ScreenText.fit(this.font, metadata, layout.codeWidth - 4),
                layout.codeX, layout.headerY + 24, stateColor, false);
        EditorTheme.renderCodeViewport(graphics, this.font, layout.codeX, layout.codeTop,
                layout.codeWidth, layout.codeBottom - layout.codeTop, currentLines(), this.lineOffset,
                logicalPath, current.change());
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(this.previous);
    }

    private void rebuild() {
        this.clearWidgets();
        this.init();
    }

    private PreviewArtifact current() {
        return this.artifacts.get(this.artifactIndex);
    }

    private List<String> currentLines() {
        return this.artifacts.isEmpty()
                ? List.of()
                : List.of(current().artifact().text().split("\\n", -1));
    }

    private Layout layout() {
        int panelWidth = Math.min(720, this.width - 12);
        int panelHeight = Math.min(390, this.height - 12);
        int left = (this.width - panelWidth) / 2;
        int top = (this.height - panelHeight) / 2;
        boolean wide = panelWidth >= 560;
        int codeX = wide ? left + SIDEBAR_WIDTH + 8 : left + 8;
        int codeWidth = wide ? panelWidth - SIDEBAR_WIDTH - 16 : panelWidth - 16;
        int headerY = wide ? top + 40 : top + 36;
        int codeTop = wide ? top + 78 : top + 76;
        int codeBottom = top + panelHeight - 60;
        return new Layout(left, top, panelWidth, panelHeight, wide,
                codeX, codeWidth, headerY, codeTop, codeBottom);
    }

    private static String artifactName(ArtifactPath path) {
        String value = path.value();
        int separator = value.lastIndexOf('/');
        return separator < 0 ? value : value.substring(separator + 1);
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

    private static void refreshEditor(Screen screen) {
        if (screen instanceof CustomizationEditorScreen editor) {
            editor.refreshFromController();
        }
    }

    private record PreviewArtifact(RenderedArtifact artifact, Optional<ChangeType> change,
                                   boolean previousContent) {
    }

    private record Layout(int left, int top, int width, int height, boolean wide,
                          int codeX, int codeWidth, int headerY, int codeTop, int codeBottom) {
    }
}
