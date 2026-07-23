package committee.nova.mods.avaritia_tweak;

import com.mojang.logging.LogUtils;
import committee.nova.mods.avaritia_tweak.init.ModReg;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;


@Mod(AvaritiaTweak.MOD_ID)
public class AvaritiaTweak {

    public static final String MOD_ID = "avaritia_tweak";
    public static final Logger LOGGER = LogUtils.getLogger();

    public AvaritiaTweak(IEventBus modEventBus) {
        ModReg.BLOCKS.register(modEventBus);
        ModReg.ITEMS.register(modEventBus);
        ModReg.TABS.register(modEventBus);
        ModReg.BLOCK_ENTITIES.register(modEventBus);
        ModReg.MENUS.register(modEventBus);
    }
}
