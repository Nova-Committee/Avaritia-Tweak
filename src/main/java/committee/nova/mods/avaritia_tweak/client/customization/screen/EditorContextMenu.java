package committee.nova.mods.avaritia_tweak.client.customization.screen;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Objects;

final class EditorContextMenu {
    private static final int ROW_HEIGHT = 18;
    private static final int PADDING = 3;
    private static final int MARGIN = 4;
    private static final int OFFSET = 2;
    private static final int MIN_WIDTH = 112;

    private final Font font;
    private final List<Item> items;
    private final EditorContextMenuPlacement placement;
    private int scrollOffset;
    private int selectedIndex;
    private boolean open = true;

    private EditorContextMenu(Font font, List<Item> items,
                              EditorContextMenuPlacement placement) {
        this.font = font;
        this.items = items;
        this.placement = placement;
        this.selectedIndex = firstEnabledItem();
    }

    static EditorContextMenu open(Font font, int screenWidth, int screenHeight,
                                  double mouseX, double mouseY, List<Item> items) {
        Objects.requireNonNull(font, "font");
        List<Item> copiedItems = List.copyOf(Objects.requireNonNull(items, "items"));
        if (copiedItems.isEmpty()) {
            throw new IllegalArgumentException("A context menu needs at least one item");
        }
        int desiredWidth = Math.max(MIN_WIDTH, copiedItems.stream()
                .mapToInt(item -> font.width(item.label()) + 20)
                .max().orElse(MIN_WIDTH));
        EditorContextMenuPlacement placement = EditorContextMenuPlacement.calculate(
                screenWidth, screenHeight, (int) mouseX, (int) mouseY,
                desiredWidth, copiedItems.size(), ROW_HEIGHT, PADDING, MARGIN, OFFSET);
        return new EditorContextMenu(font, copiedItems, placement);
    }

