package committee.nova.mods.avaritia_tweak.client.customization.screen;

import committee.nova.mods.avaritia_tweak.customization.commit.FileConflict;
import committee.nova.mods.avaritia_tweak.customization.commit.FileObservation;
import committee.nova.mods.avaritia_tweak.customization.render.ArtifactPath;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
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

    public ConflictScreen(Screen previous, List<FileConflict> conflicts,
                          Consumer<SortedMap<ArtifactPath, FileObservation>> onTakeover) {
        super(Component.translatable("gui.avaritia_tweak.conflict.title"));
        this.previous = previous;
        this.conflicts = List.copyOf(conflicts);
        this.onTakeover = onTakeover;
    }

    @Override
    protected void init() {
        int panelWidth = Math.min(380, this.width - 12);
        int panelHeight = Math.min(208, this.height - 12);
        int left = (this.width - panelWidth) / 2;
        int top = (this.height - panelHeight) / 2;
        int gap = 4;
        int buttonWidth = (panelWidth - 16 - gap * 3) / 4;
        int buttonY = top + panelHeight - 30;
        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.avaritia_tweak.conflict.takeover"),
                button -> takeover()).bounds(left + 8, buttonY, buttonWidth, 20).build());
        this.addRenderableWidget(Button.builder(Component.translatable("gui.avaritia_tweak.copy_paths"),
                button -> Minecraft.getInstance().keyboardHandler.setClipboard(paths()))
                .bounds(left + 8 + (buttonWidth + gap), buttonY, buttonWidth, 20).build());
        this.addRenderableWidget(Button.builder(Component.translatable("gui.avaritia_tweak.open_path"),
                button -> openFirstPath())
                .bounds(left + 8 + (buttonWidth + gap) * 2, buttonY, buttonWidth, 20).build());
        this.addRenderableWidget(Button.builder(Component.translatable("gui.avaritia_tweak.cancel"),
                button -> onClose())
                .bounds(left + 8 + (buttonWidth + gap) * 3, buttonY, buttonWidth, 20).build());
    }

    private void takeover() {
        TreeMap<ArtifactPath, FileObservation> confirmations = new TreeMap<>();
        this.conflicts.forEach(conflict ->
                confirmations.put(conflict.logicalPath(), conflict.actual()));
        Minecraft.getInstance().setScreen(this.previous);
        this.onTakeover.accept(confirmations);
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
        this.renderBackground(graphics);
        int panelWidth = Math.min(380, this.width - 12);
        int panelHeight = Math.min(208, this.height - 12);
        int left = (this.width - panelWidth) / 2;
        int top = (this.height - panelHeight) / 2;
        graphics.fill(left, top, left + panelWidth, top + panelHeight, 0xf0171a21);
        graphics.drawString(this.font, this.title, left + 8, top + 8, 0xffff6b6b, false);
        graphics.drawWordWrap(this.font,
                Component.translatable("gui.avaritia_tweak.conflict.explanation"),
                left + 8, top + 24, panelWidth - 16, 0xffffc76b);
        int y = top + 52;
        int rows = Math.max(1, Math.min(6, (panelHeight - 92) / 20));
        for (int index = 0; index < Math.min(rows, this.conflicts.size()); index++) {
            FileConflict conflict = this.conflicts.get(index);
            graphics.drawString(this.font,
                    ScreenText.ellipsize(conflict.reason() + "  " + conflict.logicalPath().value(),
                            Math.max(18, panelWidth / 7)),
                    left + 8, y, 0xffd7dbe5, false);
            graphics.drawString(this.font,
                    ScreenText.ellipsize(conflict.actualPath().toString(), Math.max(18, panelWidth / 6)),
                    left + 16, y + 10, 0xff8d96a8, false);
            y += 20;
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(this.previous);
    }

}
