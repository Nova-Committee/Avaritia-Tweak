package committee.nova.mods.avaritia_tweak.client.customization.screen;

import committee.nova.mods.avaritia_tweak.customization.commit.FileConflict;
import committee.nova.mods.avaritia_tweak.customization.commit.FileObservation;
import committee.nova.mods.avaritia_tweak.customization.render.ArtifactPath;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public final class ConflictScreen extends Screen {
    private final Screen previous;
    private final List<FileConflict> conflicts;
    private final Consumer<SortedMap<ArtifactPath, FileObservation>> onTakeover;
    private String status = "";

    public ConflictScreen(Screen previous, List<FileConflict> conflicts,
                          Consumer<SortedMap<ArtifactPath, FileObservation>> onTakeover) {
        super(Component.translatable("gui.avaritia_tweak.conflict.title"));
        this.previous = previous;
        this.conflicts = List.copyOf(conflicts);
        this.onTakeover = onTakeover;
    }

    @Override
    protected void init() {
        Layout layout = layout();
        int gap = 4;
        int buttonWidth = (layout.width - 16 - gap * 3) / 4;
        int buttonY = layout.top + layout.height - 30;
        this.addRenderableWidget(EditorButton.builder(
                        Component.translatable("gui.avaritia_tweak.conflict.takeover"),
                        button -> takeover())
                .bounds(layout.left + 8, buttonY, buttonWidth, 20)
                .style(EditorButton.Style.DANGER).build());
        this.addRenderableWidget(EditorButton.builder(
                        Component.translatable("gui.avaritia_tweak.copy_paths"), button -> copyPaths())
                .bounds(layout.left + 8 + buttonWidth + gap, buttonY, buttonWidth, 20)
                .style(EditorButton.Style.QUIET).build());
        EditorButton open = this.addRenderableWidget(EditorButton.builder(
                        Component.translatable("gui.avaritia_tweak.open_path"), button -> openFirstPath())
                .bounds(layout.left + 8 + (buttonWidth + gap) * 2, buttonY, buttonWidth, 20)
                .style(EditorButton.Style.DEFAULT).build());
        open.active = !this.conflicts.isEmpty();
        this.addRenderableWidget(EditorButton.builder(
                        Component.translatable("gui.avaritia_tweak.cancel"), button -> onClose())
                .bounds(layout.left + 8 + (buttonWidth + gap) * 3, buttonY, buttonWidth, 20)
                .style(EditorButton.Style.QUIET).build());
    }

    private void takeover() {
        TreeMap<ArtifactPath, FileObservation> confirmations = new TreeMap<>();
        this.conflicts.forEach(conflict -> confirmations.put(conflict.logicalPath(), conflict.actual()));
        Minecraft.getInstance().setScreen(this.previous);
        this.onTakeover.accept(confirmations);
    }

    private void copyPaths() {
        Minecraft.getInstance().keyboardHandler.setClipboard(paths());
        this.status = Component.translatable("gui.avaritia_tweak.conflict.paths_copied").getString();
    }

    private void openFirstPath() {
        if (!this.conflicts.isEmpty()) {
            Path path = this.conflicts.get(0).actualPath();
            Path target = path.getParent() == null ? path : path.getParent();
            Util.getPlatform().openFile(target.toFile());
        }
    }

    private String paths() {
        return this.conflicts.stream().map(conflict -> conflict.actualPath().toString())
                .collect(Collectors.joining(System.lineSeparator()));
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Layout layout = layout();
        EditorTheme.renderBackdrop(graphics, this.width, this.height);
        EditorTheme.renderWindow(graphics, layout.left, layout.top, layout.width, layout.height,
                EditorTheme.ERROR);
        graphics.drawString(this.font, this.title, layout.left + 10, layout.top + 12,
                EditorTheme.ERROR, false);
        String badge = Component.translatable("gui.avaritia_tweak.conflict.count",
                this.conflicts.size()).getString();
        EditorTheme.renderBadge(graphics, this.font,
                layout.left + layout.width - this.font.width(badge) - 22,
                layout.top + 9, badge, EditorTheme.ERROR);

        int contentX = layout.left + 10;
        int contentWidth = layout.width - 20;
        graphics.fill(contentX - 2, layout.top + 34, contentX + contentWidth + 2,
                layout.top + 76, 0xff35252a);
        graphics.fill(contentX - 2, layout.top + 34, contentX + 1,
                layout.top + 76, EditorTheme.ERROR);
        graphics.drawWordWrap(this.font,
                Component.translatable("gui.avaritia_tweak.conflict.explanation"),
                contentX + 6, layout.top + 42, contentWidth - 12, EditorTheme.WARNING);

        int y = layout.top + 84;
        int rows = Math.max(1, Math.min(6, (layout.height - 132) / 30));
        for (int index = 0; index < Math.min(rows, this.conflicts.size()); index++) {
            FileConflict conflict = this.conflicts.get(index);
            graphics.fill(contentX, y, contentX + contentWidth, y + 26, EditorTheme.BORDER_DARK);
            graphics.fill(contentX + 1, y + 1, contentX + contentWidth - 1, y + 25,
                    index % 2 == 0 ? EditorTheme.PANEL : EditorTheme.PANEL_RAISED);
            graphics.fill(contentX + 1, y + 1, contentX + 4, y + 25, EditorTheme.ERROR);
            graphics.drawString(this.font,
                    ScreenText.fit(this.font,
                            conflict.reason() + "  |  " + conflict.logicalPath().value(),
                            contentWidth - 14),
                    contentX + 8, y + 4, EditorTheme.TEXT, false);
            graphics.drawString(this.font,
                    ScreenText.fit(this.font, conflict.actualPath().toString(), contentWidth - 18),
                    contentX + 8, y + 15, EditorTheme.TEXT_FAINT, false);
            y += 30;
        }
        if (!this.status.isEmpty()) {
            graphics.drawString(this.font, ScreenText.fit(this.font, this.status, contentWidth),
                    contentX, layout.top + layout.height - 42, EditorTheme.SUCCESS, false);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private Layout layout() {
        EditorUiScale.Frame frame = EditorUiScale.fit(this.width, this.height, 560, 310);
        int panelWidth = frame.width();
        int panelHeight = frame.height();
        int left = frame.left();
        int top = frame.top();
        return new Layout(left, top, panelWidth, panelHeight);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(this.previous);
    }

    private record Layout(int left, int top, int width, int height) {
    }
}
