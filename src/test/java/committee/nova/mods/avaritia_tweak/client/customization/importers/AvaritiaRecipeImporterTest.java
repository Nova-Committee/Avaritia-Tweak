package committee.nova.mods.avaritia_tweak.client.customization.importers;

import committee.nova.mods.avaritia.common.crafting.recipe.CompressorRecipe;
import committee.nova.mods.avaritia.common.crafting.recipe.EternalSingularityCraftRecipe;
import committee.nova.mods.avaritia.common.crafting.recipe.ExtremeSmithingRecipe;
import committee.nova.mods.avaritia.common.crafting.recipe.InfinityCatalystCraftRecipe;
import committee.nova.mods.avaritia.common.crafting.recipe.NoConsumeCatalystShapedRecipe;
import committee.nova.mods.avaritia.common.crafting.recipe.ShapedTableCraftingRecipe;
import committee.nova.mods.avaritia.common.crafting.recipe.ShapelessTableCraftingRecipe;
import committee.nova.mods.avaritia.core.singularity.Singularity;
import committee.nova.mods.avaritia_tweak.customization.model.CraftingTier;
import committee.nova.mods.avaritia_tweak.customization.model.CustomizationEntry;
import committee.nova.mods.avaritia_tweak.customization.model.IngredientSpec;
import committee.nova.mods.avaritia_tweak.customization.model.ItemStackSpec;
import committee.nova.mods.avaritia_tweak.customization.model.OutputTarget;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.common.crafting.CompoundIngredient;
import net.neoforged.neoforge.common.crafting.DataComponentIngredient;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AvaritiaRecipeImporterTest {
    private final AvaritiaRecipeImporter importer = new AvaritiaRecipeImporter(
            new IngredientImporter(), stack -> new ItemStackSpec(id("minecraft:diamond"), 1),
            new AvaritiaRecipeImporter.SpecialRecipeReader() {
                @Override
                public AvaritiaRecipeImporter.DecodedSpecial catalyst(
                        InfinityCatalystCraftRecipe recipe, RegistryAccess registryAccess) {
                    return new AvaritiaRecipeImporter.DecodedSpecial(
                            "custom", List.of(stone(), dirt()), 3);
                }

                @Override
                public AvaritiaRecipeImporter.DecodedSpecial eternal(
                        EternalSingularityCraftRecipe recipe, RegistryAccess registryAccess) {
                    return new AvaritiaRecipeImporter.DecodedSpecial("", List.of(stone()), 4);
                }
            });

    @Test
    void importsAllSixAvaritiaRecipeKindsAndTheirSpecificFields() {
        ShapedTableCraftingRecipe shaped = mock(ShapedTableCraftingRecipe.class);
        when(shaped.getTier()).thenReturn(2);
        when(shaped.getWidth()).thenReturn(2);
        when(shaped.getHeight()).thenReturn(1);
        when(shaped.getIngredients()).thenReturn(ingredients(stone(), dirt()));
        when(shaped.getResultItem(any())).thenReturn(ItemStack.EMPTY);

        ShapelessTableCraftingRecipe shapeless = new ShapelessTableCraftingRecipe(
                ingredients(stone(), dirt()), ItemStack.EMPTY, 3);
        CompressorRecipe compressor = new CompressorRecipe(stone(), ItemStack.EMPTY, 800, 120);
        ExtremeSmithingRecipe smithing = new ExtremeSmithingRecipe(
                stone(), dirt(), CompoundIngredient.of(stone(), dirt()), ItemStack.EMPTY);

        CustomizationEntry.ShapedTable importedShaped = (CustomizationEntry.ShapedTable)
                success("test:shaped", shaped);
        CustomizationEntry.ShapelessTable importedShapeless = (CustomizationEntry.ShapelessTable)
                success("test:shapeless", shapeless);
        CustomizationEntry.Compressor importedCompressor = (CustomizationEntry.Compressor)
                success("test:compressor", compressor);
        CustomizationEntry.ExtremeSmithing importedSmithing = (CustomizationEntry.ExtremeSmithing)
                success("test:smithing", smithing);
        CustomizationEntry.InfinityCatalyst importedCatalyst =
                (CustomizationEntry.InfinityCatalyst) success("test:catalyst", catalyst());
        CustomizationEntry.EternalSingularity importedEternal =
                (CustomizationEntry.EternalSingularity) success("test:eternal", eternal());

        assertThat(importedShaped.tier()).isEqualTo(CraftingTier.NETHER);
        assertThat(importedShaped.id()).isEqualTo(id("test:shaped"));
        assertThat(importedShaped.ingredients()).hasSize(2);
        assertThat(importedShaped.result().count()).isEqualTo(1);
        assertThat(importedShapeless.tier()).isEqualTo(CraftingTier.END);
        assertThat(importedShapeless.ingredients()).hasSize(2);
        assertThat(importedCompressor.inputCount()).isEqualTo(800);
        assertThat(importedCompressor.timeCost()).isEqualTo(120);
        assertThat(importedSmithing.addition()).isInstanceOfSatisfying(IngredientSpec.Choice.class,
                choice -> assertThat(choice.alternatives()).containsExactly(
                        new IngredientSpec.Item(id("minecraft:stone")),
                        new IngredientSpec.Item(id("minecraft:dirt"))));
        assertThat(importedCatalyst.group()).isEqualTo("custom");
        assertThat(importedCatalyst.count()).isEqualTo(3);
        assertThat(importedCatalyst.ingredients()).hasSize(2);
        assertThat(importedEternal.count()).isEqualTo(4);
        assertThat(importedEternal.ingredients()).hasSize(1);
    }

    @Test
    void rejectsDatapackRecipeTargetBeforeCreatingAnEntry() {
        CompressorRecipe recipe = new CompressorRecipe(stone(), ItemStack.EMPTY, 1, 1);

        assertThat(this.importer.importRecipe(holder("test:compressor", recipe),
                OutputTarget.DATAPACK, RegistryAccess.EMPTY))
                .isInstanceOfSatisfying(RecipeImportResult.Failure.class,
                        failure -> assertThat(failure.code())
                                .isEqualTo("recipe.target.unsupported"));
    }

    @Test
    void preservesNoConsumeRecipeIdentityAndRejectsCraftTweakerTarget() {
        NoConsumeCatalystShapedRecipe recipe = mock(NoConsumeCatalystShapedRecipe.class);
        when(recipe.getTier()).thenReturn(1);
        when(recipe.getWidth()).thenReturn(2);
        when(recipe.getHeight()).thenReturn(1);
        when(recipe.getIngredients()).thenReturn(ingredients(stone(), dirt()));
        when(recipe.getResultItem(any())).thenReturn(ItemStack.EMPTY);

        CustomizationEntry imported = success("test:keep_catalyst", recipe);
        RecipeImportResult craftTweaker = this.importer.importRecipe(
                holder("test:keep_catalyst", recipe), OutputTarget.CRAFTTWEAKER, RegistryAccess.EMPTY);

        assertThat(imported).isInstanceOfSatisfying(CustomizationEntry.NoConsumeCatalystShaped.class,
                shaped -> {
                    assertThat(shaped.tier()).isEqualTo(CraftingTier.SCULK);
                    assertThat(shaped.ingredients()).hasSize(2);
                });
        assertThat(craftTweaker).isInstanceOfSatisfying(RecipeImportResult.Failure.class,
                failure -> assertThat(failure.code()).isEqualTo("recipe.target.unsupported"));
    }

    @Test
    void importsRuntimeSingularityDefinitions() {
        Singularity singularity = new Singularity(id("avaritia:iron"), "singularity.avaritia.iron",
                0xd8d8d8, 0x8f8f8f, 1450, 360, stone(), true, false);

        RecipeImportResult result = this.importer.importSingularity(singularity, OutputTarget.DATAPACK);

        assertThat(result).isInstanceOfSatisfying(RecipeImportResult.Success.class, success ->
                assertThat(success.entry()).isInstanceOfSatisfying(
                        CustomizationEntry.SingularityDefinition.class, definition -> {
                            assertThat(definition.id()).isEqualTo(id("avaritia:iron"));
                            assertThat(definition.target()).isEqualTo(OutputTarget.DATAPACK);
                            assertThat(definition.count()).isEqualTo(1450);
                            assertThat(definition.timeCost()).isEqualTo(360);
                            assertThat(definition.recipeEnabled()).isFalse();
                        }));
    }

    @Test
    void preservesNeutronHorseArmorComponentAlternativeForKubeJs() {
        CompoundTag data = new CompoundTag();
        data.putString("potion", "minecraft:swiftness");
        Ingredient potion = DataComponentIngredient.of(false, DataComponents.CUSTOM_DATA,
                CustomData.of(data), Items.POTION);
        ExtremeSmithingRecipe recipe = new ExtremeSmithingRecipe(stone(), dirt(),
                CompoundIngredient.of(potion, stone(), dirt()), ItemStack.EMPTY);

        CustomizationEntry.ExtremeSmithing imported = (CustomizationEntry.ExtremeSmithing)
                success("avaritia:neutron_horse_armor", recipe);
        RecipeImportResult craftTweaker = this.importer.importRecipe(
                holder("avaritia:neutron_horse_armor", recipe),
                OutputTarget.CRAFTTWEAKER, RegistryAccess.EMPTY);

        assertThat(imported.addition()).isInstanceOfSatisfying(IngredientSpec.Choice.class,
                choice -> assertThat(choice.alternatives())
                        .anyMatch(IngredientSpec.Components.class::isInstance));
        assertThat(craftTweaker).isInstanceOfSatisfying(RecipeImportResult.Failure.class,
                failure -> assertThat(failure.code())
                        .isEqualTo("ingredient.components.target_unsupported"));
    }

    private CustomizationEntry success(String recipeId, Recipe<?> recipe) {
        assertThat(this.importer.supports(recipe)).isTrue();
        RecipeImportResult result = this.importer.importRecipe(
                holder(recipeId, recipe), OutputTarget.KUBEJS, RegistryAccess.EMPTY);
        assertThat(result).isInstanceOf(RecipeImportResult.Success.class);
        return ((RecipeImportResult.Success) result).entry();
    }

    private static InfinityCatalystCraftRecipe catalyst() {
        return mock(InfinityCatalystCraftRecipe.class);
    }

    private static EternalSingularityCraftRecipe eternal() {
        return mock(EternalSingularityCraftRecipe.class);
    }

    private static Ingredient stone() {
        return Ingredient.of(Items.STONE);
    }

    private static Ingredient dirt() {
        return Ingredient.of(Items.DIRT);
    }

    private static NonNullList<Ingredient> ingredients(Ingredient... values) {
        NonNullList<Ingredient> result = NonNullList.create();
        result.addAll(List.of(values));
        return result;
    }

    private static <T extends Recipe<?>> RecipeHolder<T> holder(String value, T recipe) {
        return new RecipeHolder<>(id(value), recipe);
    }

    private static ResourceLocation id(String value) {
        return Objects.requireNonNull(ResourceLocation.tryParse(value));
    }
}
