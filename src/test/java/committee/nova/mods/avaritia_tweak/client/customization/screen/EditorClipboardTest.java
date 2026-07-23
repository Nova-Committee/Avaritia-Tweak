package committee.nova.mods.avaritia_tweak.client.customization.screen;

import committee.nova.mods.avaritia_tweak.customization.model.IngredientSpec;
import committee.nova.mods.avaritia_tweak.customization.model.ItemStackSpec;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EditorClipboardTest {
    @Test
    void keepsIngredientAndOutputValuesInIndependentTypedSlots() {
        EditorClipboard clipboard = new EditorClipboard();
        IngredientSpec ingredient = new IngredientSpec.Item(id("minecraft:stone"));
        ItemStackSpec output = new ItemStackSpec(id("minecraft:diamond"), 4);

        clipboard.copyIngredient(ingredient);
        assertThat(clipboard.ingredient()).contains(ingredient);
        assertThat(clipboard.itemStack()).isEmpty();

        clipboard.copyItemStack(output);
        assertThat(clipboard.ingredient()).contains(ingredient);
        assertThat(clipboard.itemStack()).contains(output);
    }

    private static ResourceLocation id(String value) {
        return ResourceLocation.tryParse(value);
    }
}
