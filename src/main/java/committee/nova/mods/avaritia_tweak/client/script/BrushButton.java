package committee.nova.mods.avaritia_tweak.client.script;

import committee.nova.mods.avaritia.api.client.screen.coordinate.TextureCoordinate;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.NotNull;

import static committee.nova.mods.avaritia_tweak.client.script.RecipeGeneratorScreen.BACKGROUND;

/**
 * @author cnlimiter
 */
public class BrushButton extends Button {
    public BrushButton(int x, int y, Button.OnPress onPress) {
        super(x, y, 22, 24, Component.translatable("gui.avaritia.recipe_generator.brush"), onPress, DEFAULT_NARRATION);
    }

    @Override
    protected @NotNull MutableComponent createNarrationMessage() {
        return CommonComponents.joinForNarration(super.createNarrationMessage(), Component.translatable("gui.avaritia.recipe_generator.brush"));
    }

    @Override
    public void renderWidget(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        TextureCoordinate icon;
        if (this.isHovered()) {
            icon = new TextureCoordinate(23, 304);
        } else {
            icon = new TextureCoordinate(0, 304);
        }
        guiGraphics.blit(BACKGROUND, this.getX(), this.getY(), icon.xTexStart(), icon.yTexStart(), this.width, this.height, 512, 512);

    }

}
