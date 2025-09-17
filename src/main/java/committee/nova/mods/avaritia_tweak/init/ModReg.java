package committee.nova.mods.avaritia_tweak.init;

import committee.nova.mods.avaritia.api.common.item.BaseItem;
import committee.nova.mods.avaritia_tweak.AvaritiaTweak;
import committee.nova.mods.avaritia_tweak.common.RecipeGeneratorBlock;
import committee.nova.mods.avaritia_tweak.common.RecipeGeneratorMenu;
import committee.nova.mods.avaritia_tweak.common.RecipeGeneratorTile;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.network.IContainerFactory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author: cnlimiter
 */
public class ModReg {
    public static DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, AvaritiaTweak.MOD_ID);
    public static DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, AvaritiaTweak.MOD_ID);
    public static DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, AvaritiaTweak.MOD_ID);
    public static DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, AvaritiaTweak.MOD_ID);
    public static DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, AvaritiaTweak.MOD_ID);
    public static final List<RegistryObject<Item>> ACCEPT_ITEM = new ArrayList<>();


    public static RegistryObject<Block> recipe_generator = itemBlock("recipe_generator_table", RecipeGeneratorBlock::new);
    public static RegistryObject<BlockEntityType<RecipeGeneratorTile>> recipe_generator_tile = blockEntity("recipe_generator_tile", RecipeGeneratorTile::new, () -> new Block[]{recipe_generator.get()});
    public static RegistryObject<MenuType<RecipeGeneratorMenu>> recipe_generator_menu = menu("recipe_generator_menu", RecipeGeneratorMenu::fromNetwork);
    public static final RegistryObject<CreativeModeTab> CREATIVE_TAB = TABS.register("avaritia_tweak_group", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.tab.avaritia_tweak"))
            .icon(() -> recipe_generator.get().asItem().getDefaultInstance())
            .displayItems((parameters, output) -> {
                for (var item : ACCEPT_ITEM) {
                    output.accept(item.get());
                }

            })
            .build());


    public static RegistryObject<Item> item(String name) {
        return item(name, true);
    }

    public static RegistryObject<Item> item(String name, boolean exist) {
        return item(name, (e) -> new BaseItem(), exist);
    }

    public static RegistryObject<Item> item(String name, Function<String, Item> item) {
        return item(name, item, true);
    }

    public static RegistryObject<Item> item(String name, Function<String, Item> item, boolean exist) {
        return item(name, () -> item.apply(name), exist);
    }

    public static RegistryObject<Item> item(String name, Supplier<Item> item) {
        return item(name, item, true);
    }

    public static RegistryObject<Item> item(String name, Supplier<Item> item, boolean exist) {
        var regItem = ITEMS.register(name, item);
        if (exist) ACCEPT_ITEM.add(regItem);
        return regItem;
    }

    private static RegistryObject<Block> baseBlock(String name, Supplier<Block> block) {
        return BLOCKS.register(name, block);
    }

    public static RegistryObject<Block> itemBlock(String name, Supplier<Block> block) {
        return itemBlock(name, block, true);
    }

    public static RegistryObject<Block> itemBlock(String name, Supplier<Block> block, boolean hasItem) {
        return itemBlock(name, block, hasItem, true, new Item.Properties());
    }

    public static RegistryObject<Block> itemBlock(String name, Supplier<Block> block, boolean hasItem, boolean exist) {
        return itemBlock(name, block, hasItem, exist, new Item.Properties());
    }

    public static RegistryObject<Block> itemBlock(String name, Supplier<Block> block, Rarity rarity) {
        return itemBlock(name, block, true, true, new Item.Properties().rarity(rarity));
    }

    public static RegistryObject<Block> itemBlock(String name, Supplier<Block> block, boolean hasItem, Item.Properties properties) {
        return itemBlock(name, block, hasItem, true, properties);
    }

    public static RegistryObject<Block> itemBlock(String name, Supplier<Block> block, boolean hasItem, boolean exist, Item.Properties properties) {
        var reg = BLOCKS.register(name, block);
        if (hasItem) item(name, () -> new BlockItem(reg.get(), properties), exist);
        return reg;
    }

    public static <T extends BlockEntity> RegistryObject<BlockEntityType<T>> blockEntity(String name, BlockEntityType.BlockEntitySupplier<T> tile, Supplier<Block[]> blocks) {
        return BLOCK_ENTITIES.register(name, () -> BlockEntityType.Builder.of(tile, blocks.get()).build(null));
    }

    public static <T extends AbstractContainerMenu> RegistryObject<MenuType<T>> menu(String name, IContainerFactory<T> container) {
        return MENUS.register(name, () -> IForgeMenuType.create(container));
    }
}
