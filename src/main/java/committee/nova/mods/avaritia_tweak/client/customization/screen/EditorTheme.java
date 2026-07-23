package committee.nova.mods.avaritia_tweak.client.customization.screen;

import committee.nova.mods.avaritia_tweak.customization.diff.ChangeType;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

final class EditorTheme {
    static final int BACKDROP = 0xff0b0c11;
    static final int WINDOW = 0xff202228;
    static final int PANEL = 0xff292c33;
    static final int PANEL_DARK = 0xff17191e;
    static final int PANEL_RAISED = 0xff343840;
    static final int BORDER = 0xff5c626d;
    static final int BORDER_DARK = 0xff08090c;
    static final int TEXT = 0xffe4e5e9;
    static final int TEXT_MUTED = 0xff9da3ae;
    static final int TEXT_FAINT = 0xff737985;
    static final int AVARITIA_RED = 0xffd9535b;
    static final int AVARITIA_CYAN = 0xff62c7c7;
    static final int AVARITIA_GOLD = 0xffe3bd62;
    static final int SUCCESS = 0xff6ed39d;
    static final int WARNING = 0xffffc76b;
    static final int ERROR = 0xffff6b6b;
    static final int MODIFIED = 0xffffc76b;
    static final int ADDED = 0xff6ed39d;
    static final int REMOVED = 0xffff6b6b;

    private static final int CODE_LINE_HEIGHT = 10;

    private EditorTheme() {
    }

    static void renderBackdrop(GuiGraphics graphics, int width, int height) {
        graphics.fill(0, 0, width, height, BACKDROP);
        graphics.fill(0, 0, width, Math.max(1, height / 2), 0xff11131a);
        for (int index = 0; index < 48; index++) {
            int x = Math.floorMod(index * 73 + 19, Math.max(1, width));
            int y = Math.floorMod(index * 41 + 7, Math.max(1, height));
            int color = index % 5 == 0 ? 0x5577d6d6 : 0x334b5262;
            graphics.fill(x, y, x + 1, y + 1, color);
        }
    }

    static void renderWindow(GuiGraphics graphics, int x, int y, int width, int height, int accent) {
        graphics.fill(x + 3, y + 4, x + width + 3, y + height + 4, 0x99000000);
        graphics.fill(x, y, x + width, y + height, BORDER_DARK);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, BORDER);
        graphics.fill(x + 2, y + 2, x + width - 2, y + height - 2, WINDOW);
        graphics.fill(x + 2, y + 2, x + width - 2, y + 5, accent);
        graphics.fill(x + 2, y + 5, x + width - 2, y + 6, 0xff101116);
    }

    static void renderPanel(GuiGraphics graphics, int x, int y, int width, int height, int accent) {
        graphics.fill(x, y, x + width, y + height, BORDER_DARK);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, BORDER);
        graphics.fill(x + 2, y + 2, x + width - 2, y + height - 2, PANEL);
        graphics.fill(x + 2, y + 2, x + 4, y + height - 2, accent);
        graphics.fill(x + 4, y + 2, x + width - 2, y + 3, 0xff474c56);
    }

    static void renderCanvasGrid(GuiGraphics graphics, int x, int y, int width, int height) {
        int right = x + width;
        int bottom = y + height;
        for (int gridX = x + 12; gridX < right; gridX += 12) {
            graphics.fill(gridX, y, gridX + 1, bottom, 0x122f343d);
        }
        for (int gridY = y + 12; gridY < bottom; gridY += 12) {
            graphics.fill(x, gridY, right, gridY + 1, 0x122f343d);
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
        graphics.fill(x, y + 1, x + width, y + 2, 0xff444952);
    }

    static void renderStatusBar(GuiGraphics graphics, Font font, int y, int width,
                                String left, String center, String right, int statusColor) {
        graphics.fill(0, y, width, y + 18, 0xff181a20);
        graphics.fill(0, y, width, y + 1, 0xff505661);
        graphics.fill(0, y, 4, y + 18, AVARITIA_RED);
        graphics.drawString(font, ScreenText.fit(font, left, Math.max(20, width / 3 - 12)),
                9, y + 5, statusColor, false);
        if (!center.isBlank() && width >= 420) {
            String fitted = ScreenText.fit(font, center, width / 3);
            graphics.drawCenteredString(font, fitted, width / 2, y + 5, TEXT_MUTED);
        }
        String fittedRight = ScreenText.fit(font, right, Math.max(20, width / 3 - 12));
        graphics.drawString(font, fittedRight, width - 8 - font.width(fittedRight), y + 5,
                AVARITIA_CYAN, false);
    }

    static void renderBadge(GuiGraphics graphics, Font font, int x, int y, String text, int color) {
        int width = font.width(text) + 8;
        graphics.fill(x, y, x + width, y + 13, 0xff111319);
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
        int outer = hovered ? AVARITIA_GOLD : 0xff101116;
        graphics.fill(x, y, x + width, y + height, outer);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xff8c9198);
        graphics.fill(x + 2, y + 2, x + width - 1, y + height - 1, 0xff2c2f36);
        graphics.fill(x + 2, y + 2, x + width - 2, y + 3, 0xff555a64);
        graphics.fill(x + 2, y + 2, x + 3, y + height - 2, 0xff555a64);
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
        graphics.fill(x + 1, y + 1, x + gutterWidth, y + height - 1, 0xff202229);
        graphics.fill(x + gutterWidth, y + 1, x + gutterWidth + 1, y + height - 1, 0xff444953);
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

    private static int syntaxColor(String line, String logicalPath) {
        String trimmed = line.stripLeading();
        if (trimmed.startsWith("//") || trimmed.startsWith("#") || trimmed.startsWith("/*")
                || trimmed.startsWith("*") || trimmed.startsWith("<!--")) {
            return 0xff7fa978;
        }
        String path = logicalPath.toLowerCase(Locale.ROOT);
        if (path.endsWith(".json")) {
            if (trimmed.startsWith("\"") && trimmed.contains(":")) {
                return 0xff8fc7df;
            }
            return trimmed.contains("\"") ? 0xffd8b886 : TEXT;
        }
        if (trimmed.contains("ServerEvents") || trimmed.contains("AvaritiaEvents")
                || trimmed.contains("mods.avaritia")) {
            return 0xffc59be1;
        }
        if (trimmed.contains(".id(") || trimmed.startsWith("<")) {
            return AVARITIA_CYAN;
        }
        if (trimmed.contains("\"") || trimmed.contains("'")) {
            return 0xffd8b886;
        }
        return TEXT;
    }
}
