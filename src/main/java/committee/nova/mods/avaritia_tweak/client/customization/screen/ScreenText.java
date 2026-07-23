package committee.nova.mods.avaritia_tweak.client.customization.screen;

import net.minecraft.client.gui.Font;

final class ScreenText {
    private ScreenText() {
    }

    static String ellipsize(String value, int maximum) {
        if (maximum <= 3 || value.length() <= maximum) {
            return value.length() <= maximum ? value : value.substring(0, Math.max(0, maximum));
        }
        return value.substring(0, maximum - 3) + "...";
    }

    static String fit(Font font, String value, int maximumWidth) {
        if (maximumWidth <= 0) {
            return "";
        }
        if (font.width(value) <= maximumWidth) {
            return value;
        }
        String suffix = "...";
        int suffixWidth = font.width(suffix);
        if (suffixWidth >= maximumWidth) {
            return font.plainSubstrByWidth(value, maximumWidth);
        }
        return font.plainSubstrByWidth(value, maximumWidth - suffixWidth) + suffix;
    }
}
