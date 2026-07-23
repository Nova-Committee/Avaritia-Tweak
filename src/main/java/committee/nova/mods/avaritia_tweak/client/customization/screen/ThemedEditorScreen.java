package committee.nova.mods.avaritia_tweak.client.customization.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

/**
 * Keeps the vanilla background pass behind the editor's custom-drawn content.
 *
 * <p>Minecraft 1.21 renders the blurred background from {@link Screen#render}, so editor
 * screens must render that pass before drawing their own panels. The later call to
 * {@code super.render(...)} then renders widgets without blurring the completed editor.</p>
 */
abstract class ThemedEditorScreen extends Screen {
    protected ThemedEditorScreen(Component title) {
        super(title);
    }

    protected final void renderThemedBackground(@NotNull GuiGraphics graphics,
                                                int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        EditorTheme.renderBackdrop(graphics, this.width, this.height);
    }

    @Override
    public final void renderBackground(@NotNull GuiGraphics graphics,
                                       int mouseX, int mouseY, float partialTick) {
        // renderThemedBackground already rendered this pass before the custom editor content.
    }
}
