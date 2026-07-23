package committee.nova.mods.avaritia_tweak.client.customization.screen;

import committee.nova.mods.avaritia_tweak.client.customization.EntryForm;
import committee.nova.mods.avaritia_tweak.customization.model.EntryKind;
import committee.nova.mods.avaritia_tweak.customization.model.IngredientSpec;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Applies one selected ingredient to table-recipe ghost slots without touching a real inventory. */
final class TableIngredientBrush {
    private Optional<IngredientSpec> selection = Optional.empty();

    Optional<IngredientSpec> selection() {
        return this.selection;
    }

    boolean active() {
        return this.selection.isPresent();
    }

    void select(Optional<IngredientSpec> selection) {
        this.selection = Objects.requireNonNull(selection, "selection");
    }

    void clear() {
        this.selection = Optional.empty();
    }

    boolean apply(EntryForm form, int slot) {
        IngredientSpec ingredient = this.selection.orElse(null);
        if (ingredient == null || slot < 0 || slot >= form.tier().capacity()) {
            return false;
        }
        if (form.kind() == EntryKind.SHAPED_TABLE) {
            if (ingredient.equals(form.grid().get(slot))) {
                return false;
            }
            form.gridIngredient(slot, Optional.of(ingredient));
            return true;
        }
        if (form.kind() != EntryKind.SHAPELESS_TABLE) {
            return false;
        }

        List<IngredientSpec> ingredients = form.ingredients();
        if (slot < ingredients.size()) {
            if (ingredient.equals(ingredients.get(slot))) {
                return false;
            }
            form.ingredientAt(slot, ingredient);
            return true;
        }
        for (int index = ingredients.size(); index <= slot; index++) {
            form.addIngredient(ingredient);
        }
        return true;
    }

    int fillEmpty(EntryForm form) {
        IngredientSpec ingredient = this.selection.orElse(null);
        if (ingredient == null) {
            return 0;
        }
        if (form.kind() == EntryKind.SHAPED_TABLE) {
            int changed = 0;
            for (int slot = 0; slot < form.tier().capacity(); slot++) {
                if (!form.grid().containsKey(slot)) {
                    form.gridIngredient(slot, Optional.of(ingredient));
                    changed++;
                }
            }
            return changed;
        }
        if (form.kind() != EntryKind.SHAPELESS_TABLE) {
            return 0;
        }

        int changed = form.tier().capacity() - form.ingredients().size();
        for (int index = 0; index < changed; index++) {
            form.addIngredient(ingredient);
        }
        return changed;
    }
}
