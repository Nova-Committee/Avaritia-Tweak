package committee.nova.mods.avaritia_tweak;

import com.mojang.logging.LogUtils;
import committee.nova.mods.avaritia_tweak.init.ModReg;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;


@Mod(AvaritiaTweak.MOD_ID)
public class AvaritiaTweak {

    public static final String MOD_ID = "avaritia_tweak";
    public static final Logger LOGGER = LogUtils.getLogger();

    public AvaritiaTweak() {
        var bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModReg.BLOCKS.register(bus);
        ModReg.ITEMS.register(bus);
        ModReg.TABS.register(bus);
        ModReg.BLOCK_ENTITIES.register(bus);
        ModReg.MENUS.register(bus);
    }
}
