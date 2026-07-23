package committee.nova.mods.avaritia_tweak.client.customization.screen;

import committee.nova.mods.avaritia_tweak.client.customization.EditorController;
import committee.nova.mods.avaritia_tweak.customization.diff.ChangeType;
import committee.nova.mods.avaritia_tweak.customization.diff.EntryChange;
import committee.nova.mods.avaritia_tweak.customization.diff.WorkspaceDiff;
import committee.nova.mods.avaritia_tweak.customization.diff.WorkspaceDiffer;
import committee.nova.mods.avaritia_tweak.customization.history.RepositoryException;
import committee.nova.mods.avaritia_tweak.customization.history.Revision;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class HistoryScreen extends Screen {
    private static final int TIMELINE_WIDTH = 236;
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
        Layout layout = layout();
        int pageSize = pageSize(layout);
        int maxPage = maxPage(pageSize);
        this.page = Math.min(this.page, maxPage);
        int start = this.page * pageSize;
        for (int index = start; index < Math.min(this.revisions.size(), start + pageSize); index++) {
            Revision revision = this.revisions.get(index);
            int row = index - start;
            String message = ScreenText.fit(this.font, revision.message(), layout.timelineWidth - 74);
            Component label = Component.literal("v" + revision.version() + "  " + message);
            this.addRenderableWidget(EditorButton.builder(label, button -> {
                this.selected = revision;
                rebuild();
            }).bounds(layout.left + 28, layout.listTop + row * 22,
                    layout.timelineWidth - 38, 20)
                    .style(EditorButton.Style.LIST).selected(revision == this.selected).build());
        }

        int actionY = layout.top + layout.height - 30;
        this.addRenderableWidget(EditorButton.builder(Component.literal("<"), button -> {
            this.page = Math.max(0, this.page - 1);
            rebuild();
        }).bounds(layout.left + 8, actionY, 30, 20).style(EditorButton.Style.QUIET).build());
        this.addRenderableWidget(EditorButton.builder(Component.literal(">"), button -> {
            this.page = Math.min(maxPage(pageSize), this.page + 1);
            rebuild();
        }).bounds(layout.left + 44, actionY, 30, 20)
                .style(EditorButton.Style.QUIET).build());

        int closeWidth = 76;
        int inspectWidth = 96;
        EditorButton inspect = this.addRenderableWidget(EditorButton.builder(
                        Component.translatable("gui.avaritia_tweak.history.inspect"), button -> openSelected())
                .bounds(layout.left + layout.width - closeWidth - inspectWidth - 18,
                        actionY, inspectWidth, 20).style(EditorButton.Style.PRIMARY).build());
        inspect.active = this.selected != null;
        this.addRenderableWidget(EditorButton.builder(Component.translatable("gui.avaritia_tweak.close"),
                        button -> onClose())
                .bounds(layout.left + layout.width - closeWidth - 8, actionY, closeWidth, 20)
                .style(EditorButton.Style.QUIET).build());
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
        Layout layout = layout();
        if (mouseX > layout.left + layout.timelineWidth) {
            return false;
        }
        int pageSize = pageSize(layout);
        this.page = Math.max(0, Math.min(maxPage(pageSize),
                this.page + (delta < 0 ? 1 : -1)));
        rebuild();
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
        String local = Component.translatable("gui.avaritia_tweak.history.local").getString();
        EditorTheme.renderBadge(graphics, this.font,
                layout.left + layout.width - this.font.width(local) - 22,
                layout.top + 9, local, EditorTheme.AVARITIA_CYAN);

        EditorTheme.renderSectionHeader(graphics, this.font, layout.left + 2, layout.top + 34,
                layout.timelineWidth - 2, Component.translatable("gui.avaritia_tweak.history.timeline"),
                Integer.toString(this.revisions.size()), EditorTheme.AVARITIA_CYAN);
        renderTimeline(graphics, layout);

        if (layout.wide) {
            int detailsX = layout.detailsX;
            int detailsWidth = layout.detailsWidth;
            EditorTheme.renderPanel(graphics, detailsX - 6, layout.top + 34,
                    detailsWidth + 12, layout.detailsBottom - layout.top - 34, EditorTheme.AVARITIA_GOLD);
            EditorTheme.renderCanvasGrid(graphics, detailsX - 2, layout.top + 38,
                    detailsWidth + 4, layout.detailsBottom - layout.top - 42);
            EditorTheme.renderSectionHeader(graphics, this.font, detailsX - 4, layout.top + 36,
                    detailsWidth + 8, Component.translatable("gui.avaritia_tweak.history.details"),
                    this.selected == null ? "" : "v" + this.selected.version(), EditorTheme.AVARITIA_GOLD);

            if (this.selected == null) {
                Component empty = this.status.isEmpty()
                        ? Component.translatable("gui.avaritia_tweak.history.empty")
                        : Component.literal(ScreenText.fit(this.font, this.status, detailsWidth));
                graphics.drawWordWrap(this.font, empty, detailsX, layout.top + 64,
                        detailsWidth, this.status.isEmpty() ? EditorTheme.TEXT_MUTED : EditorTheme.ERROR);
            } else {
                renderDetails(graphics, layout);
            }
        } else {
            renderCompactDetails(graphics, layout);
        }

        int pageSize = pageSize(layout);
        int pageX = layout.wide ? layout.left + 112 : layout.left + 94;
        graphics.drawCenteredString(this.font, (this.page + 1) + "/" + (maxPage(pageSize) + 1),
                pageX, layout.top + layout.height - 24, EditorTheme.TEXT_MUTED);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderTimeline(GuiGraphics graphics, Layout layout) {
        int pageSize = pageSize(layout);
        int start = this.page * pageSize;
        int count = Math.min(pageSize, this.revisions.size() - start);
        if (count <= 0) {
            return;
        }
        int lineX = layout.left + 18;
        graphics.fill(lineX, layout.listTop + 9, lineX + 2,
                layout.listTop + (count - 1) * 22 + 11, 0xff59616d);
        for (int row = 0; row < count; row++) {
            Revision revision = this.revisions.get(start + row);
            int y = layout.listTop + row * 22 + 7;
            int color = revision == this.selected ? EditorTheme.AVARITIA_CYAN : EditorTheme.BORDER;
            graphics.fill(lineX - 3, y, lineX + 5, y + 8, EditorTheme.BORDER_DARK);
            graphics.fill(lineX - 2, y + 1, lineX + 4, y + 7, color);
            if (revision.rollbackOf().isPresent()) {
                graphics.fill(lineX, y + 3, lineX + 2, y + 5, EditorTheme.AVARITIA_RED);
            }
        }
    }

    private void renderDetails(GuiGraphics graphics, Layout layout) {
        int x = layout.detailsX;
        int width = layout.detailsWidth;
        int y = layout.top + 64;
        WorkspaceDiff rollbackDiff = this.differ.diff(this.controller.committed(), this.selected.snapshot());

        graphics.drawString(this.font, Component.translatable("gui.avaritia_tweak.history.version",
                this.selected.version()), x, y, EditorTheme.SUCCESS, false);
        graphics.drawString(this.font, TIME_FORMAT.format(this.selected.committedAt()),
                x, y + 13, EditorTheme.TEXT_FAINT, false);
        this.selected.rollbackOf().ifPresent(version -> EditorTheme.renderBadge(graphics, this.font,
                x + width - this.font.width("v" + version) - 62, y - 3,
                Component.translatable("gui.avaritia_tweak.history.rollback_of", version).getString(),
                EditorTheme.AVARITIA_RED));

        graphics.drawWordWrap(this.font, Component.literal(this.selected.message()),
                x, y + 31, width, EditorTheme.TEXT);
        int summaryY = y + 68;
        graphics.drawString(this.font, Component.translatable("gui.avaritia_tweak.history.original"),
                x, summaryY, EditorTheme.TEXT_MUTED, false);
        EditorTheme.renderChangeSummary(graphics, this.font, x, summaryY + 12,
                this.selected.summary().added(), this.selected.summary().modified(),
                this.selected.summary().removed());
        graphics.drawString(this.font, Component.translatable("gui.avaritia_tweak.history.rollback_impact"),
                x, summaryY + 32, EditorTheme.TEXT_MUTED, false);
        EditorTheme.renderChangeSummary(graphics, this.font, x, summaryY + 44,
                rollbackDiff.count(ChangeType.ADDED), rollbackDiff.count(ChangeType.MODIFIED),
                rollbackDiff.count(ChangeType.REMOVED));

        int changeY = summaryY + 68;
        graphics.drawString(this.font, Component.translatable("gui.avaritia_tweak.history.affected_entries",
                        rollbackDiff.entries().size(), this.selected.artifacts().artifacts().size()),
                x, changeY, EditorTheme.TEXT_MUTED, false);
        int available = Math.max(0, Math.min(7,
                (layout.detailsBottom - changeY - 20) / 12));
        for (int index = 0; index < Math.min(available, rollbackDiff.entries().size()); index++) {
            EntryChange change = rollbackDiff.entries().get(index);
            String value = EditorTheme.changeMark(change.type()) + "  " + change.key().id();
            graphics.drawString(this.font, ScreenText.fit(this.font, value, width),
                    x, changeY + 14 + index * 12, EditorTheme.changeColor(change.type()), false);
        }
    }

    private void renderCompactDetails(GuiGraphics graphics, Layout layout) {
        int y = layout.detailsBottom + 6;
        int width = layout.width - 20;
        graphics.fill(layout.left + 8, layout.detailsBottom + 2,
                layout.left + layout.width - 8, layout.top + layout.height - 36, EditorTheme.PANEL_DARK);
        graphics.fill(layout.left + 8, layout.detailsBottom + 2,
                layout.left + 11, layout.top + layout.height - 36, EditorTheme.AVARITIA_GOLD);
        if (this.selected == null) {
            Component empty = this.status.isEmpty()
                    ? Component.translatable("gui.avaritia_tweak.history.empty")
                    : Component.literal(this.status);
            graphics.drawString(this.font, ScreenText.fit(this.font, empty.getString(), width - 10),
                    layout.left + 16, y + 7, EditorTheme.TEXT_MUTED, false);
            return;
        }
        graphics.drawString(this.font, "v" + this.selected.version() + "  "
                        + ScreenText.fit(this.font, this.selected.message(), width - 82),
                layout.left + 16, y + 3, EditorTheme.TEXT, false);
        graphics.drawString(this.font, TIME_FORMAT.format(this.selected.committedAt()),
                layout.left + 16, y + 15, EditorTheme.TEXT_FAINT, false);
    }

    private int pageSize(Layout layout) {
        return Math.max(1, (layout.detailsBottom - layout.listTop - 4) / 22);
    }

    private int maxPage(int pageSize) {
        return Math.max(0, (this.revisions.size() - 1) / pageSize);
    }

    private Layout layout() {
        EditorUiScale.Frame frame = EditorUiScale.fit(this.width, this.height, 720, 390);
        int panelWidth = frame.width();
        int panelHeight = frame.height();
        int left = frame.left();
        int top = frame.top();
        boolean wide = panelWidth >= 560;
        int timelineWidth = wide
                ? Math.min(TIMELINE_WIDTH, Math.max(150, panelWidth / 3))
                : panelWidth - 4;
        int detailsX = wide ? left + timelineWidth + 10 : left + 10;
        int detailsWidth = wide ? panelWidth - timelineWidth - 18 : panelWidth - 20;
        int listTop = top + 58;
        int detailsBottom = top + panelHeight - (wide ? 38 : 72);
        return new Layout(left, top, panelWidth, panelHeight, wide, timelineWidth,
                detailsX, detailsWidth, listTop, detailsBottom);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(this.previous);
    }

    private record Layout(int left, int top, int width, int height, boolean wide, int timelineWidth,
                          int detailsX, int detailsWidth, int listTop, int detailsBottom) {
    }
}
