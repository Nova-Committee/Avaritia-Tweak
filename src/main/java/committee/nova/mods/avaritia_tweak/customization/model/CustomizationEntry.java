package committee.nova.mods.avaritia_tweak.customization.model;

import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;

public sealed interface CustomizationEntry permits CustomizationEntry.ShapedTable,
        CustomizationEntry.NoConsumeCatalystShaped, CustomizationEntry.ShapelessTable,
        CustomizationEntry.Compressor,
        CustomizationEntry.ExtremeSmithing, CustomizationEntry.InfinityCatalyst,
        CustomizationEntry.EternalSingularity, CustomizationEntry.SingularityDefinition,
        CustomizationEntry.SingularityOperation {

    ResourceLocation id();

    OutputTarget target();

    String note();

    EntryKind kind();

    default EntryKey key() {
        return new EntryKey(this.kind(), this.id());
    }

    record ShapedTable(ResourceLocation id, OutputTarget target, String note, CraftingTier tier,
                       SortedMap<Integer, IngredientSpec> ingredients,
                       ItemStackSpec result) implements CustomizationEntry {
        public ShapedTable(ResourceLocation id, OutputTarget target, CraftingTier tier,
                           SortedMap<Integer, IngredientSpec> ingredients, ItemStackSpec result) {
            this(id, target, "", tier, ingredients, result);
        }

        public ShapedTable {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(target, "target");
            Objects.requireNonNull(note, "note");
            Objects.requireNonNull(tier, "tier");
            ingredients = immutableGrid(ingredients);
            Objects.requireNonNull(result, "result");
        }

        @Override
        public EntryKind kind() {
            return EntryKind.SHAPED_TABLE;
        }
    }

    record NoConsumeCatalystShaped(ResourceLocation id, OutputTarget target, String note, CraftingTier tier,
                                   SortedMap<Integer, IngredientSpec> ingredients,
                                   ItemStackSpec result) implements CustomizationEntry {
        public NoConsumeCatalystShaped(ResourceLocation id, OutputTarget target, CraftingTier tier,
                                       SortedMap<Integer, IngredientSpec> ingredients, ItemStackSpec result) {
            this(id, target, "", tier, ingredients, result);
        }

        public NoConsumeCatalystShaped {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(target, "target");
            Objects.requireNonNull(note, "note");
            Objects.requireNonNull(tier, "tier");
            ingredients = immutableGrid(ingredients);
            Objects.requireNonNull(result, "result");
        }

        @Override
        public EntryKind kind() {
            return EntryKind.NO_CONSUME_CATALYST_SHAPED;
        }
    }

    record ShapelessTable(ResourceLocation id, OutputTarget target, String note, CraftingTier tier,
                          List<IngredientSpec> ingredients,
                          ItemStackSpec result) implements CustomizationEntry {
        public ShapelessTable(ResourceLocation id, OutputTarget target, CraftingTier tier,
                              List<IngredientSpec> ingredients, ItemStackSpec result) {
            this(id, target, "", tier, ingredients, result);
        }

        public ShapelessTable {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(target, "target");
            Objects.requireNonNull(note, "note");
            Objects.requireNonNull(tier, "tier");
            ingredients = immutableList(ingredients, "ingredients");
            Objects.requireNonNull(result, "result");
        }

        @Override
        public EntryKind kind() {
            return EntryKind.SHAPELESS_TABLE;
        }
    }

    record Compressor(ResourceLocation id, OutputTarget target, String note, IngredientSpec ingredient,
                      ItemStackSpec result, int inputCount, int timeCost) implements CustomizationEntry {
        public Compressor(ResourceLocation id, OutputTarget target, IngredientSpec ingredient,
                          ItemStackSpec result, int inputCount, int timeCost) {
            this(id, target, "", ingredient, result, inputCount, timeCost);
        }

        public Compressor {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(target, "target");
            Objects.requireNonNull(note, "note");
            Objects.requireNonNull(ingredient, "ingredient");
            Objects.requireNonNull(result, "result");
        }

        @Override
        public EntryKind kind() {
            return EntryKind.COMPRESSOR;
        }
    }

    record ExtremeSmithing(ResourceLocation id, OutputTarget target, String note, IngredientSpec template,
                           IngredientSpec base, IngredientSpec addition,
                           ItemStackSpec result) implements CustomizationEntry {
        public ExtremeSmithing(ResourceLocation id, OutputTarget target, IngredientSpec template,
                               IngredientSpec base, IngredientSpec addition, ItemStackSpec result) {
            this(id, target, "", template, base, addition, result);
        }

        public ExtremeSmithing {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(target, "target");
            Objects.requireNonNull(note, "note");
            Objects.requireNonNull(template, "template");
            Objects.requireNonNull(base, "base");
            Objects.requireNonNull(addition, "addition");
            Objects.requireNonNull(result, "result");
        }

        @Override
        public EntryKind kind() {
            return EntryKind.EXTREME_SMITHING;
        }
    }

    record InfinityCatalyst(ResourceLocation id, OutputTarget target, String note, String group,
                            List<IngredientSpec> ingredients, int count) implements CustomizationEntry {
        public InfinityCatalyst(ResourceLocation id, OutputTarget target, String group,
                                List<IngredientSpec> ingredients, int count) {
            this(id, target, "", group, ingredients, count);
        }

        public InfinityCatalyst {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(target, "target");
            Objects.requireNonNull(note, "note");
            Objects.requireNonNull(group, "group");
            ingredients = immutableList(ingredients, "ingredients");
        }

        @Override
        public EntryKind kind() {
            return EntryKind.INFINITY_CATALYST;
        }
    }

    record EternalSingularity(ResourceLocation id, OutputTarget target, String note,
                              List<IngredientSpec> ingredients, int count) implements CustomizationEntry {
        public EternalSingularity(ResourceLocation id, OutputTarget target,
                                  List<IngredientSpec> ingredients, int count) {
            this(id, target, "", ingredients, count);
        }

        public EternalSingularity {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(target, "target");
            Objects.requireNonNull(note, "note");
            ingredients = immutableList(ingredients, "ingredients");
        }

        @Override
        public EntryKind kind() {
            return EntryKind.ETERNAL_SINGULARITY;
        }
    }

    record SingularityDefinition(ResourceLocation id, OutputTarget target, String note, String displayName,
                                 int overlayColor, int underlayColor, int count, int timeCost,
                                 IngredientSpec ingredient, boolean enabled,
                                 boolean recipeEnabled) implements CustomizationEntry {
        public SingularityDefinition(ResourceLocation id, OutputTarget target, String displayName,
                                     int overlayColor, int underlayColor, int count, int timeCost,
                                     IngredientSpec ingredient, boolean enabled, boolean recipeEnabled) {
            this(id, target, "", displayName, overlayColor, underlayColor, count, timeCost,
                    ingredient, enabled, recipeEnabled);
        }

        public SingularityDefinition {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(target, "target");
            Objects.requireNonNull(note, "note");
            Objects.requireNonNull(displayName, "displayName");
            Objects.requireNonNull(ingredient, "ingredient");
        }

        @Override
        public EntryKind kind() {
            return EntryKind.SINGULARITY_DEFINITION;
        }
    }

    record SingularityOperation(ResourceLocation id, OutputTarget target, String note, SingularityAction action,
                                Optional<ResourceLocation> singularityId) implements CustomizationEntry {
        public SingularityOperation(ResourceLocation id, OutputTarget target, SingularityAction action,
                                    Optional<ResourceLocation> singularityId) {
            this(id, target, "", action, singularityId);
        }

        public SingularityOperation {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(target, "target");
            Objects.requireNonNull(note, "note");
            Objects.requireNonNull(action, "action");
            singularityId = Objects.requireNonNull(singularityId, "singularityId");
        }

        @Override
        public EntryKind kind() {
            return EntryKind.SINGULARITY_OPERATION;
        }
    }

    private static SortedMap<Integer, IngredientSpec> immutableGrid(SortedMap<Integer, IngredientSpec> source) {
        Objects.requireNonNull(source, "ingredients");
        TreeMap<Integer, IngredientSpec> copy = new TreeMap<>();
        source.forEach((slot, ingredient) -> copy.put(
                Objects.requireNonNull(slot, "slot"), Objects.requireNonNull(ingredient, "ingredient")));
        return Collections.unmodifiableSortedMap(copy);
    }

    private static <T> List<T> immutableList(List<T> source, String name) {
        Objects.requireNonNull(source, name);
        source.forEach(value -> Objects.requireNonNull(value, name + " element"));
        return List.copyOf(source);
    }
}
