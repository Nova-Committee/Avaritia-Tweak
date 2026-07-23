package committee.nova.mods.avaritia_tweak.client.customization.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

final class EditorButton extends AbstractButton {
    private final OnPress onPress;
    private final Style style;
    private final boolean selected;

    private EditorButton(int x, int y, int width, int height, Component message,
                         OnPress onPress, Style style, boolean selected) {
        super(x, y, width, height, message);
        this.onPress = onPress;
        this.style = style;
        this.selected = selected;
    }

    static Builder builder(Component message, OnPress onPress) {
        return new Builder(message, onPress);
    }

    @Override
    public void onPress() {
        this.onPress.onPress(this);
    }

    @Override
    protected void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        boolean highlighted = this.active && (this.isHoveredOrFocused() || this.selected);
        Palette palette = palette(highlighted);
        int x = this.getX();
        int y = this.getY();
        graphics.fill(x + 1, y + 2, x + this.width + 1, y + this.height + 2, 0x88000000);
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
        if (this.style == Style.LIST) {
            graphics.drawString(font, label, x + 8,
                    y + (this.height - 8) / 2, palette.text, false);
        } else {
            graphics.drawCenteredString(font, label, x + this.width / 2,
                    y + (this.height - 8) / 2, palette.text);
        }
    }

    private Palette palette(boolean highlighted) {
        if (!this.active) {
            return new Palette(0xff34373e, 0xff23252a, 0xff2b2e34,
                    0xff4c515b, 0xff747983);
        }
        return switch (this.style) {
            case PRIMARY -> new Palette(EditorTheme.AVARITIA_CYAN,
                    highlighted ? 0xff3d686b : 0xff31575a,
                    highlighted ? 0xff79dddd : 0xff548f92,
                    EditorTheme.AVARITIA_CYAN, EditorTheme.TEXT);
            case DANGER -> new Palette(EditorTheme.AVARITIA_RED,
                    highlighted ? 0xff65343a : 0xff4d2c31,
                    highlighted ? 0xffb45a61 : 0xff85444a,
                    EditorTheme.AVARITIA_RED, EditorTheme.TEXT);
            case QUIET -> new Palette(highlighted ? 0xff69707c : 0xff454a54,
                    highlighted ? 0xff393d45 : 0xff292c33,
                    highlighted ? 0xff525863 : 0xff3a3e47,
                    EditorTheme.AVARITIA_GOLD, EditorTheme.TEXT);
            case TAB -> new Palette(highlighted ? EditorTheme.AVARITIA_RED : 0xff4d535e,
                    highlighted ? 0xff3b3238 : 0xff25282e,
                    highlighted ? 0xff5a474d : 0xff373a42,
                    EditorTheme.AVARITIA_RED, highlighted ? EditorTheme.TEXT : EditorTheme.TEXT_MUTED);
            case LIST -> new Palette(highlighted ? EditorTheme.AVARITIA_CYAN : 0xff484d57,
                    highlighted ? 0xff354347 : 0xff292c33,
                    highlighted ? 0xff4c6064 : 0xff3a3e46,
                    EditorTheme.AVARITIA_CYAN, EditorTheme.TEXT);
            case DEFAULT -> new Palette(highlighted ? EditorTheme.AVARITIA_GOLD : 0xff555b66,
                    highlighted ? 0xff42464f : 0xff33363e,
                    highlighted ? 0xff5d626d : 0xff484c55,
                    EditorTheme.AVARITIA_GOLD, EditorTheme.TEXT);
        };
    }

    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, this.getMessage());
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

        EditorButton build() {
            return new EditorButton(this.x, this.y, this.width, this.height,
                    this.message, this.onPress, this.style, this.selected);
        }
    }

    private record Palette(int border, int background, int highlight, int accent, int text) {
    }
}
