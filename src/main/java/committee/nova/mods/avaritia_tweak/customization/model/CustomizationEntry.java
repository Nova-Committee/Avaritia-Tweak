package committee.nova.mods.avaritia_tweak.customization.model;

import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;

public sealed interface CustomizationEntry permits CustomizationEntry.ShapedTable,
        CustomizationEntry.ShapelessTable, CustomizationEntry.Compressor,
        CustomizationEntry.ExtremeSmithing, CustomizationEntry.InfinityCatalyst,
        CustomizationEntry.EternalSingularity, CustomizationEntry.SingularityDefinition,
        CustomizationEntry.SingularityOperation {

    ResourceLocation id();

    OutputTarget target();

    EntryKind kind();

    default EntryKey key() {
        return new EntryKey(this.kind(), this.id());
    }

    record ShapedTable(ResourceLocation id, OutputTarget target, CraftingTier tier,
                       SortedMap<Integer, IngredientSpec> ingredients,
                       ItemStackSpec result) implements CustomizationEntry {
        public ShapedTable {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(target, "target");
            Objects.requireNonNull(tier, "tier");
            ingredients = immutableGrid(ingredients);
            Objects.requireNonNull(result, "result");
        }

        @Override
        public EntryKind kind() {
            return EntryKind.SHAPED_TABLE;
        }
    }

    record ShapelessTable(ResourceLocation id, OutputTarget target, CraftingTier tier,
                          List<IngredientSpec> ingredients,
                          ItemStackSpec result) implements CustomizationEntry {
        public ShapelessTable {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(target, "target");
            Objects.requireNonNull(tier, "tier");
            ingredients = immutableList(ingredients, "ingredients");
            Objects.requireNonNull(result, "result");
        }

        @Override
        public EntryKind kind() {
            return EntryKind.SHAPELESS_TABLE;
        }
    }

    record Compressor(ResourceLocation id, OutputTarget target, IngredientSpec ingredient,
                      ItemStackSpec result, int inputCount, int timeCost) implements CustomizationEntry {
        public Compressor {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(target, "target");
            Objects.requireNonNull(ingredient, "ingredient");
            Objects.requireNonNull(result, "result");
        }

        @Override
        public EntryKind kind() {
            return EntryKind.COMPRESSOR;
        }
    }

    record ExtremeSmithing(ResourceLocation id, OutputTarget target, IngredientSpec template,
                           IngredientSpec base, IngredientSpec addition,
                           ItemStackSpec result) implements CustomizationEntry {
        public ExtremeSmithing {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(target, "target");
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

    record InfinityCatalyst(ResourceLocation id, OutputTarget target, String group,
                            List<IngredientSpec> ingredients, int count) implements CustomizationEntry {
        public InfinityCatalyst {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(target, "target");
            Objects.requireNonNull(group, "group");
            ingredients = immutableList(ingredients, "ingredients");
        }

        @Override
        public EntryKind kind() {
            return EntryKind.INFINITY_CATALYST;
        }
    }

    record EternalSingularity(ResourceLocation id, OutputTarget target,
                              List<IngredientSpec> ingredients, int count) implements CustomizationEntry {
        public EternalSingularity {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(target, "target");
            ingredients = immutableList(ingredients, "ingredients");
        }

        @Override
        public EntryKind kind() {
            return EntryKind.ETERNAL_SINGULARITY;
        }
    }

    record SingularityDefinition(ResourceLocation id, OutputTarget target, String displayName,
                                 int overlayColor, int underlayColor, int count, int timeCost,
                                 IngredientSpec ingredient, boolean enabled,
                                 boolean recipeEnabled) implements CustomizationEntry {
        public SingularityDefinition {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(target, "target");
            Objects.requireNonNull(displayName, "displayName");
            Objects.requireNonNull(ingredient, "ingredient");
        }

        @Override
        public EntryKind kind() {
            return EntryKind.SINGULARITY_DEFINITION;
        }
    }

    record SingularityOperation(ResourceLocation id, OutputTarget target, SingularityAction action,
                                Optional<ResourceLocation> singularityId) implements CustomizationEntry {
        public SingularityOperation {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(target, "target");
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
