package committee.nova.mods.avaritia_tweak.client.customization.screen;

import committee.nova.mods.avaritia_tweak.client.customization.EditorController;
import committee.nova.mods.avaritia_tweak.customization.diff.ChangeType;
import committee.nova.mods.avaritia_tweak.customization.diff.WorkspaceDiff;
import committee.nova.mods.avaritia_tweak.customization.diff.WorkspaceDiffer;
import committee.nova.mods.avaritia_tweak.customization.history.RepositoryException;
import committee.nova.mods.avaritia_tweak.customization.history.Revision;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class HistoryScreen extends Screen {
    private static final int PAGE_SIZE = 8;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());
    private final Screen previous;
    private final EditorController controller;
    private final WorkspaceDiffer differ = new WorkspaceDiffer();
    private final List<Revision> revisions = new ArrayList<>();
    private Revision selected;
    private int page;
    private String status = "";
    private boolean loaded;

    public HistoryScreen(Screen previous, EditorController controller) {
        super(Component.translatable("gui.avaritia_tweak.history.title"));
        this.previous = previous;
        this.controller = controller;
    }

    @Override
    protected void init() {
        loadHistory();
        int panelWidth = Math.min(460, this.width - 12);
        int panelHeight = Math.min(250, this.height - 12);
        int left = (this.width - panelWidth) / 2;
        int top = (this.height - panelHeight) / 2;
        int listWidth = Math.min(224, Math.max(132, panelWidth / 2));
        int start = this.page * PAGE_SIZE;
        for (int index = start; index < Math.min(this.revisions.size(), start + PAGE_SIZE); index++) {
            Revision revision = this.revisions.get(index);
            int row = index - start;
            String marker = revision == this.selected ? "> " : "";
            Component label = Component.literal(marker + "v" + revision.version() + "  "
                    + ScreenText.ellipsize(revision.message(), Math.max(8, listWidth / 7 - 6)));
            this.addRenderableWidget(Button.builder(label, button -> {
                this.selected = revision;
                rebuild();
            }).bounds(left + 8, top + 28 + row * 21, listWidth - 16, 19).build());
        }
        int bottom = top + panelHeight - 28;
        this.addRenderableWidget(Button.builder(Component.literal("<"), button -> {
            this.page = Math.max(0, this.page - 1);
            rebuild();
        }).bounds(left + 8, bottom, 32, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal(">"), button -> {
            int max = Math.max(0, (this.revisions.size() - 1) / PAGE_SIZE);
            this.page = Math.min(max, this.page + 1);
            rebuild();
        }).bounds(left + listWidth - 40, bottom, 32, 20).build());
        Button inspect = this.addRenderableWidget(Button.builder(
                Component.translatable("gui.avaritia_tweak.history.inspect"), button -> openSelected())
                .bounds(left + panelWidth - 160, bottom, 72, 20).build());
        inspect.active = this.selected != null;
        this.addRenderableWidget(Button.builder(Component.translatable("gui.avaritia_tweak.close"),
                button -> onClose()).bounds(left + panelWidth - 82, bottom, 74, 20).build());
    }

    private void loadHistory() {
        if (this.loaded) {
            return;
        }
        this.loaded = true;
        try {
            this.revisions.addAll(this.controller.history());
            Collections.reverse(this.revisions);
            if (!this.revisions.isEmpty()) {
                this.selected = this.revisions.get(0);
            }
        } catch (RepositoryException exception) {
            this.status = exception.getMessage() == null
                    ? exception.getClass().getSimpleName()
                    : exception.getMessage();
        }
    }

    private void openSelected() {
        if (this.selected != null) {
            Minecraft.getInstance().setScreen(new RollbackPreviewScreen(
                    this, this.previous, this.controller, this.selected));
        }
    }

    private void rebuild() {
        this.clearWidgets();
        this.init();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int max = Math.max(0, (this.revisions.size() - 1) / PAGE_SIZE);
        this.page = Math.max(0, Math.min(max, this.page + (delta < 0 ? 1 : -1)));
        rebuild();
        return true;
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        int panelWidth = Math.min(460, this.width - 12);
        int panelHeight = Math.min(250, this.height - 12);
        int left = (this.width - panelWidth) / 2;
        int top = (this.height - panelHeight) / 2;
        int listWidth = Math.min(224, Math.max(132, panelWidth / 2));
        graphics.fill(left, top, left + panelWidth, top + panelHeight, 0xf0171a21);
        graphics.drawString(this.font, this.title, left + 8, top + 9, 0xfff0c66a, false);
        int max = Math.max(0, (this.revisions.size() - 1) / PAGE_SIZE);
        graphics.drawCenteredString(this.font, (this.page + 1) + "/" + (max + 1),
                left + listWidth / 2, top + panelHeight - 22, 0xffaeb6c6);
        if (this.selected == null) {
            Component empty = this.status.isEmpty()
                    ? Component.translatable("gui.avaritia_tweak.history.empty")
                    : Component.literal(ScreenText.ellipsize(this.status, 44));
            graphics.drawWordWrap(this.font, empty, left + listWidth + 8, top + 30,
                    panelWidth - listWidth - 16, 0xffffc76b);
        } else {
            renderDetails(graphics, left + listWidth + 8, top + 30,
                    panelWidth - listWidth - 16);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderDetails(GuiGraphics graphics, int x, int y, int width) {
        WorkspaceDiff rollbackDiff = this.differ.diff(this.controller.committed(), this.selected.snapshot());
        graphics.drawString(this.font, "Version " + this.selected.version(), x, y,
                0xff78d6a3, false);
        graphics.drawString(this.font, TIME_FORMAT.format(this.selected.committedAt()), x, y + 14,
                0xff8d96a8, false);
        graphics.drawWordWrap(this.font, Component.literal(this.selected.message()), x, y + 30,
                width, 0xffd7dbe5);
        graphics.drawString(this.font, "Original: +" + this.selected.summary().added()
                + "  ~" + this.selected.summary().modified()
                + "  -" + this.selected.summary().removed(), x, y + 68, 0xffaeb6c6, false);
        graphics.drawString(this.font, "Rollback: +" + rollbackDiff.count(ChangeType.ADDED)
                + "  ~" + rollbackDiff.count(ChangeType.MODIFIED)
                + "  -" + rollbackDiff.count(ChangeType.REMOVED), x, y + 82, 0xfff0c66a, false);
        graphics.drawString(this.font, "Artifacts: " + this.selected.artifacts().artifacts().size(),
                x, y + 96, 0xffaeb6c6, false);
        this.selected.rollbackOf().ifPresent(version -> graphics.drawString(this.font,
                "Rollback of v" + version, x, y + 110, 0xffffc76b, false));
        int changeY = y + 126;
        for (int index = 0; index < Math.min(5, rollbackDiff.entries().size()); index++) {
            var change = rollbackDiff.entries().get(index);
            graphics.drawString(this.font,
                    ScreenText.ellipsize(change.type() + " " + change.key(), Math.max(12, width / 6)),
                    x, changeY + index * 11, 0xffc8ceda, false);
        }
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(this.previous);
    }

}
