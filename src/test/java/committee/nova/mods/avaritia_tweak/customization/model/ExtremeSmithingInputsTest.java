package committee.nova.mods.avaritia_tweak.customization.model;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExtremeSmithingInputsTest {
    private static final IngredientSpec STONE = item("minecraft:stone");
    private static final IngredientSpec DIRT = item("minecraft:dirt");
    private static final IngredientSpec DIAMOND = item("minecraft:diamond");

    @Test
    void validatesAndDefensivelyCopiesExactlyThreeAdditions() {
        ArrayList<IngredientSpec> source = new ArrayList<>(List.of(STONE, DIRT, DIAMOND));

        List<IngredientSpec> validated = ExtremeSmithingInputs.validateAdditions(source);
        source.set(0, DIAMOND);

        assertThat(validated).containsExactly(STONE, DIRT, DIAMOND);
        assertThatThrownBy(() -> ExtremeSmithingInputs.validateAdditions(List.of(STONE, DIRT)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exactly three");
    }

    @Test
    void expandsThreeAlternativesIntoPhysicalSlots() {
        IngredientSpec.Choice serialized = new IngredientSpec.Choice(List.of(STONE, DIRT, DIAMOND));

        assertThat(ExtremeSmithingInputs.expandSerializedAddition(serialized))
                .containsExactly(STONE, DIRT, DIAMOND);
    }

    @Test
    void repeatsAGenericSerializedMatcherAcrossPhysicalSlots() {
        assertThat(ExtremeSmithingInputs.expandSerializedAddition(STONE))
                .containsExactly(STONE, STONE, STONE);
    }

    @Test
    void collapsesEqualSlotsAndPreservesDistinctSlotsAsAChoice() {
        assertThat(ExtremeSmithingInputs.serializedAddition(List.of(STONE, STONE, STONE)))
                .isEqualTo(STONE);
        assertThat(ExtremeSmithingInputs.serializedAddition(List.of(STONE, DIRT, DIAMOND)))
                .isEqualTo(new IngredientSpec.Choice(List.of(STONE, DIRT, DIAMOND)));
    }

    private static IngredientSpec item(String id) {
        return new IngredientSpec.Item(ResourceLocation.tryParse(id));
    }
}
