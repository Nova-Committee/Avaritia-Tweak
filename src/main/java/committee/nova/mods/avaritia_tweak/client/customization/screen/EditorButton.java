package committee.nova.mods.avaritia_tweak.client.customization.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

final class EditorButton extends AbstractButton {
    private final OnPress onPress;
    private final SecondaryPressHandler<EditorButton> onSecondaryPress;
    private final Style style;
    private final boolean selected;
    private final Component secondary;
    private final Integer swatchColor;

    private EditorButton(int x, int y, int width, int height, Component message,
                         OnPress onPress, SecondaryPressHandler<EditorButton> onSecondaryPress,
                         Style style, boolean selected, Component secondary, Integer swatchColor) {
        super(x, y, width, height, message);
        this.onPress = onPress;
        this.onSecondaryPress = onSecondaryPress;
        this.style = style;
        this.selected = selected;
        this.secondary = secondary;
        this.swatchColor = swatchColor;
    }

    static Builder builder(Component message, OnPress onPress) {
        return new Builder(message, onPress);
    }

    @Override
    public void onPress() {
        this.onPress.onPress(this);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT && this.active && this.visible
                && this.onSecondaryPress != null && this.isMouseOver(mouseX, mouseY)) {
            this.onSecondaryPress.onPress(this, mouseX, mouseY);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        boolean highlighted = this.active && (this.isHoveredOrFocused() || this.selected);
        Palette palette = palette(highlighted);
        int x = this.getX();
        int y = this.getY();
        graphics.fill(x + 1, y + 2, x + this.width + 1, y + this.height + 2,
                EditorTheme.withAlpha(EditorTheme.BORDER_DARK, 0x88));
        graphics.fill(x, y, x + this.width, y + this.height, palette.border);
        graphics.fill(x + 1, y + 1, x + this.width - 1, y + this.height - 1, palette.background);
        graphics.fill(x + 1, y + 1, x + this.width - 1, y + 2, palette.highlight);
        if (this.selected || this.style == Style.PRIMARY) {
            graphics.fill(x + 1, y + this.height - 2, x + this.width - 1, y + this.height - 1,
                    palette.accent);
        }
        if (this.style == Style.LIST && this.selected) {
            graphics.fill(x, y, x + 3, y + this.height, palette.accent);
        }
        Font font = Minecraft.getInstance().font;
        int labelWidth = this.style == Style.LIST ? this.width - 18 : this.width - 8;
        String label = ScreenText.fit(font, this.getMessage().getString(), Math.max(0, labelWidth));
        if (this.style == Style.LIST && this.secondary != null) {
            String secondaryLabel = ScreenText.fit(font, this.secondary.getString(), Math.max(0, labelWidth));
            graphics.drawString(font, label, x + 8, y + 3, palette.text, false);
            graphics.drawString(font, secondaryLabel, x + 8, y + this.height - 10,
                    this.active ? EditorTheme.TEXT_MUTED : EditorTheme.TEXT_FAINT, false);
        } else if (this.style == Style.LIST) {
            graphics.drawString(font, label, x + 8,
                    y + (this.height - 8) / 2, palette.text, false);
        } else if (this.swatchColor != null) {
            int swatchSize = Math.max(6, this.height - 10);
            int swatchX = x + 5;
            int swatchY = y + (this.height - swatchSize) / 2;
            graphics.fill(swatchX - 1, swatchY - 1, swatchX + swatchSize + 1,
                    swatchY + swatchSize + 1, EditorTheme.BORDER_DARK);
            graphics.fill(swatchX, swatchY, swatchX + swatchSize, swatchY + swatchSize,
                    0xff000000 | this.swatchColor);
            int textX = swatchX + swatchSize + 6;
            String swatchLabel = ScreenText.fit(font, this.getMessage().getString(),
                    Math.max(0, x + this.width - textX - 4));
            graphics.drawString(font, swatchLabel, textX,
                    y + (this.height - 8) / 2, palette.text, false);
        } else {
            graphics.drawCenteredString(font, label, x + this.width / 2,
                    y + (this.height - 8) / 2, palette.text);
        }
    }

    private Palette palette(boolean highlighted) {
        if (!this.active) {
            int background = EditorTheme.mix(EditorTheme.PANEL_DARK, EditorTheme.PANEL, 36);
            return new Palette(EditorTheme.mix(EditorTheme.BORDER_DARK, EditorTheme.BORDER, 45),
                    background, EditorTheme.mix(background, EditorTheme.TEXT_MUTED, 12),
                    EditorTheme.BORDER, EditorTheme.TEXT_FAINT);
        }
        return switch (this.style) {
            case PRIMARY -> accented(EditorTheme.AVARITIA_CYAN, highlighted, false);
            case DANGER -> accented(EditorTheme.ERROR, highlighted, false);
            case QUIET -> accented(EditorTheme.AVARITIA_GOLD, highlighted, true);
            case TAB -> accented(EditorTheme.AVARITIA_RED, highlighted, !highlighted);
            case LIST -> accented(EditorTheme.AVARITIA_CYAN, highlighted, false);
            case DEFAULT -> accented(EditorTheme.AVARITIA_GOLD, highlighted, false);
        };
    }

    private static Palette accented(int accent, boolean highlighted, boolean mutedText) {
        int border = highlighted ? accent : EditorTheme.mix(EditorTheme.BORDER, accent, 18);
        int background = EditorTheme.mix(EditorTheme.PANEL_DARK, accent, highlighted ? 29 : 15);
        int highlight = EditorTheme.mix(background, EditorTheme.TEXT, highlighted ? 22 : 11);
        return new Palette(border, background, highlight, accent,
                mutedText ? EditorTheme.TEXT_MUTED : EditorTheme.TEXT);
    }

    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, this.secondary == null
                ? this.getMessage()
                : Component.literal(this.getMessage().getString() + ", " + this.secondary.getString()));
    }

    enum Style {
        DEFAULT,
        PRIMARY,
        DANGER,
        QUIET,
        TAB,
        LIST
    }

    @FunctionalInterface
    interface OnPress {
        void onPress(EditorButton button);
    }

    static final class Builder {
        private final Component message;
        private final OnPress onPress;
        private int x;
        private int y;
        private int width = 150;
        private int height = 20;
        private Style style = Style.DEFAULT;
        private boolean selected;
        private Component secondary;
        private Integer swatchColor;
        private SecondaryPressHandler<EditorButton> onSecondaryPress;

        private Builder(Component message, OnPress onPress) {
            this.message = message;
            this.onPress = onPress;
        }

        Builder bounds(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            return this;
        }

        Builder style(Style style) {
            this.style = style;
            return this;
        }

        Builder selected(boolean selected) {
            this.selected = selected;
            return this;
        }

        Builder secondary(Component secondary) {
            this.secondary = secondary;
            return this;
        }

        Builder swatch(int color) {
            this.swatchColor = color & 0x00ffffff;
            return this;
        }

        Builder onSecondaryPress(SecondaryPressHandler<EditorButton> onSecondaryPress) {
            this.onSecondaryPress = onSecondaryPress;
            return this;
        }

        EditorButton build() {
            return new EditorButton(this.x, this.y, this.width, this.height,
                    this.message, this.onPress, this.onSecondaryPress, this.style, this.selected,
                    this.secondary, this.swatchColor);
        }
    }

    private record Palette(int border, int background, int highlight, int accent, int text) {
    }
}
