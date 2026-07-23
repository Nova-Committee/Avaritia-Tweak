package committee.nova.mods.avaritia_tweak.client.customization.screen;

import net.minecraft.client.gui.GuiGraphics;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ThemedEditorScreenTest {
    @Test
    void standaloneEditorScreensShareTheProtectedBackgroundPass() {
        List<Class<?>> screens = List.of(
                ConflictScreen.class,
                HistoryScreen.class,
                IngredientEditorScreen.class,
                ItemStackEditorScreen.class,
                PreviewScreen.class,
                RecipeSelectScreen.class,
                RegistryItemSelectScreen.class,
                RollbackPreviewScreen.class,
                SingularitySelectScreen.class);

        assertThat(screens).allSatisfy(screen ->
                assertThat(ThemedEditorScreen.class.isAssignableFrom(screen)).isTrue());
    }

    @Test
    void subclassesCannotRestoreTheLateVanillaBackgroundPass() throws NoSuchMethodException {
        Method background = ThemedEditorScreen.class.getDeclaredMethod("renderBackground",
                GuiGraphics.class, int.class, int.class, float.class);

        assertThat(Modifier.isFinal(background.getModifiers())).isTrue();
    }
}
