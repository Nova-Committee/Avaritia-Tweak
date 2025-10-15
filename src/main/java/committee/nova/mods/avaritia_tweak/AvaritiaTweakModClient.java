package committee.nova.mods.avaritia_tweak;

import committee.nova.mods.avaritia_tweak.client.script.RecipeGeneratorScreen2;
import committee.nova.mods.avaritia_tweak.init.ModReg;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = AvaritiaTweak.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class AvaritiaTweakModClient {
    @SubscribeEvent
    public static void clientSetUp(FMLClientSetupEvent event) {
        MenuScreens.register(ModReg.recipe_generator_menu.get(), RecipeGeneratorScreen2::new);
    }
}
