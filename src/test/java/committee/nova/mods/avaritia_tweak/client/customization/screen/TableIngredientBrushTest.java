package committee.nova.mods.avaritia_tweak.client.customization.screen;

import committee.nova.mods.avaritia_tweak.client.customization.EntryForm;
import committee.nova.mods.avaritia_tweak.customization.model.CraftingTier;
import committee.nova.mods.avaritia_tweak.customization.model.EntryKind;
import committee.nova.mods.avaritia_tweak.customization.model.IngredientSpec;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class TableIngredientBrushTest {
    private static final IngredientSpec STONE = item("minecraft:stone");
    private static final IngredientSpec DIAMOND = item("minecraft:diamond");

    @Test
    void paintsAndFillsOnlyEmptyShapedSlots() {
        EntryForm form = EntryForm.newEntry(EntryKind.SHAPED_TABLE);
        form.tier(CraftingTier.SCULK);
        form.gridIngredient(0, Optional.of(DIAMOND));
        TableIngredientBrush brush = selected(STONE);

        assertThat(brush.apply(form, 4)).isTrue();
        assertThat(brush.apply(form, 4)).isFalse();
        assertThat(brush.fillEmpty(form)).isEqualTo(7);
        assertThat(form.grid()).hasSize(9);
        assertThat(form.grid().get(0)).isEqualTo(DIAMOND);
        assertThat(form.grid().get(8)).isEqualTo(STONE);
    }

    @Test
    void paintingLaterShapelessSlotFillsTheContiguousIngredientList() {
        EntryForm form = EntryForm.newEntry(EntryKind.SHAPELESS_TABLE);
        form.tier(CraftingTier.SCULK);
        TableIngredientBrush brush = selected(STONE);

        assertThat(brush.apply(form, 3)).isTrue();
        assertThat(form.ingredients()).containsExactly(STONE, STONE, STONE, STONE);
        assertThat(brush.fillEmpty(form)).isEqualTo(5);
        assertThat(form.ingredients()).hasSize(9);
    }

    @Test
    void inactiveBrushAndOutOfRangeSlotsDoNothing() {
        EntryForm form = EntryForm.newEntry(EntryKind.SHAPED_TABLE);
        form.tier(CraftingTier.SCULK);
        TableIngredientBrush brush = new TableIngredientBrush();

        assertThat(brush.apply(form, 0)).isFalse();
        brush.select(Optional.of(STONE));
        assertThat(brush.apply(form, 9)).isFalse();
        brush.clear();
        assertThat(brush.fillEmpty(form)).isZero();
        assertThat(form.grid()).isEmpty();
    }

    private static TableIngredientBrush selected(IngredientSpec ingredient) {
        TableIngredientBrush brush = new TableIngredientBrush();
        brush.select(Optional.of(ingredient));
        return brush;
    }

    private static IngredientSpec item(String id) {
        return new IngredientSpec.Item(ResourceLocation.tryParse(id));
    }
}
