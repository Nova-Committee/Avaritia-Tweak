package committee.nova.mods.avaritia_tweak.client.customization.importers;

import com.google.gson.JsonParser;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.crafting.CompoundIngredient;
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
                        InfinityCatalystCraftRecipe recipe) {
                    return new AvaritiaRecipeImporter.DecodedSpecial(
                            "custom", List.of(item(), tag()), 3);
                }

                @Override
                public AvaritiaRecipeImporter.DecodedSpecial eternal(
                        EternalSingularityCraftRecipe recipe) {
                    return new AvaritiaRecipeImporter.DecodedSpecial("", List.of(item()), 4);
                }
            });

    @Test
    void importsAllSixAvaritiaRecipeKindsAndTheirSpecificFields() {
        ShapedTableCraftingRecipe shaped = new ShapedTableCraftingRecipe(
                id("test:shaped"), 2, 1, ingredients(item(), tag()),
                null, 2, false);
        ShapelessTableCraftingRecipe shapeless = new ShapelessTableCraftingRecipe(
                id("test:shapeless"), ingredients(item(), tag()), null, 3);
        CompressorRecipe compressor = new CompressorRecipe(
                id("test:compressor"), item(), null, 800, 120);
        ExtremeSmithingRecipe smithing = new ExtremeSmithingRecipe(
                id("test:smithing"), item(), tag(),
                CompoundIngredient.of(item(), ingredient("{\"item\":\"minecraft:dirt\"}")), null);

        CustomizationEntry.ShapedTable importedShaped = (CustomizationEntry.ShapedTable)
                success(shaped);
        CustomizationEntry.ShapelessTable importedShapeless = (CustomizationEntry.ShapelessTable)
                success(shapeless);
        CustomizationEntry.Compressor importedCompressor = (CustomizationEntry.Compressor)
                success(compressor);
        CustomizationEntry.ExtremeSmithing importedSmithing = (CustomizationEntry.ExtremeSmithing)
                success(smithing);
        CustomizationEntry.InfinityCatalyst importedCatalyst =
                (CustomizationEntry.InfinityCatalyst) success(catalyst());
        CustomizationEntry.EternalSingularity importedEternal =
                (CustomizationEntry.EternalSingularity) success(eternal());

        assertThat(importedShaped.tier()).isEqualTo(CraftingTier.NETHER);
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
        CompressorRecipe recipe = new CompressorRecipe(
                id("test:compressor"), item(), null, 1, 1);

        assertThat(this.importer.importRecipe(recipe, OutputTarget.DATAPACK, RegistryAccess.EMPTY))
                .isInstanceOfSatisfying(RecipeImportResult.Failure.class,
                        failure -> assertThat(failure.code())
                                .isEqualTo("recipe.target.unsupported"));
    }

    @Test
    void preservesNoConsumeRecipeIdentityAndRejectsCraftTweakerTarget() {
        NoConsumeCatalystShapedRecipe recipe = mock(NoConsumeCatalystShapedRecipe.class);
        NonNullList<Ingredient> recipeIngredients = ingredients(item(), tag());
        when(recipe.getId()).thenReturn(id("test:keep_catalyst"));
        when(recipe.getTier()).thenReturn(1);
        when(recipe.getWidth()).thenReturn(2);
        when(recipe.getHeight()).thenReturn(1);
        when(recipe.getIngredients()).thenReturn(recipeIngredients);
        when(recipe.getResultItem(any())).thenReturn(null);

        CustomizationEntry imported = success(recipe);
        RecipeImportResult craftTweaker = this.importer.importRecipe(
                recipe, OutputTarget.CRAFTTWEAKER, RegistryAccess.EMPTY);

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
                0xd8d8d8, 0x8f8f8f, 1450, 360, item(), true, false);

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
    void importsNeutronHorseArmorAlternatives() {
        ExtremeSmithingRecipe recipe = new ExtremeSmithingRecipe(id("avaritia:neutron_horse_armor"),
                item(), tag(), CompoundIngredient.of(
                ingredient("{\"item\":\"minecraft:potion\"}"),
                ingredient("{\"item\":\"avaritia:enhancement_core\"}"),
                ingredient("{\"item\":\"minecraft:blue_ice\"}")), null);

        CustomizationEntry.ExtremeSmithing imported = (CustomizationEntry.ExtremeSmithing) success(recipe);

        assertThat(imported.addition()).isInstanceOfSatisfying(IngredientSpec.Choice.class,
                choice -> assertThat(choice.alternatives()).extracting(Object::toString)
                        .anyMatch(value -> value.contains("minecraft:potion"))
                        .anyMatch(value -> value.contains("avaritia:enhancement_core"))
                        .anyMatch(value -> value.contains("minecraft:blue_ice")));
    }

    private CustomizationEntry success(net.minecraft.world.item.crafting.Recipe<?> recipe) {
        assertThat(this.importer.supports(recipe)).isTrue();
        RecipeImportResult result = this.importer.importRecipe(
                recipe, OutputTarget.KUBEJS, RegistryAccess.EMPTY);
        assertThat(result).isInstanceOf(RecipeImportResult.Success.class);
        return ((RecipeImportResult.Success) result).entry();
    }

    private static InfinityCatalystCraftRecipe catalyst() {
        InfinityCatalystCraftRecipe recipe = mock(InfinityCatalystCraftRecipe.class);
        when(recipe.getId()).thenReturn(id("test:catalyst"));
        return recipe;
    }

    private static EternalSingularityCraftRecipe eternal() {
        EternalSingularityCraftRecipe recipe = mock(EternalSingularityCraftRecipe.class);
        when(recipe.getId()).thenReturn(id("test:eternal"));
        return recipe;
    }

    private static Ingredient item() {
        return ingredient("{\"item\":\"minecraft:stone\"}");
    }

    private static Ingredient tag() {
        return ingredient("{\"tag\":\"forge:ingots/iron\"}");
    }

    private static Ingredient ingredient(String json) {
        Ingredient ingredient = mock(Ingredient.class);
        when(ingredient.toJson()).thenReturn(JsonParser.parseString(json));
        return ingredient;
    }

    private static NonNullList<Ingredient> ingredients(Ingredient... values) {
        NonNullList<Ingredient> result = NonNullList.create();
        result.addAll(List.of(values));
        return result;
    }

    private static ResourceLocation id(String value) {
        return Objects.requireNonNull(ResourceLocation.tryParse(value));
    }
}
