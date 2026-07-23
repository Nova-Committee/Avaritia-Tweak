package committee.nova.mods.avaritia_tweak.client.customization.importers;

import committee.nova.mods.avaritia.common.crafting.recipe.CompressorRecipe;
import committee.nova.mods.avaritia.common.crafting.recipe.EternalSingularityCraftRecipe;
import committee.nova.mods.avaritia.common.crafting.recipe.ExtremeSmithingRecipe;
import committee.nova.mods.avaritia.common.crafting.recipe.InfinityCatalystCraftRecipe;
import committee.nova.mods.avaritia.common.crafting.recipe.ShapedTableCraftingRecipe;
import committee.nova.mods.avaritia.common.crafting.recipe.ShapelessTableCraftingRecipe;
import committee.nova.mods.avaritia_tweak.customization.model.CraftingTier;
import committee.nova.mods.avaritia_tweak.customization.model.CustomizationEntry;
import committee.nova.mods.avaritia_tweak.customization.model.ItemStackSpec;
import committee.nova.mods.avaritia_tweak.customization.model.OutputTarget;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
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
                stone(), dirt(), stone(), ItemStack.EMPTY);

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
        assertThat(importedSmithing.addition()).isNotNull();
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
