package committee.nova.mods.avaritia_tweak.client.customization.screen;

import committee.nova.mods.avaritia_tweak.client.customization.EntryForm;
import committee.nova.mods.avaritia_tweak.customization.model.CraftingTier;
import committee.nova.mods.avaritia_tweak.customization.model.EntryKind;
import committee.nova.mods.avaritia_tweak.customization.model.IngredientSpec;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TableGridOperationsTest {
    private static final IngredientSpec STONE = item("minecraft:stone");
    private static final IngredientSpec DIAMOND = item("minecraft:diamond");
    private static final IngredientSpec GOLD = item("minecraft:gold_ingot");

    @Test
    void fillsAndClearsTheSelectedRowAndColumn() {
        EntryForm form = shaped();

        assertThat(TableGridOperations.fillRow(form, 4, STONE)).isEqualTo(3);
        assertThat(form.grid()).containsOnlyKeys(3, 4, 5);
        assertThat(TableGridOperations.fillColumn(form, 4, DIAMOND)).isEqualTo(3);
        assertThat(form.grid().get(1)).isEqualTo(DIAMOND);
        assertThat(form.grid().get(4)).isEqualTo(DIAMOND);
        assertThat(form.grid().get(7)).isEqualTo(DIAMOND);

        assertThat(TableGridOperations.clearRow(form, 4)).isEqualTo(3);
        assertThat(form.grid()).containsOnlyKeys(1, 7);
        assertThat(TableGridOperations.clearColumn(form, 1)).isEqualTo(2);
        assertThat(form.grid()).isEmpty();
    }

    @Test
    void replacesMatchingInputsForShapedAndShapelessRecipes() {
        EntryForm shaped = shaped();
        shaped.gridIngredient(0, Optional.of(STONE));
        shaped.gridIngredient(1, Optional.of(DIAMOND));
        shaped.gridIngredient(8, Optional.of(STONE));

        assertThat(TableGridOperations.replaceAll(shaped, STONE, GOLD)).isEqualTo(2);
        assertThat(shaped.grid().get(0)).isEqualTo(GOLD);
        assertThat(shaped.grid().get(1)).isEqualTo(DIAMOND);
        assertThat(shaped.grid().get(8)).isEqualTo(GOLD);

        EntryForm shapeless = EntryForm.newEntry(EntryKind.SHAPELESS_TABLE);
        shapeless.tier(CraftingTier.SCULK);
        shapeless.addIngredient(STONE);
        shapeless.addIngredient(DIAMOND);
        shapeless.addIngredient(STONE);
        assertThat(TableGridOperations.replaceAll(shapeless, STONE, GOLD)).isEqualTo(2);
        assertThat(shapeless.ingredients()).containsExactly(GOLD, DIAMOND, GOLD);
    }

    @Test
    void swapsOrMovesPositionedIngredients() {
        EntryForm form = shaped();
        form.gridIngredient(0, Optional.of(STONE));
        form.gridIngredient(1, Optional.of(DIAMOND));

        assertThat(TableGridOperations.inspectSwap(form, 0, 1))
                .isEqualTo(TableGridOperations.SwapResult.SWAPPED);
        assertThat(TableGridOperations.swap(form, 0, 1))
                .isEqualTo(TableGridOperations.SwapResult.SWAPPED);
        assertThat(form.grid()).containsEntry(0, DIAMOND).containsEntry(1, STONE);

        assertThat(TableGridOperations.swap(form, 1, 8))
                .isEqualTo(TableGridOperations.SwapResult.MOVED);
        assertThat(form.grid()).doesNotContainKey(1).containsEntry(8, STONE);
    }

    @Test
    void shapelessSwapRequiresTwoExistingDifferentInputs() {
        EntryForm form = EntryForm.newEntry(EntryKind.SHAPELESS_TABLE);
        form.tier(CraftingTier.SCULK);
        form.addIngredient(STONE);
        form.addIngredient(DIAMOND);
        form.addIngredient(STONE);

        assertThat(TableGridOperations.swap(form, 0, 1))
                .isEqualTo(TableGridOperations.SwapResult.SWAPPED);
        assertThat(form.ingredients()).containsExactly(DIAMOND, STONE, STONE);
        assertThat(TableGridOperations.swap(form, 1, 2))
                .isEqualTo(TableGridOperations.SwapResult.UNCHANGED);
        assertThat(TableGridOperations.swap(form, 0, 3))
                .isEqualTo(TableGridOperations.SwapResult.INVALID);
        assertThat(form.ingredients()).containsExactly(DIAMOND, STONE, STONE);
    }

    @Test
    void invalidSwapRequestsLeaveEveryInputUntouched() {
        EntryForm shaped = shaped();
        shaped.gridIngredient(0, Optional.of(STONE));

        assertThat(TableGridOperations.swap(shaped, 1, 2))
                .isEqualTo(TableGridOperations.SwapResult.INVALID);
        assertThat(TableGridOperations.swap(shaped, 0, 0))
                .isEqualTo(TableGridOperations.SwapResult.INVALID);
        assertThat(TableGridOperations.swap(shaped, 0, 9))
                .isEqualTo(TableGridOperations.SwapResult.INVALID);
        assertThat(shaped.grid()).containsOnlyKeys(0).containsEntry(0, STONE);

        EntryForm compressor = EntryForm.newEntry(EntryKind.COMPRESSOR);
        assertThat(TableGridOperations.swap(compressor, 0, 1))
                .isEqualTo(TableGridOperations.SwapResult.INVALID);
    }

    @Test
    void mirrorsAndRotatesSparseShapedGrids() {
        EntryForm form = shaped();
        form.gridIngredient(0, Optional.of(STONE));
        form.gridIngredient(5, Optional.of(DIAMOND));

        assertThat(TableGridOperations.mirrorHorizontal(form)).isEqualTo(4);
        assertThat(form.grid()).containsEntry(2, STONE).containsEntry(3, DIAMOND);
        assertThat(TableGridOperations.mirrorVertical(form)).isEqualTo(2);
        assertThat(form.grid()).containsEntry(8, STONE).containsEntry(3, DIAMOND);
        assertThat(TableGridOperations.rotateClockwise(form)).isEqualTo(4);
        assertThat(form.grid()).containsEntry(6, STONE).containsEntry(1, DIAMOND);
    }

    @Test
    void positionedOperationsRejectShapelessRecipesAndInvalidSlots() {
        EntryForm shapeless = EntryForm.newEntry(EntryKind.SHAPELESS_TABLE);
        shapeless.tier(CraftingTier.SCULK);

        assertThatThrownBy(() -> TableGridOperations.fillRow(shapeless, 0, STONE))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TableGridOperations.rotateClockwise(shapeless))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TableGridOperations.clearColumn(shaped(), 9))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void catalystPreservingRecipeSupportsTheSamePositionedOperations() {
        EntryForm form = EntryForm.newEntry(EntryKind.NO_CONSUME_CATALYST_SHAPED);
        form.tier(CraftingTier.SCULK);

        assertThat(TableGridOperations.fillRow(form, 0, STONE)).isEqualTo(3);
        assertThat(TableGridOperations.swap(form, 0, 8))
                .isEqualTo(TableGridOperations.SwapResult.MOVED);
        assertThat(TableGridOperations.rotateClockwise(form)).isEqualTo(4);
        assertThat(form.grid()).containsOnlyKeys(5, 6, 8);
    }

    private static EntryForm shaped() {
        EntryForm form = EntryForm.newEntry(EntryKind.SHAPED_TABLE);
        form.tier(CraftingTier.SCULK);
        return form;
    }

    private static IngredientSpec item(String value) {
        return new IngredientSpec.Item(ResourceLocation.tryParse(value));
    }
}
