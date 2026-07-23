package committee.nova.mods.avaritia_tweak.client.customization.screen;

import committee.nova.mods.avaritia_tweak.customization.diff.ChangeType;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fml.loading.FMLPaths;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

final class EditorTheme {
    private static final int CODE_LINE_HEIGHT = 10;
    private static final EditorThemePreferenceStore PREFERENCES = new EditorThemePreferenceStore(
            FMLPaths.CONFIGDIR.get().resolve("avaritia_tweak/visual_editor/ui-theme.txt"));

    static int BACKDROP;
    static int WINDOW;
    static int PANEL;
    static int PANEL_DARK;
    static int PANEL_RAISED;
    static int BORDER;
    static int BORDER_DARK;
    static int TEXT;
    static int TEXT_MUTED;
    static int TEXT_FAINT;
    static int AVARITIA_RED;
    static int AVARITIA_CYAN;
    static int AVARITIA_GOLD;
    static int SUCCESS;
    static int WARNING;
    static int ERROR;
    static int MODIFIED;
    static int ADDED;
    static int REMOVED;

    private static int syntaxComment;
    private static int syntaxProperty;
    private static int syntaxString;
    private static int syntaxKeyword;
    private static EditorThemeStyle currentStyle;
    private static Optional<String> startupWarning;

    static {
        EditorThemePreferenceStore.LoadResult loaded = PREFERENCES.load();
        startupWarning = loaded.warning();
        apply(loaded.style());
    }

    private EditorTheme() {
    }

    static EditorThemeStyle currentStyle() {
        return currentStyle;
    }

    static Optional<String> startupWarning() {
        return startupWarning;
    }

    static ThemeSwitchResult cycleTheme() {
        EditorThemeStyle next = currentStyle.next();
        apply(next);
        Optional<String> warning = PREFERENCES.save(next);
        startupWarning = warning;
        return new ThemeSwitchResult(next, warning);
    }

    private static void apply(EditorThemeStyle style) {
        EditorThemeStyle.Palette palette = style.palette();
        currentStyle = style;
        BACKDROP = palette.backdrop();
        WINDOW = palette.window();
        PANEL = palette.panel();
        PANEL_DARK = palette.panelDark();
        PANEL_RAISED = palette.panelRaised();
        BORDER = palette.border();
        BORDER_DARK = palette.borderDark();
        TEXT = palette.text();
        TEXT_MUTED = palette.textMuted();
        TEXT_FAINT = palette.textFaint();
        AVARITIA_RED = palette.structure();
        AVARITIA_CYAN = palette.focus();
        AVARITIA_GOLD = palette.heading();
        SUCCESS = palette.success();
        WARNING = palette.warning();
        ERROR = palette.error();
        MODIFIED = palette.warning();
        ADDED = palette.success();
        REMOVED = palette.error();
        syntaxComment = palette.syntaxComment();
        syntaxProperty = palette.syntaxProperty();
        syntaxString = palette.syntaxString();
        syntaxKeyword = palette.syntaxKeyword();
    }

    static void renderBackdrop(GuiGraphics graphics, int width, int height) {
        graphics.fill(0, 0, width, height, BACKDROP);
        graphics.fill(0, 0, width, Math.max(1, height / 2), mix(BACKDROP, WINDOW, 28));
        for (int index = 0; index < 48; index++) {
            int x = Math.floorMod(index * 73 + 19, Math.max(1, width));
            int y = Math.floorMod(index * 41 + 7, Math.max(1, height));
            int color = index % 5 == 0 ? withAlpha(AVARITIA_CYAN, 0x55) : withAlpha(BORDER, 0x33);
            graphics.fill(x, y, x + 1, y + 1, color);
        }
    }

