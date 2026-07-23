package committee.nova.mods.avaritia_tweak.client.customization.screen;

@FunctionalInterface
interface SecondaryPressHandler<T> {
    void onPress(T widget, double mouseX, double mouseY);
}
