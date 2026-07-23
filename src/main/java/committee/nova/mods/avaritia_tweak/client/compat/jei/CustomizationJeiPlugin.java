package committee.nova.mods.avaritia_tweak.client.compat.jei;

import committee.nova.mods.avaritia_tweak.AvaritiaTweak;
import committee.nova.mods.avaritia_tweak.client.customization.screen.CustomizationEditorScreen;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

/**
 * Keeps JEI's ingredient overlay out of the full-screen customization editor.
 */
@JeiPlugin
public final class CustomizationJeiPlugin implements IModPlugin {
    private static final ResourceLocation UID = Objects.requireNonNull(
            ResourceLocation.tryBuild(AvaritiaTweak.MOD_ID, "customization_editor"));

    @Override
    public @NotNull ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGuiContainerHandler(CustomizationEditorScreen.class,
                new IGuiContainerHandler<>() {
                    @Override
                    public @NotNull List<Rect2i> getGuiExtraAreas(CustomizationEditorScreen screen) {
                        return List.of(new Rect2i(0, 0, screen.width, screen.height));
                    }
                });
    }
}
