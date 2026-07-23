package committee.nova.mods.avaritia_tweak.client.customization.screen;

import committee.nova.mods.avaritia_tweak.customization.model.IngredientSpec;
import committee.nova.mods.avaritia_tweak.customization.model.ItemStackSpec;

import java.util.Objects;
import java.util.Optional;

/** Screen-local, typed clipboard for ghost recipe values. */
final class EditorClipboard {
    private Optional<IngredientSpec> ingredient = Optional.empty();
    private Optional<ItemStackSpec> itemStack = Optional.empty();

    Optional<IngredientSpec> ingredient() {
        return this.ingredient;
    }

    void copyIngredient(IngredientSpec ingredient) {
        this.ingredient = Optional.of(Objects.requireNonNull(ingredient, "ingredient"));
    }

    Optional<ItemStackSpec> itemStack() {
        return this.itemStack;
    }

    void copyItemStack(ItemStackSpec itemStack) {
        this.itemStack = Optional.of(Objects.requireNonNull(itemStack, "itemStack"));
    }
}
