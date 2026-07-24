package committee.nova.mods.avaritia_tweak.client.customization.screen;

import committee.nova.mods.avaritia_tweak.customization.model.CustomizationEntry;
import committee.nova.mods.avaritia_tweak.customization.model.EntryKind;
import committee.nova.mods.avaritia_tweak.customization.model.IngredientSpec;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;

/** Converts an imported recipe or singularity into a read-only inspector projection. */
final class RecipeInspectorDetails {
    private static final String TIER_GRID = "gui.avaritia_tweak.recipe_import.tier_grid";
    private static final String INPUT = "gui.avaritia_tweak.input";
    private static final String INPUT_COUNT = "gui.avaritia_tweak.input_count";
    private static final String TIME_COST = "gui.avaritia_tweak.time_cost";
    private static final String GROUP = "gui.avaritia_tweak.group";
    private static final String COUNT = "gui.avaritia_tweak.count";

    private RecipeInspectorDetails() {
    }

    static Details from(CustomizationEntry entry) {
        Objects.requireNonNull(entry, "entry");
        if (entry instanceof CustomizationEntry.ShapedTable shaped) {
            return shaped(shaped.kind(), shaped.tier().value(), shaped.tier().gridSize(),
                    shaped.ingredients());
        }
        if (entry instanceof CustomizationEntry.NoConsumeCatalystShaped shaped) {
            return shaped(shaped.kind(), shaped.tier().value(), shaped.tier().gridSize(),
                    shaped.ingredients());
        }
        if (entry instanceof CustomizationEntry.ShapelessTable shapeless) {
            return list(shapeless.kind(), shapeless.ingredients(), shapeless.tier().gridSize(),
                    List.of(tierGrid(shapeless.tier().value(), shapeless.tier().gridSize())));
        }
        if (entry instanceof CustomizationEntry.Compressor compressor) {
            return new Details(compressor.kind(), 1, 1,
                    cells(new IngredientCell(compressor.ingredient(), INPUT)),
                    List.of(new Attribute(INPUT_COUNT, Integer.toString(compressor.inputCount())),
                            new Attribute(TIME_COST, Integer.toString(compressor.timeCost()))));
        }
        if (entry instanceof CustomizationEntry.ExtremeSmithing smithing) {
            return new Details(smithing.kind(), 3, 3,
                    cells(new IngredientCell(smithing.template(), "gui.avaritia_tweak.template"),
                            new IngredientCell(smithing.base(), "gui.avaritia_tweak.base"),
                            new IngredientCell(smithing.addition(), "gui.avaritia_tweak.addition")),
                    List.of());
        }
        if (entry instanceof CustomizationEntry.InfinityCatalyst catalyst) {
            return list(catalyst.kind(), catalyst.ingredients(), 9,
                    List.of(new Attribute(GROUP, catalyst.group()),
                            new Attribute(COUNT, Integer.toString(catalyst.count()))));
        }
        if (entry instanceof CustomizationEntry.EternalSingularity eternal) {
            return list(eternal.kind(), eternal.ingredients(), 9,
                    List.of(new Attribute(COUNT, Integer.toString(eternal.count()))));
        }
        if (entry instanceof CustomizationEntry.SingularityDefinition singularity) {
            return new Details(singularity.kind(), 1, 1,
                    cells(new IngredientCell(singularity.ingredient(), INPUT)),
                    List.of(new Attribute(COUNT, Integer.toString(singularity.count())),
                            new Attribute(TIME_COST, Integer.toString(singularity.timeCost()))));
        }
        throw new IllegalArgumentException("Recipe inspector does not support " + entry.kind());
    }

    private static Details list(EntryKind kind, List<IngredientSpec> ingredients,
                                int maximumColumns, List<Attribute> attributes) {
        TreeMap<Integer, IngredientCell> cells = new TreeMap<>();
        for (int index = 0; index < ingredients.size(); index++) {
            cells.put(index, new IngredientCell(ingredients.get(index), ""));
        }
        int slotCount = Math.max(1, ingredients.size());
        int columns = Math.max(1, Math.min(maximumColumns, slotCount));
        return new Details(kind, columns, slotCount, cells, attributes);
    }

    private static Details shaped(EntryKind kind, int tier, int gridSize,
                                  SortedMap<Integer, IngredientSpec> ingredients) {
        TreeMap<Integer, IngredientCell> cells = new TreeMap<>();
        ingredients.forEach((slot, ingredient) -> cells.put(slot, new IngredientCell(ingredient, "")));
        return new Details(kind, gridSize, gridSize * gridSize, cells,
                List.of(tierGrid(tier, gridSize)));
    }

    private static Attribute tierGrid(int tier, int gridSize) {
        return new Attribute(TIER_GRID, tier + " / " + gridSize + "x" + gridSize);
    }

    private static SortedMap<Integer, IngredientCell> cells(IngredientCell... values) {
        TreeMap<Integer, IngredientCell> cells = new TreeMap<>();
        for (int index = 0; index < values.length; index++) {
            cells.put(index, values[index]);
        }
        return cells;
    }

    record Details(EntryKind kind, int columns, int slotCount,
                   SortedMap<Integer, IngredientCell> cells, List<Attribute> attributes) {
        Details {
            Objects.requireNonNull(kind, "kind");
            if (columns < 1 || slotCount < 1) {
                throw new IllegalArgumentException("Inspector grid dimensions must be positive");
            }
            TreeMap<Integer, IngredientCell> cellCopy = new TreeMap<>();
            Objects.requireNonNull(cells, "cells").forEach((slot, cell) -> {
                Objects.requireNonNull(slot, "slot");
                if (slot < 0 || slot >= slotCount) {
                    throw new IllegalArgumentException("Inspector slot is outside the grid: " + slot);
                }
                cellCopy.put(slot, Objects.requireNonNull(cell, "cell"));
            });
            cells = Collections.unmodifiableSortedMap(cellCopy);
            attributes = List.copyOf(Objects.requireNonNull(attributes, "attributes"));
        }

        int rows() {
            return (this.slotCount + this.columns - 1) / this.columns;
        }
    }

    record IngredientCell(IngredientSpec ingredient, String roleKey) {
        IngredientCell {
            Objects.requireNonNull(ingredient, "ingredient");
            Objects.requireNonNull(roleKey, "roleKey");
        }
    }

    record Attribute(String labelKey, String value) {
        Attribute {
            Objects.requireNonNull(labelKey, "labelKey");
            Objects.requireNonNull(value, "value");
        }
    }
}