    static void renderWindow(GuiGraphics graphics, int x, int y, int width, int height, int accent) {
        graphics.fill(x + 3, y + 4, x + width + 3, y + height + 4, withAlpha(BORDER_DARK, 0x99));
        graphics.fill(x, y, x + width, y + height, BORDER_DARK);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, BORDER);
        graphics.fill(x + 2, y + 2, x + width - 2, y + height - 2, WINDOW);
        graphics.fill(x + 2, y + 2, x + width - 2, y + 5, accent);
        graphics.fill(x + 2, y + 5, x + width - 2, y + 6, mix(BORDER_DARK, WINDOW, 22));
    }

    static void renderPanel(GuiGraphics graphics, int x, int y, int width, int height, int accent) {
        graphics.fill(x, y, x + width, y + height, BORDER_DARK);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, BORDER);
        graphics.fill(x + 2, y + 2, x + width - 2, y + height - 2, PANEL);
        graphics.fill(x + 2, y + 2, x + 4, y + height - 2, accent);
        graphics.fill(x + 4, y + 2, x + width - 2, y + 3, mix(PANEL, TEXT_MUTED, 25));
    }

    static void renderCanvasGrid(GuiGraphics graphics, int x, int y, int width, int height) {
        int right = x + width;
        int bottom = y + height;
        int gridColor = withAlpha(mix(PANEL_DARK, AVARITIA_CYAN, 25), 0x18);
        for (int gridX = x + 12; gridX < right; gridX += 12) {
            graphics.fill(gridX, y, gridX + 1, bottom, gridColor);
        }
        for (int gridY = y + 12; gridY < bottom; gridY += 12) {
            graphics.fill(x, gridY, right, gridY + 1, gridColor);
        }
    }

    static void renderSectionHeader(GuiGraphics graphics, Font font, int x, int y, int width,
                                    Component title, String meta, int accent) {
        graphics.fill(x, y, x + width, y + 18, PANEL_RAISED);
        graphics.fill(x, y + 17, x + width, y + 18, BORDER_DARK);
        graphics.fill(x, y, x + 3, y + 18, accent);
        graphics.drawString(font, title, x + 8, y + 5, TEXT, false);
        if (!meta.isBlank()) {
            String fitted = ScreenText.fit(font, meta, Math.max(0, width / 2));
            graphics.drawString(font, fitted, x + width - 6 - font.width(fitted), y + 5, TEXT_MUTED, false);
        }
    }

    static void renderDivider(GuiGraphics graphics, int x, int y, int width) {
        graphics.fill(x, y, x + width, y + 1, BORDER_DARK);
        graphics.fill(x, y + 1, x + width, y + 2, dividerSoft());
    }

    static void renderStatusBar(GuiGraphics graphics, Font font, int y, int width,
                                String left, String center, String right, int statusColor) {
        int edgeInset = width >= 160 ? 42 : 9;
        graphics.fill(0, y, width, y + 18, mix(PANEL_DARK, WINDOW, 24));
        graphics.fill(0, y, width, y + 1, mix(PANEL, BORDER, 64));
        graphics.fill(0, y, 4, y + 18, AVARITIA_RED);
        graphics.drawString(font, ScreenText.fit(font, left,
                        Math.max(20, width / 3 - edgeInset - 4)),
                edgeInset, y + 5, statusColor, false);
        if (!center.isBlank() && width >= 420) {
            String fitted = ScreenText.fit(font, center, width / 3);
            graphics.drawCenteredString(font, fitted, width / 2, y + 5, TEXT_MUTED);
        }
        String fittedRight = ScreenText.fit(font, right,
                Math.max(20, width / 3 - edgeInset - 4));
        graphics.drawString(font, fittedRight, width - edgeInset - font.width(fittedRight), y + 5,
                AVARITIA_CYAN, false);
    }

    static void renderBadge(GuiGraphics graphics, Font font, int x, int y, String text, int color) {
        int width = font.width(text) + 8;
        graphics.fill(x, y, x + width, y + 13, PANEL_DARK);
        graphics.fill(x, y, x + 2, y + 13, color);
        graphics.fill(x + 2, y, x + width, y + 1, color);
        graphics.drawString(font, text, x + 5, y + 3, color, false);
    }

    static void renderChangeSummary(GuiGraphics graphics, Font font, int x, int y,
                                    long added, long modified, long removed) {
        renderBadge(graphics, font, x, y, "A " + added, ADDED);
        int modifiedX = x + font.width("A " + added) + 14;
        renderBadge(graphics, font, modifiedX, y, "M " + modified, MODIFIED);
        int removedX = modifiedX + font.width("M " + modified) + 14;
        renderBadge(graphics, font, removedX, y, "D " + removed, REMOVED);
    }

    static void renderSlot(GuiGraphics graphics, int x, int y, int width, int height, boolean hovered) {
        int outer = hovered ? AVARITIA_GOLD : mix(BORDER_DARK, PANEL_DARK, 18);
        graphics.fill(x, y, x + width, y + height, outer);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, mix(BORDER, TEXT, 35));
        graphics.fill(x + 2, y + 2, x + width - 1, y + height - 1, mix(PANEL_DARK, PANEL, 42));
        int highlight = mix(PANEL, TEXT_MUTED, 28);
        graphics.fill(x + 2, y + 2, x + width - 2, y + 3, highlight);
        graphics.fill(x + 2, y + 2, x + 3, y + height - 2, highlight);
    }

    static int changeColor(ChangeType type) {
        return switch (type) {
            case ADDED -> ADDED;
            case MODIFIED -> MODIFIED;
            case REMOVED -> REMOVED;
        };
    }

    static String changeMark(ChangeType type) {
        return switch (type) {
            case ADDED -> "A";
            case MODIFIED -> "M";
            case REMOVED -> "D";
        };
    }

    static int visibleCodeLines(int height) {
        return Math.max(1, (height - 4) / CODE_LINE_HEIGHT);
    }

    static void renderCodeViewport(GuiGraphics graphics, Font font, int x, int y, int width, int height,
                                   List<String> lines, int lineOffset, String logicalPath,
                                   Optional<ChangeType> change) {
        graphics.fill(x, y, x + width, y + height, BORDER_DARK);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, PANEL_DARK);
        int gutterWidth = Math.min(38, Math.max(26, width / 8));
        graphics.fill(x + 1, y + 1, x + gutterWidth, y + height - 1, mix(PANEL_DARK, PANEL, 48));
        graphics.fill(x + gutterWidth, y + 1, x + gutterWidth + 1, y + height - 1, dividerSoft());
        change.ifPresent(type -> graphics.fill(x + 1, y + 1, x + 3, y + height - 1, changeColor(type)));

        int visible = visibleCodeLines(height);
        int end = Math.min(lines.size(), lineOffset + visible);
        graphics.enableScissor(x + 1, y + 1, x + width - 1, y + height - 1);
        for (int index = lineOffset; index < end; index++) {
            int lineY = y + 3 + (index - lineOffset) * CODE_LINE_HEIGHT;
            String lineNumber = Integer.toString(index + 1);
            graphics.drawString(font, lineNumber,
                    x + gutterWidth - 5 - font.width(lineNumber), lineY, TEXT_FAINT, false);
            String line = ScreenText.fit(font, lines.get(index), Math.max(0, width - gutterWidth - 8));
            graphics.drawString(font, line, x + gutterWidth + 5, lineY,
                    syntaxColor(lines.get(index), logicalPath), false);
        }
        graphics.disableScissor();
    }

    static int headerBackground() {
        return mix(PANEL_DARK, WINDOW, 45);
    }

    static int errorSurface() {
        return mix(PANEL_DARK, ERROR, 22);
    }

    static int dividerSoft() {
        return mix(PANEL, BORDER, 62);
    }

    static int selectionSurface() {
        return mix(PANEL_DARK, AVARITIA_CYAN, 22);
    }

    static int mix(int first, int second, int secondPercent) {
        int weight = Math.max(0, Math.min(100, secondPercent));
        int firstWeight = 100 - weight;
        int alpha = channel(first, 24) * firstWeight / 100 + channel(second, 24) * weight / 100;
        int red = channel(first, 16) * firstWeight / 100 + channel(second, 16) * weight / 100;
        int green = channel(first, 8) * firstWeight / 100 + channel(second, 8) * weight / 100;
        int blue = channel(first, 0) * firstWeight / 100 + channel(second, 0) * weight / 100;
        return alpha << 24 | red << 16 | green << 8 | blue;
    }

    static int withAlpha(int color, int alpha) {
        return Math.max(0, Math.min(255, alpha)) << 24 | (color & 0x00ffffff);
    }

    private static int channel(int color, int shift) {
        return color >>> shift & 0xff;
    }

    private static int syntaxColor(String line, String logicalPath) {
        String trimmed = line.stripLeading();
        if (trimmed.startsWith("//") || trimmed.startsWith("#") || trimmed.startsWith("/*")
                || trimmed.startsWith("*") || trimmed.startsWith("<!--")) {
            return syntaxComment;
        }
        String path = logicalPath.toLowerCase(Locale.ROOT);
        if (path.endsWith(".json")) {
            if (trimmed.startsWith("\"") && trimmed.contains(":")) {
                return syntaxProperty;
            }
            return trimmed.contains("\"") ? syntaxString : TEXT;
        }
        if (trimmed.contains("ServerEvents") || trimmed.contains("AvaritiaEvents")
                || trimmed.contains("mods.avaritia")) {
            return syntaxKeyword;
        }
        if (trimmed.contains(".id(") || trimmed.startsWith("<")) {
            return AVARITIA_CYAN;
        }
        if (trimmed.contains("\"") || trimmed.contains("'")) {
            return syntaxString;
        }
        return TEXT;
    }

    record ThemeSwitchResult(EditorThemeStyle style, Optional<String> warning) {
    }
}
