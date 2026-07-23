package committee.nova.mods.avaritia_tweak.customization.model;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ModelImmutabilityTest {
    @Test
    void copiesMutableNbtAndCollections() {
        CompoundTag sourceNbt = new CompoundTag();
        sourceNbt.putString("value", "original");
        IngredientSpec.Item ingredient = new IngredientSpec.Item(id("minecraft:stone"), Optional.of(sourceNbt));
        sourceNbt.putString("value", "changed");

        CompoundTag returnedNbt = ingredient.strictNbt().orElseThrow();
        returnedNbt.putString("value", "returned-change");
        assertThat(ingredient.strictNbt().orElseThrow().getString("value")).isEqualTo("original");

        List<IngredientSpec> ingredients = new ArrayList<>(List.of(ingredient));
        CustomizationEntry.ShapelessTable recipe = new CustomizationEntry.ShapelessTable(
                id("test:recipe"), OutputTarget.KUBEJS, CraftingTier.SCULK,
                ingredients, new ItemStackSpec(id("minecraft:diamond"), 1));
        ingredients.clear();
        assertThat(recipe.ingredients()).hasSize(1);
        assertThatThrownBy(() -> recipe.ingredients().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void copiesAndFlattensChoiceAlternatives() {
        IngredientSpec stone = new IngredientSpec.Item(id("minecraft:stone"));
        IngredientSpec dirt = new IngredientSpec.Item(id("minecraft:dirt"));
        IngredientSpec diamond = new IngredientSpec.Item(id("minecraft:diamond"));
        List<IngredientSpec> source = new ArrayList<>(List.of(stone, dirt));

        IngredientSpec.Choice choice = new IngredientSpec.Choice(source);
        source.clear();
        IngredientSpec.Choice flattened = new IngredientSpec.Choice(List.of(choice, diamond));

        assertThat(choice.alternatives()).containsExactly(stone, dirt);
        assertThat(flattened.alternatives()).containsExactly(stone, dirt, diamond);
        assertThatThrownBy(() -> choice.alternatives().clear())
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> new IngredientSpec.Choice(List.of(stone)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least two");
    }

    @Test
    void rejectsWorkspaceMapKeysThatDoNotMatchEntries() {
        CustomizationEntry recipe = new CustomizationEntry.ShapelessTable(
                id("test:recipe"), OutputTarget.KUBEJS, CraftingTier.SCULK,
                List.of(new IngredientSpec.Item(id("minecraft:stone"))),
                new ItemStackSpec(id("minecraft:diamond"), 1));
        TreeMap<EntryKey, CustomizationEntry> entries = new TreeMap<>();
        entries.put(new EntryKey(EntryKind.COMPRESSOR, recipe.id()), recipe);

        assertThatThrownBy(() -> new WorkspaceSnapshot(WorkspaceSnapshot.CURRENT_SCHEMA_VERSION, entries))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not match");
    }

    private static ResourceLocation id(String value) {
        return ResourceLocation.tryParse(value);
    }
}
