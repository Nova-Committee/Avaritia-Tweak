package committee.nova.mods.avaritia_tweak.client.customization.screen;

import committee.nova.mods.avaritia_tweak.customization.diff.ChangeType;
import committee.nova.mods.avaritia_tweak.customization.diff.EntryChange;
import committee.nova.mods.avaritia_tweak.customization.diff.FieldChange;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/** Read-only, responsive comparison of one committed entry and its local draft value. */
public final class EntryCompareScreen extends Screen {
    private final Screen previous;
    private final EntryChange change;
    private int scrollOffset;

    public EntryCompareScreen(Screen previous, EntryChange change) {
        super(Component.translatable("gui.avaritia_tweak.entry_compare.title"));
        this.previous = Objects.requireNonNull(previous, "previous");
        this.change = Objects.requireNonNull(change, "change");
    }

    @Override
    protected void init() {
        Layout layout = layout();
        this.scrollOffset = Math.min(this.scrollOffset, maxScroll(layout));
        int actionsY = layout.top + layout.height - 30;
        this.addRenderableWidget(EditorButton.builder(Component.literal("↑"),
                        button -> scrollBy(-1))
                .bounds(layout.left + 8, actionsY, 28, 20)
                .style(EditorButton.Style.QUIET).build());
        this.addRenderableWidget(EditorButton.builder(Component.literal("↓"),
                        button -> scrollBy(1))
                .bounds(layout.left + 42, actionsY, 28, 20)
                .style(EditorButton.Style.QUIET).build());
        this.addRenderableWidget(EditorButton.builder(
                        Component.translatable("gui.avaritia_tweak.close"), button -> onClose())
                .bounds(layout.left + layout.width - 84, actionsY, 76, 20)
                .style(EditorButton.Style.PRIMARY).build());
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        Layout layout = layout();
        if (!layout.containsRows(mouseX, mouseY) || delta == 0.0D) {
            return super.mouseScrolled(mouseX, mouseY, delta);
        }
        scrollBy(delta < 0.0D ? 1 : -1);
        return true;
    }

