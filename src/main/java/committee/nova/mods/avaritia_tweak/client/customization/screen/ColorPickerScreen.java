package committee.nova.mods.avaritia_tweak.client.customization.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;
import java.util.function.IntConsumer;

public final class ColorPickerScreen extends Screen {
    private final Screen previous;
    private final IntConsumer onSave;
    private final int originalColor;
    private float hue;
    private float saturation;
    private float value;
    private EditBox hexBox;
    private String hexText;
    private String error = "";
    private boolean syncingHex;
    private DragTarget dragTarget = DragTarget.NONE;

    public ColorPickerScreen(Screen previous, Component title, int initialColor, IntConsumer onSave) {
        super(title);
        this.previous = previous;
        this.onSave = onSave;
        this.originalColor = initialColor & 0x00ffffff;
        ColorPickerModel.Hsv initial = ColorPickerModel.fromRgb(this.originalColor);
        this.hue = initial.hue();
        this.saturation = initial.saturation();
        this.value = initial.value();
        this.hexText = formatColor(this.originalColor);
    }

    @Override
    protected void init() {
        Layout layout = layout();
        this.hexBox = new EditBox(this.font, layout.detailsX, layout.top + 112,
                layout.detailsWidth, 20, Component.translatable("gui.avaritia_tweak.color_picker.hex"));
        this.hexBox.setMaxLength(7);
        this.hexBox.setValue(this.hexText);
        this.hexBox.setResponder(this::updateFromHex);
        this.addRenderableWidget(this.hexBox);

        int actionY = layout.top + layout.height - 30;
        int cancelWidth = Math.min(80, Math.max(52, layout.detailsWidth / 2 - 3));
        this.addRenderableWidget(EditorButton.builder(Component.translatable("gui.avaritia_tweak.cancel"),
                        button -> onClose())
                .bounds(layout.detailsX, actionY, cancelWidth, 20)
                .style(EditorButton.Style.QUIET).build());
        this.addRenderableWidget(EditorButton.builder(Component.translatable("gui.avaritia_tweak.save"),
                        button -> save())
                .bounds(layout.detailsX + cancelWidth + 6, actionY,
                        Math.max(1, layout.detailsWidth - cancelWidth - 6), 20)
                .style(EditorButton.Style.PRIMARY).build());
    }

    private void updateFromHex(String raw) {
        this.hexText = raw;
        if (this.syncingHex) {
            return;
        }
        Integer parsed = parseColor(raw);
        if (parsed == null) {
            return;
        }
        ColorPickerModel.Hsv decoded = ColorPickerModel.fromRgb(parsed);
        this.hue = decoded.hue();
        this.saturation = decoded.saturation();
        this.value = decoded.value();
        this.error = "";
    }

    private void save() {
        Integer parsed = parseColor(this.hexText);
        if (parsed == null) {
            this.error = Component.translatable("gui.avaritia_tweak.color_picker.invalid").getString();
            return;
        }
        this.onSave.accept(parsed);
        Minecraft.getInstance().setScreen(this.previous);
    }

    private void syncHex() {
        this.hexText = formatColor(color());
        if (this.hexBox != null) {
            this.syncingHex = true;
            this.hexBox.setValue(this.hexText);
            this.syncingHex = false;
        }
        this.error = "";
    }

