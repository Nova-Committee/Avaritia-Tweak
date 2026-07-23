package committee.nova.mods.avaritia_tweak.client.customization.screen;

import committee.nova.mods.avaritia_tweak.client.customization.EntryForm;
import committee.nova.mods.avaritia_tweak.customization.model.IngredientSpec;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

/** Pure local-form operations used by the table-grid context menu. */
final class TableGridOperations {
    private TableGridOperations() {
    }

    static int fillRow(EntryForm form, int slot, IngredientSpec ingredient) {
        int size = requireShapedSlot(form, slot);
        int rowStart = slot / size * size;
        int changed = 0;
        for (int column = 0; column < size; column++) {
            changed += setIfChanged(form, rowStart + column, ingredient);
        }
        return changed;
    }

    static int fillColumn(EntryForm form, int slot, IngredientSpec ingredient) {
        int size = requireShapedSlot(form, slot);
        int column = slot % size;
        int changed = 0;
        for (int row = 0; row < size; row++) {
            changed += setIfChanged(form, row * size + column, ingredient);
        }
        return changed;
    }

    static int clearRow(EntryForm form, int slot) {
        int size = requireShapedSlot(form, slot);
        int rowStart = slot / size * size;
        int changed = 0;
        for (int column = 0; column < size; column++) {
            int target = rowStart + column;
            if (form.grid().containsKey(target)) {
                form.gridIngredient(target, Optional.empty());
                changed++;
            }
        }
        return changed;
    }

    static int clearColumn(EntryForm form, int slot) {
        int size = requireShapedSlot(form, slot);
        int column = slot % size;
        int changed = 0;
        for (int row = 0; row < size; row++) {
            int target = row * size + column;
            if (form.grid().containsKey(target)) {
                form.gridIngredient(target, Optional.empty());
                changed++;
            }
        }
        return changed;
    }

    static int replaceAll(EntryForm form, IngredientSpec source, IngredientSpec replacement) {
        Objects.requireNonNull(form, "form");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(replacement, "replacement");
        if (!form.kind().usesTableGrid() || source.equals(replacement)) {
            return 0;
        }
        int changed = 0;
        if (form.kind().usesShapedGrid()) {
            for (Map.Entry<Integer, IngredientSpec> entry : new ArrayList<>(form.grid().entrySet())) {
                if (source.equals(entry.getValue())) {
                    form.gridIngredient(entry.getKey(), Optional.of(replacement));
                    changed++;
                }
            }
            return changed;
        }
        for (int index = 0; index < form.ingredients().size(); index++) {
            if (source.equals(form.ingredients().get(index))) {
                form.ingredientAt(index, replacement);
                changed++;
            }
        }
        return changed;
    }

    static SwapResult inspectSwap(EntryForm form, int sourceSlot, int targetSlot) {
        Objects.requireNonNull(form, "form");
        int capacity = form.tier().capacity();
        if (!form.kind().usesTableGrid() || sourceSlot < 0 || sourceSlot >= capacity
                || targetSlot < 0 || targetSlot >= capacity || sourceSlot == targetSlot) {
            return SwapResult.INVALID;
        }
        if (form.kind().usesShapedGrid()) {
            Map<Integer, IngredientSpec> grid = form.grid();
            IngredientSpec source = grid.get(sourceSlot);
            if (source == null) {
                return SwapResult.INVALID;
            }
            IngredientSpec target = grid.get(targetSlot);
            if (Objects.equals(source, target)) {
                return SwapResult.UNCHANGED;
            }
            return target == null ? SwapResult.MOVED : SwapResult.SWAPPED;
        }

        List<IngredientSpec> ingredients = form.ingredients();
        if (sourceSlot >= ingredients.size() || targetSlot >= ingredients.size()) {
            return SwapResult.INVALID;
        }
        return Objects.equals(ingredients.get(sourceSlot), ingredients.get(targetSlot))
                ? SwapResult.UNCHANGED : SwapResult.SWAPPED;
    }

    static SwapResult swap(EntryForm form, int sourceSlot, int targetSlot) {
        SwapResult result = inspectSwap(form, sourceSlot, targetSlot);
        if (!result.changed()) {
            return result;
        }
        if (form.kind().usesShapedGrid()) {
            Map<Integer, IngredientSpec> grid = form.grid();
            IngredientSpec source = grid.get(sourceSlot);
            IngredientSpec target = grid.get(targetSlot);
            form.gridIngredient(sourceSlot, Optional.ofNullable(target));
            form.gridIngredient(targetSlot, Optional.of(source));
            return result;
        }

        List<IngredientSpec> ingredients = form.ingredients();
        IngredientSpec source = ingredients.get(sourceSlot);
        IngredientSpec target = ingredients.get(targetSlot);
        form.ingredientAt(sourceSlot, target);
        form.ingredientAt(targetSlot, source);
        return result;
    }

    static int mirrorHorizontal(EntryForm form) {
        int size = requireShaped(form);
        return remap(form, size, (row, column) -> row * size + (size - 1 - column));
    }

    static int mirrorVertical(EntryForm form) {
        int size = requireShaped(form);
        return remap(form, size, (row, column) -> (size - 1 - row) * size + column);
    }

    static int rotateClockwise(EntryForm form) {
        int size = requireShaped(form);
        return remap(form, size, (row, column) -> column * size + (size - 1 - row));
    }

    private static int setIfChanged(EntryForm form, int slot, IngredientSpec ingredient) {
        if (ingredient.equals(form.grid().get(slot))) {
            return 0;
        }
        form.gridIngredient(slot, Optional.of(ingredient));
        return 1;
    }

    private static int remap(EntryForm form, int size, SlotMapping mapping) {
        TreeMap<Integer, IngredientSpec> before = new TreeMap<>(form.grid());
        TreeMap<Integer, IngredientSpec> after = new TreeMap<>();
        before.forEach((slot, ingredient) -> {
            int row = slot / size;
            int column = slot % size;
            after.put(mapping.map(row, column), ingredient);
        });
        int changed = 0;
        for (int slot = 0; slot < size * size; slot++) {
            if (!Objects.equals(before.get(slot), after.get(slot))) {
                changed++;
            }
        }
        if (changed == 0) {
            return 0;
        }
        for (Integer slot : before.keySet()) {
            form.gridIngredient(slot, Optional.empty());
        }
        after.forEach((slot, ingredient) -> form.gridIngredient(slot, Optional.of(ingredient)));
        return changed;
    }

    private static int requireShapedSlot(EntryForm form, int slot) {
        int size = requireShaped(form);
        if (slot < 0 || slot >= size * size) {
            throw new IllegalArgumentException("slot is outside the shaped grid");
        }
        return size;
    }

    private static int requireShaped(EntryForm form) {
        Objects.requireNonNull(form, "form");
        if (!form.kind().usesShapedGrid()) {
            throw new IllegalArgumentException("operation requires a shaped table recipe");
        }
        return form.tier().gridSize();
    }

    @FunctionalInterface
    private interface SlotMapping {
        int map(int row, int column);
    }

    enum SwapResult {
        SWAPPED(true),
        MOVED(true),
        UNCHANGED(false),
        INVALID(false);

        private final boolean changed;

        SwapResult(boolean changed) {
            this.changed = changed;
        }

        boolean changed() {
            return this.changed;
        }
    }
}
