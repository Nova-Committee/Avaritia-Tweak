package committee.nova.mods.avaritia_tweak.client.customization.screen;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/**
 * Immutable primitive palettes for the native customization editor.
 *
 * <p>{@link EditorTheme} maps these values to semantic and component colors. Keeping raw values
 * here makes theme switching independent from screen layout and workflow state.</p>
 */
enum EditorThemeStyle {
    CLASSIC("classic", "gui.avaritia_tweak.theme.classic", new Palette(
            0xff0b0c11, 0xff202228, 0xff292c33, 0xff17191e, 0xff343840,
            0xff5c626d, 0xff08090c, 0xffe4e5e9, 0xff9da3ae, 0xff737985,
            0xffd9535b, 0xff62c7c7, 0xffe3bd62,
            0xff6ed39d, 0xffffc76b, 0xffff6b6b,
            0xff7fa978, 0xff8fc7df, 0xffd8b886, 0xffc59be1)),
    SAKURA_NIGHT("sakura_night", "gui.avaritia_tweak.theme.sakura_night", new Palette(
            0xff100b12, 0xff281f2a, 0xff332735, 0xff1b141f, 0xff443348,
            0xff7a6078, 0xff0c080e, 0xfff5e9f0, 0xffc2a9b8, 0xff8c7482,
            0xffe36d91, 0xff75cfd0, 0xffffcf83,
            0xff82d6a8, 0xffffc978, 0xffff7185,
            0xff93b58f, 0xff9ad4e6, 0xffffc39b, 0xffd5a4ef)),
    VOID("void", "gui.avaritia_tweak.theme.void", new Palette(
            0xff090812, 0xff1b1930, 0xff24213a, 0xff121022, 0xff302c4c,
            0xff625f82, 0xff05040b, 0xffecebff, 0xffaaa7c7, 0xff767293,
            0xffa76bda, 0xff6ed9e8, 0xffe8c86d,
            0xff72d6a0, 0xffffc86f, 0xffff6f91,
            0xff79a987, 0xff89cde0, 0xffe3b982, 0xffc49aef));

    private final String id;
    private final String translationKey;
    private final Palette palette;

    EditorThemeStyle(String id, String translationKey, Palette palette) {
        this.id = id;
        this.translationKey = translationKey;
        this.palette = palette;
    }

    String id() {
        return this.id;
    }

    String translationKey() {
        return this.translationKey;
    }

    Palette palette() {
        return this.palette;
    }

    EditorThemeStyle next() {
        EditorThemeStyle[] values = values();
        return values[(this.ordinal() + 1) % values.length];
    }

    static Optional<EditorThemeStyle> find(String id) {
        if (id == null) {
            return Optional.empty();
        }
        String normalized = id.strip().toLowerCase(Locale.ROOT);
        return Arrays.stream(values()).filter(style -> style.id.equals(normalized)).findFirst();
    }

    record Palette(int backdrop, int window, int panel, int panelDark, int panelRaised,
                   int border, int borderDark, int text, int textMuted, int textFaint,
                   int structure, int focus, int heading, int success, int warning, int error,
                   int syntaxComment, int syntaxProperty, int syntaxString, int syntaxKeyword) {
    }
}
