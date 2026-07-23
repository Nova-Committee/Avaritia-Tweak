package committee.nova.mods.avaritia_tweak.client.customization.screen;

final class ScreenText {
    private ScreenText() {
    }

    static String ellipsize(String value, int maximum) {
        if (maximum <= 3 || value.length() <= maximum) {
            return value.length() <= maximum ? value : value.substring(0, Math.max(0, maximum));
        }
        return value.substring(0, maximum - 3) + "...";
    }
}
