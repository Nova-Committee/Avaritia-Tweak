package committee.nova.mods.avaritia_tweak.init;

import committee.nova.mods.avaritia_tweak.AvaritiaTweak;
import committee.nova.mods.avaritia_tweak.common.RecipeGeneratorBlock;
import committee.nova.mods.avaritia_tweak.common.RecipeGeneratorMenu;
import committee.nova.mods.avaritia_tweak.common.RecipeGeneratorTile;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.network.IContainerFactory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModReg {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(BuiltInRegistries.BLOCK, AvaritiaTweak.MOD_ID);
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(BuiltInRegistries.ITEM, AvaritiaTweak.MOD_ID);
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, AvaritiaTweak.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, AvaritiaTweak.MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(BuiltInRegistries.MENU, AvaritiaTweak.MOD_ID);

    public static final DeferredHolder<Block, Block> recipe_generator =
            BLOCKS.register("recipe_generator_table", RecipeGeneratorBlock::new);
    public static final DeferredHolder<Item, Item> recipe_generator_item = ITEMS.register(
            "recipe_generator_table", () -> new BlockItem(recipe_generator.get(), new Item.Properties()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RecipeGeneratorTile>>
            recipe_generator_tile = BLOCK_ENTITIES.register("recipe_generator_tile",
            () -> BlockEntityType.Builder.of(RecipeGeneratorTile::new, recipe_generator.get()).build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<RecipeGeneratorMenu>> recipe_generator_menu =
            MENUS.register("recipe_generator_menu", () -> new MenuType<>(
                    (IContainerFactory<RecipeGeneratorMenu>) RecipeGeneratorMenu::fromNetwork,
                    FeatureFlagSet.of()));
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> CREATIVE_TAB = TABS.register(
            "avaritia_tweak_group", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.tab.avaritia_tweak"))
                    .icon(() -> recipe_generator_item.get().getDefaultInstance())
                    .displayItems((parameters, output) -> output.accept(recipe_generator_item.get()))
                    .build());

    private ModReg() {
    }
}