    void render(GuiGraphics graphics, int mouseX, int mouseY) {
        if (!this.open) {
            return;
        }
        int hoveredIndex = this.placement.itemAt(mouseX, mouseY, ROW_HEIGHT, PADDING,
                this.scrollOffset, this.items.size());
        int highlightedIndex = hoveredIndex >= 0 ? hoveredIndex : this.selectedIndex;
        int x = this.placement.x();
        int y = this.placement.y();
        int width = this.placement.width();
        int height = this.placement.height();

        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 600.0F);
        graphics.fill(x + 3, y + 4, x + width + 3, y + height + 4,
                EditorTheme.withAlpha(EditorTheme.BORDER_DARK, 0xaa));
        graphics.fill(x, y, x + width, y + height, EditorTheme.BORDER_DARK);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, EditorTheme.PANEL_RAISED);
        graphics.fill(x + 1, y + 1, x + width - 1, y + 2, EditorTheme.AVARITIA_CYAN);

        int textWidth = width - (this.items.size() > this.placement.visibleRows() ? 20 : 14);
        for (int row = 0; row < this.placement.visibleRows(); row++) {
            int index = this.scrollOffset + row;
            if (index >= this.items.size()) {
                break;
            }
            Item item = this.items.get(index);
            int rowY = y + PADDING + row * ROW_HEIGHT;
            if (item.separated()) {
                graphics.fill(x + 5, rowY, x + width - 5, rowY + 1, EditorTheme.dividerSoft());
            }
            if (index == highlightedIndex) {
                int surface = item.tone() == Tone.DANGER
                        ? EditorTheme.mix(EditorTheme.PANEL_DARK, EditorTheme.ERROR, 24)
                        : EditorTheme.selectionSurface();
                graphics.fill(x + 2, rowY + 1, x + width - 2, rowY + ROW_HEIGHT, surface);
                graphics.fill(x + 2, rowY + 1, x + 4, rowY + ROW_HEIGHT,
                        item.tone() == Tone.DANGER ? EditorTheme.ERROR : EditorTheme.AVARITIA_CYAN);
            }
            int color = !item.enabled() ? EditorTheme.TEXT_FAINT
                    : item.tone() == Tone.DANGER ? EditorTheme.ERROR : EditorTheme.TEXT;
            String label = ScreenText.fit(this.font, item.label().getString(), Math.max(0, textWidth));
            graphics.drawString(this.font, label, x + 8, rowY + 5, color, false);
        }
        renderScrollBar(graphics);
        graphics.pose().popPose();
    }

    boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.open || !this.placement.contains(mouseX, mouseY)) {
            return false;
        }
        int index = this.placement.itemAt(mouseX, mouseY, ROW_HEIGHT, PADDING,
                this.scrollOffset, this.items.size());
        if (index >= 0) {
            this.selectedIndex = index;
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                activate(index);
            }
        }
        return true;
    }

    boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (!this.open || !this.placement.contains(mouseX, mouseY)) {
            return false;
        }
        if (this.items.size() <= this.placement.visibleRows() || delta == 0.0D) {
            return true;
        }
        int direction = delta < 0.0D ? 1 : -1;
        this.scrollOffset = Math.max(0, Math.min(maxScroll(), this.scrollOffset + direction));
        ensureSelectedVisible();
        return true;
    }

    boolean keyPressed(int keyCode) {
        if (!this.open) {
            return false;
        }
        return switch (keyCode) {
            case GLFW.GLFW_KEY_ESCAPE -> {
                close();
                yield true;
            }
            case GLFW.GLFW_KEY_UP -> {
                moveSelection(-1);
                yield true;
            }
            case GLFW.GLFW_KEY_DOWN -> {
                moveSelection(1);
                yield true;
            }
            case GLFW.GLFW_KEY_HOME -> {
                this.selectedIndex = firstEnabledItem();
                ensureSelectedVisible();
                yield true;
            }
            case GLFW.GLFW_KEY_END -> {
                this.selectedIndex = lastEnabledItem();
                ensureSelectedVisible();
                yield true;
            }
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                activate(this.selectedIndex);
                yield true;
            }
            default -> false;
        };
    }

    boolean isOpen() {
        return this.open;
    }

    void close() {
        this.open = false;
    }

    private void activate(int index) {
        if (index < 0 || index >= this.items.size()) {
            return;
        }
        Item item = this.items.get(index);
        if (!item.enabled()) {
            return;
        }
        this.open = false;
        item.action().run();
    }

    private void moveSelection(int direction) {
        if (this.selectedIndex < 0) {
            this.selectedIndex = direction > 0 ? firstEnabledItem() : lastEnabledItem();
            ensureSelectedVisible();
            return;
        }
        for (int step = 1; step <= this.items.size(); step++) {
            int candidate = Math.floorMod(this.selectedIndex + direction * step, this.items.size());
            if (this.items.get(candidate).enabled()) {
                this.selectedIndex = candidate;
                ensureSelectedVisible();
                return;
            }
        }
    }

    private int firstEnabledItem() {
        for (int index = 0; index < this.items.size(); index++) {
            if (this.items.get(index).enabled()) {
                return index;
            }
        }
        return -1;
    }

    private int lastEnabledItem() {
        for (int index = this.items.size() - 1; index >= 0; index--) {
            if (this.items.get(index).enabled()) {
                return index;
            }
        }
        return -1;
    }

    private void ensureSelectedVisible() {
        if (this.selectedIndex < 0) {
            return;
        }
        if (this.selectedIndex < this.scrollOffset) {
            this.scrollOffset = this.selectedIndex;
        } else if (this.selectedIndex >= this.scrollOffset + this.placement.visibleRows()) {
            this.scrollOffset = this.selectedIndex - this.placement.visibleRows() + 1;
        }
        this.scrollOffset = Math.max(0, Math.min(this.scrollOffset, maxScroll()));
    }

    private int maxScroll() {
        return Math.max(0, this.items.size() - this.placement.visibleRows());
    }

    private void renderScrollBar(GuiGraphics graphics) {
        if (this.items.size() <= this.placement.visibleRows()) {
            return;
        }
        int x = this.placement.x() + this.placement.width() - 5;
        int top = this.placement.y() + PADDING;
        int trackHeight = this.placement.visibleRows() * ROW_HEIGHT;
        graphics.fill(x, top, x + 2, top + trackHeight, EditorTheme.BORDER_DARK);
        int thumbHeight = Math.max(6, trackHeight * this.placement.visibleRows() / this.items.size());
        int thumbTravel = trackHeight - thumbHeight;
        int thumbY = top + (maxScroll() == 0 ? 0 : thumbTravel * this.scrollOffset / maxScroll());
        graphics.fill(x, thumbY, x + 2, thumbY + thumbHeight, EditorTheme.AVARITIA_CYAN);
    }

    static Item action(Component label, Runnable action) {
        return action(label, action, true);
    }

    static Item action(Component label, Runnable action, boolean enabled) {
        return new Item(label, action, Tone.NORMAL, enabled, false);
    }

    static Item separatedAction(Component label, Runnable action) {
        return separatedAction(label, action, true);
    }

    static Item separatedAction(Component label, Runnable action, boolean enabled) {
        return new Item(label, action, Tone.NORMAL, enabled, true);
    }

    static Item danger(Component label, Runnable action) {
        return new Item(label, action, Tone.DANGER, true, true);
    }

    record Item(Component label, Runnable action, Tone tone, boolean enabled, boolean separated) {
        Item {
            Objects.requireNonNull(label, "label");
            Objects.requireNonNull(action, "action");
            Objects.requireNonNull(tone, "tone");
        }
    }

    enum Tone {
        NORMAL,
        DANGER
    }
}
