package committee.nova.mods.avaritia_tweak.client.customization.screen;

import committee.nova.mods.avaritia_tweak.customization.model.CraftingTier;
import committee.nova.mods.avaritia_tweak.customization.model.CustomizationEntry;
import committee.nova.mods.avaritia_tweak.customization.model.EntryKind;
import committee.nova.mods.avaritia_tweak.customization.model.IngredientSpec;
import committee.nova.mods.avaritia_tweak.customization.model.ItemStackSpec;
import committee.nova.mods.avaritia_tweak.customization.model.OutputTarget;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RecipeInspectorDetailsTest {
    @Test
    void shapedRecipePreservesItsCompleteNineByNineSlotLayout() {
        IngredientSpec first = item("minecraft:stone");
        IngredientSpec last = tag("forge:storage_blocks/diamond");
        CustomizationEntry.ShapedTable entry = new CustomizationEntry.ShapedTable(
                id("test:extreme"), OutputTarget.KUBEJS, CraftingTier.EXTREME,
                new TreeMap<>(Map.of(0, first, 80, last)), result());

        RecipeInspectorDetails.Details details = RecipeInspectorDetails.from(entry);

        assertThat(details.columns()).isEqualTo(9);
        assertThat(details.rows()).isEqualTo(9);
        assertThat(details.slotCount()).isEqualTo(81);
        assertThat(details.cells()).containsOnlyKeys(0, 80);
        assertThat(details.cells().get(80).ingredient()).isEqualTo(last);
        assertThat(details.attributes()).containsExactly(
                new RecipeInspectorDetails.Attribute(
                        "gui.avaritia_tweak.recipe_import.tier_grid", "4 / 9x9"));
    }

    @Test
    void typeSpecificParametersAndIngredientRolesRemainVisible() {
        CustomizationEntry.Compressor compressor = new CustomizationEntry.Compressor(
                id("test:compressor"), OutputTarget.CRAFTTWEAKER, item("minecraft:iron_ingot"),
                result(), 800, 240);
        RecipeInspectorDetails.Details compressorDetails = RecipeInspectorDetails.from(compressor);

        assertThat(compressorDetails.cells().get(0).roleKey())
                .isEqualTo("gui.avaritia_tweak.input");
        assertThat(compressorDetails.attributes()).containsExactly(
                new RecipeInspectorDetails.Attribute("gui.avaritia_tweak.input_count", "800"),
                new RecipeInspectorDetails.Attribute("gui.avaritia_tweak.time_cost", "240"));

        CustomizationEntry.ExtremeSmithing smithing = new CustomizationEntry.ExtremeSmithing(
                id("test:smithing"), OutputTarget.KUBEJS, item("minecraft:netherite_upgrade_smithing_template"),
                item("minecraft:diamond_sword"), item("minecraft:nether_star"), result());
        RecipeInspectorDetails.Details smithingDetails = RecipeInspectorDetails.from(smithing);

        assertThat(smithingDetails.cells().values()).extracting(
                        RecipeInspectorDetails.IngredientCell::roleKey)
                .containsExactly("gui.avaritia_tweak.template", "gui.avaritia_tweak.base",
                        "gui.avaritia_tweak.addition");
    }

    @Test
    void orderedRecipeInputsKeepTheirImportedOrder() {
        IngredientSpec first = item("minecraft:iron_ingot");
        IngredientSpec second = tag("forge:gems/diamond");
        CustomizationEntry.ShapelessTable entry = new CustomizationEntry.ShapelessTable(
                id("test:ordered"), OutputTarget.KUBEJS, CraftingTier.EXTREME,
                List.of(first, second), result());

        RecipeInspectorDetails.Details details = RecipeInspectorDetails.from(entry);

        assertThat(details.cells().values())
                .extracting(RecipeInspectorDetails.IngredientCell::ingredient)
                .containsExactly(first, second);
    }

    @Test
    void inspectorRejectsCellsOutsideItsDeclaredGrid() {
        TreeMap<Integer, RecipeInspectorDetails.IngredientCell> cells = new TreeMap<>(Map.of(
                1, new RecipeInspectorDetails.IngredientCell(item("minecraft:stone"), "")));

        assertThatThrownBy(() -> new RecipeInspectorDetails.Details(
                EntryKind.COMPRESSOR, 1, 1, cells, List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("outside the grid: 1");
    }

    @Test
    void inspectorRejectsNonRecipeEntries() {
        CustomizationEntry.SingularityDefinition definition = new CustomizationEntry.SingularityDefinition(
                id("test:singularity"), OutputTarget.DATAPACK, "Test", 0xffffff, 0x000000,
                1, 1, item("minecraft:stone"), true, true);

        assertThatThrownBy(() -> RecipeInspectorDetails.from(definition))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SINGULARITY_DEFINITION");
    }

    private static IngredientSpec item(String value) {
        return new IngredientSpec.Item(id(value), Optional.empty());
    }

    private static IngredientSpec tag(String value) {
        return new IngredientSpec.Tag(id(value));
    }

    private static ItemStackSpec result() {
        return new ItemStackSpec(id("minecraft:diamond"), 2);
    }

    private static ResourceLocation id(String value) {
        return ResourceLocation.tryParse(value);
    }
}