    private void scrollBy(int amount) {
        Layout layout = layout();
        this.scrollOffset = Math.max(0, Math.min(maxScroll(layout), this.scrollOffset + amount));
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Layout layout = layout();
        EditorTheme.renderBackdrop(graphics, this.width, this.height);
        int accent = EditorTheme.changeColor(this.change.type());
        EditorTheme.renderWindow(graphics, layout.left, layout.top, layout.width, layout.height, accent);
        graphics.drawString(this.font, this.title, layout.left + 10, layout.top + 12,
                EditorTheme.AVARITIA_GOLD, false);
        String id = ScreenText.fit(this.font, this.change.key().id().toString(), layout.width - 130);
        graphics.drawString(this.font, id, layout.left + 10, layout.top + 27,
                EditorTheme.TEXT_MUTED, false);
        String type = Component.translatable(typeKey(this.change.type())).getString();
        EditorTheme.renderBadge(graphics, this.font,
                layout.left + layout.width - this.font.width(type) - 20,
                layout.top + 10, type, accent);

        EditorTheme.renderSectionHeader(graphics, this.font, layout.left + 2, layout.top + 42,
                layout.width - 4, Component.translatable("gui.avaritia_tweak.entry_compare.fields"),
                Integer.toString(this.change.fields().size()), accent);
        renderColumnHeaders(graphics, layout);
        renderRows(graphics, layout, mouseX, mouseY);

        int first = this.change.fields().isEmpty() ? 0 : this.scrollOffset + 1;
        int last = Math.min(this.change.fields().size(), this.scrollOffset + pageSize(layout));
        graphics.drawString(this.font, first + "-" + last + "/" + this.change.fields().size(),
                layout.left + 82, layout.top + layout.height - 24, EditorTheme.TEXT_MUTED, false);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderColumnHeaders(GuiGraphics graphics, Layout layout) {
        int y = layout.rowsTop - 13;
        if (!layout.wide) {
            graphics.drawString(this.font,
                    Component.translatable("gui.avaritia_tweak.entry_compare.change"),
                    layout.left + 10, y, EditorTheme.TEXT_FAINT, false);
            return;
        }
        int pathWidth = pathWidth(layout);
        int valueWidth = (layout.width - 28 - pathWidth) / 2;
        graphics.drawString(this.font,
                Component.translatable("gui.avaritia_tweak.entry_compare.field"),
                layout.left + 10, y, EditorTheme.TEXT_FAINT, false);
        graphics.drawString(this.font,
                Component.translatable("gui.avaritia_tweak.entry_compare.committed"),
                layout.left + 14 + pathWidth, y, EditorTheme.TEXT_FAINT, false);
        graphics.drawString(this.font,
                Component.translatable("gui.avaritia_tweak.entry_compare.draft"),
                layout.left + 18 + pathWidth + valueWidth, y, EditorTheme.TEXT_FAINT, false);
    }

    private void renderRows(GuiGraphics graphics, Layout layout, int mouseX, int mouseY) {
        int pageSize = pageSize(layout);
        int limit = Math.min(this.change.fields().size(), this.scrollOffset + pageSize);
        for (int index = this.scrollOffset; index < limit; index++) {
            int row = index - this.scrollOffset;
            int y = layout.rowsTop + row * layout.rowHeight;
            FieldChange field = this.change.fields().get(index);
            boolean hovered = mouseX >= layout.left + 7 && mouseX < layout.left + layout.width - 7
                    && mouseY >= y && mouseY < y + layout.rowHeight - 2;
            int surface = hovered ? EditorTheme.selectionSurface()
                    : (row & 1) == 0 ? EditorTheme.PANEL : EditorTheme.PANEL_DARK;
            graphics.fill(layout.left + 7, y, layout.left + layout.width - 7,
                    y + layout.rowHeight - 2, surface);
            graphics.fill(layout.left + 7, y, layout.left + 9,
                    y + layout.rowHeight - 2, EditorTheme.changeColor(this.change.type()));
            renderField(graphics, layout, field, y);
        }
    }

    private void renderField(GuiGraphics graphics, Layout layout, FieldChange field, int y) {
        String before = field.before().orElse("∅");
        String after = field.after().orElse("∅");
        if (!layout.wide) {
            graphics.drawString(this.font,
                    ScreenText.fit(this.font, field.fieldPath(), layout.width - 24),
                    layout.left + 13, y + 5, EditorTheme.AVARITIA_CYAN, false);
            String values = before + "  →  " + after;
            graphics.drawString(this.font, ScreenText.fit(this.font, values, layout.width - 24),
                    layout.left + 13, y + 20, EditorTheme.TEXT, false);
            return;
        }
        int pathWidth = pathWidth(layout);
        int valueWidth = (layout.width - 28 - pathWidth) / 2;
        graphics.drawString(this.font, ScreenText.fit(this.font, field.fieldPath(), pathWidth - 6),
                layout.left + 13, y + 9, EditorTheme.AVARITIA_CYAN, false);
        graphics.drawString(this.font, ScreenText.fit(this.font, before, valueWidth - 8),
                layout.left + 14 + pathWidth, y + 9, EditorTheme.TEXT_MUTED, false);
        graphics.drawString(this.font, ScreenText.fit(this.font, after, valueWidth - 8),
                layout.left + 18 + pathWidth + valueWidth, y + 9, EditorTheme.TEXT, false);
    }

    private int pageSize(Layout layout) {
        return Math.max(1, (layout.rowsBottom - layout.rowsTop) / layout.rowHeight);
    }

    private int maxScroll(Layout layout) {
        return Math.max(0, this.change.fields().size() - pageSize(layout));
    }

    private static int pathWidth(Layout layout) {
        return Math.max(120, layout.width * 28 / 100);
    }

    private Layout layout() {
        EditorUiScale.Frame frame = EditorUiScale.fit(this.width, this.height, 720, 390);
        boolean wide = frame.width() >= 520;
        return new Layout(frame.left(), frame.top(), frame.width(), frame.height(), wide,
                frame.top() + 78, frame.top() + frame.height() - 38, wide ? 28 : 40);
    }

    private static String typeKey(ChangeType type) {
        return switch (type) {
            case ADDED -> "gui.avaritia_tweak.entry_compare.added";
            case MODIFIED -> "gui.avaritia_tweak.entry_compare.modified";
            case REMOVED -> "gui.avaritia_tweak.entry_compare.removed";
        };
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(this.previous);
    }

    private record Layout(int left, int top, int width, int height, boolean wide,
                          int rowsTop, int rowsBottom, int rowHeight) {
        boolean containsRows(double x, double y) {
            return x >= this.left + 7 && x < this.left + this.width - 7
                    && y >= this.rowsTop && y < this.rowsBottom;
        }
    }
}
