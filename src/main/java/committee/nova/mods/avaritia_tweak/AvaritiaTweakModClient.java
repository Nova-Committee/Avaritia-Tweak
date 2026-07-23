package committee.nova.mods.avaritia_tweak;

import committee.nova.mods.avaritia_tweak.client.customization.screen.CustomizationEditorScreen;
import committee.nova.mods.avaritia_tweak.init.ModReg;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = AvaritiaTweak.MOD_ID, value = Dist.CLIENT)
public class AvaritiaTweakModClient {
    @SubscribeEvent
    public static void registerMenuScreens(RegisterMenuScreensEvent event) {
        event.register(ModReg.recipe_generator_menu.get(), CustomizationEditorScreen::new);
    }
}