    private int color() {
        return ColorPickerModel.toRgb(this.hue, this.saturation, this.value);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            Layout layout = layout();
            if (layout.insideSaturationValue(mouseX, mouseY)) {
                this.dragTarget = DragTarget.SATURATION_VALUE;
                updateSaturationValue(layout, mouseX, mouseY);
                return true;
            }
            if (layout.insideHue(mouseX, mouseY)) {
                this.dragTarget = DragTarget.HUE;
                updateHue(layout, mouseY);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button,
                                double dragX, double dragY) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            Layout layout = layout();
            if (this.dragTarget == DragTarget.SATURATION_VALUE) {
                updateSaturationValue(layout, mouseX, mouseY);
                return true;
            }
            if (this.dragTarget == DragTarget.HUE) {
                updateHue(layout, mouseY);
                return true;
            }
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && this.dragTarget != DragTarget.NONE) {
            this.dragTarget = DragTarget.NONE;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void updateSaturationValue(Layout layout, double mouseX, double mouseY) {
        this.saturation = ColorPickerModel.clamp((float) ((mouseX - layout.svX)
                / Math.max(1, layout.svWidth - 1)));
        this.value = ColorPickerModel.clamp(1.0f - (float) ((mouseY - layout.svY)
                / Math.max(1, layout.svHeight - 1)));
        syncHex();
    }

    private void updateHue(Layout layout, double mouseY) {
        this.hue = ColorPickerModel.clamp((float) ((mouseY - layout.svY)
                / Math.max(1, layout.svHeight - 1)));
        if (this.hue >= 1.0f) {
            this.hue = Math.nextDown(1.0f);
        }
        syncHex();
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Layout layout = layout();
        EditorTheme.renderBackdrop(graphics, this.width, this.height);
        EditorTheme.renderWindow(graphics, layout.left, layout.top, layout.width, layout.height,
                EditorTheme.AVARITIA_CYAN);
        graphics.drawString(this.font, this.title, layout.left + 10, layout.top + 12,
                EditorTheme.AVARITIA_GOLD, false);
        graphics.drawString(this.font, Component.translatable("gui.avaritia_tweak.color_picker.subtitle"),
                layout.left + 10, layout.top + 27, EditorTheme.TEXT_MUTED, false);
        renderSaturationValue(graphics, layout);
        renderHue(graphics, layout);
        renderDetails(graphics, layout);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderSaturationValue(GuiGraphics graphics, Layout layout) {
        graphics.fill(layout.svX - 1, layout.svY - 1,
                layout.svX + layout.svWidth + 1, layout.svY + layout.svHeight + 1,
                EditorTheme.BORDER);
        int step = layout.svWidth > 220 ? 2 : 1;
        for (int y = 0; y < layout.svHeight; y += step) {
            float sampleValue = 1.0f - (float) y / Math.max(1, layout.svHeight - 1);
            int bottom = Math.min(layout.svHeight, y + step);
            for (int x = 0; x < layout.svWidth; x += step) {
                float sampleSaturation = (float) x / Math.max(1, layout.svWidth - 1);
                int right = Math.min(layout.svWidth, x + step);
                graphics.fill(layout.svX + x, layout.svY + y,
                        layout.svX + right, layout.svY + bottom,
                        0xff000000 | ColorPickerModel.toRgb(this.hue, sampleSaturation, sampleValue));
            }
        }
        int cursorX = layout.svX + Math.round(this.saturation * (layout.svWidth - 1));
        int cursorY = layout.svY + Math.round((1.0f - this.value) * (layout.svHeight - 1));
        renderCursor(graphics, cursorX, cursorY);
    }

    private void renderHue(GuiGraphics graphics, Layout layout) {
        graphics.fill(layout.hueX - 1, layout.svY - 1,
                layout.hueX + layout.hueWidth + 1, layout.svY + layout.svHeight + 1,
                EditorTheme.BORDER);
        for (int y = 0; y < layout.svHeight; y++) {
            float sampleHue = (float) y / Math.max(1, layout.svHeight - 1);
            graphics.fill(layout.hueX, layout.svY + y,
                    layout.hueX + layout.hueWidth, layout.svY + y + 1,
                    0xff000000 | ColorPickerModel.toRgb(sampleHue, 1.0f, 1.0f));
        }
        int cursorY = layout.svY + Math.round(this.hue * (layout.svHeight - 1));
        graphics.fill(layout.hueX - 3, cursorY - 1,
                layout.hueX + layout.hueWidth + 3, cursorY + 2, 0xff101018);
        graphics.fill(layout.hueX - 2, cursorY,
                layout.hueX + layout.hueWidth + 2, cursorY + 1, 0xffffffff);
    }

    private void renderDetails(GuiGraphics graphics, Layout layout) {
        graphics.drawString(this.font, Component.translatable("gui.avaritia_tweak.color_picker.preview"),
                layout.detailsX, layout.top + 54, EditorTheme.TEXT_MUTED, false);
        int swatchWidth = Math.max(20, (layout.detailsWidth - 6) / 2);
        renderSwatch(graphics, layout.detailsX, layout.top + 66, swatchWidth, 30,
                this.originalColor, Component.translatable("gui.avaritia_tweak.color_picker.original"));
        renderSwatch(graphics, layout.detailsX + swatchWidth + 6, layout.top + 66,
                Math.max(1, layout.detailsWidth - swatchWidth - 6), 30,
                color(), Component.translatable("gui.avaritia_tweak.color_picker.current"));
        graphics.drawString(this.font, Component.translatable("gui.avaritia_tweak.color_picker.hex"),
                layout.detailsX, layout.top + 101, EditorTheme.TEXT_MUTED, false);
        graphics.drawString(this.font, Component.literal(String.format(Locale.ROOT,
                        "H %3d°  S %3d%%  V %3d%%", Math.round(this.hue * 360.0f),
                        Math.round(this.saturation * 100.0f), Math.round(this.value * 100.0f))),
                layout.detailsX, layout.top + 141, EditorTheme.TEXT_FAINT, false);
        if (!this.error.isEmpty()) {
            graphics.drawString(this.font, ScreenText.fit(this.font, this.error, layout.detailsWidth),
                    layout.detailsX, layout.top + 158, EditorTheme.ERROR, false);
        }
    }

    private void renderSwatch(GuiGraphics graphics, int x, int y, int width, int height,
                              int rgb, Component label) {
        graphics.fill(x, y, x + width, y + height, EditorTheme.BORDER_DARK);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xff000000 | rgb);
        int luminance = (((rgb >> 16) & 0xff) * 299 + ((rgb >> 8) & 0xff) * 587
                + (rgb & 0xff) * 114) / 1000;
        graphics.drawCenteredString(this.font, ScreenText.fit(this.font, label.getString(), width - 4),
                x + width / 2, y + (height - 8) / 2, luminance > 150 ? 0xff202028 : 0xfff4f4f7);
    }

    private static void renderCursor(GuiGraphics graphics, int x, int y) {
        renderCursorBorder(graphics, x, y, 4, 0xff101018);
        renderCursorBorder(graphics, x, y, 3, 0xffffffff);
    }

    private static void renderCursorBorder(GuiGraphics graphics, int x, int y, int radius, int color) {
        graphics.fill(x - radius, y - radius, x + radius + 1, y - radius + 1, color);
        graphics.fill(x - radius, y + radius, x + radius + 1, y + radius + 1, color);
        graphics.fill(x - radius, y - radius + 1, x - radius + 1, y + radius, color);
        graphics.fill(x + radius, y - radius + 1, x + radius + 1, y + radius, color);
    }

    private Layout layout() {
        EditorUiScale.Frame frame = EditorUiScale.fit(this.width, this.height, 520, 310);
        int left = frame.left();
        int top = frame.top();
        int contentWidth = frame.width() - 36;
        int detailsWidth = Math.max(104, Math.min(150, contentWidth / 3));
        int hueWidth = 18;
        int gap = 12;
        int svWidth = Math.max(96, contentWidth - detailsWidth - hueWidth - gap * 2);
        int svHeight = Math.max(96, frame.height() - 116);
        int svX = left + 12;
        int svY = top + 50;
        int hueX = svX + svWidth + gap;
        int detailsX = hueX + hueWidth + gap;
        return new Layout(left, top, frame.width(), frame.height(), svX, svY,
                svWidth, svHeight, hueX, hueWidth, detailsX,
                Math.max(1, left + frame.width() - 12 - detailsX));
    }

    private static Integer parseColor(String raw) {
        String normalized = raw.strip();
        if (normalized.startsWith("#")) {
            normalized = normalized.substring(1);
        }
        if (normalized.length() != 6) {
            return null;
        }
        try {
            return Integer.parseInt(normalized, 16);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static String formatColor(int color) {
        return String.format(Locale.ROOT, "#%06X", color & 0x00ffffff);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(this.previous);
    }

    private enum DragTarget {
        NONE,
        SATURATION_VALUE,
        HUE
    }

    private record Layout(int left, int top, int width, int height,
                          int svX, int svY, int svWidth, int svHeight,
                          int hueX, int hueWidth, int detailsX, int detailsWidth) {
        boolean insideSaturationValue(double x, double y) {
            return x >= this.svX && x < this.svX + this.svWidth
                    && y >= this.svY && y < this.svY + this.svHeight;
        }

        boolean insideHue(double x, double y) {
            return x >= this.hueX && x < this.hueX + this.hueWidth
                    && y >= this.svY && y < this.svY + this.svHeight;
        }
    }
}
