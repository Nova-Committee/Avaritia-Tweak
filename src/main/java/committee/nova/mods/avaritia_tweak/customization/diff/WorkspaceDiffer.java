package committee.nova.mods.avaritia_tweak.customization.diff;

import committee.nova.mods.avaritia_tweak.customization.model.CustomizationEntry;
import committee.nova.mods.avaritia_tweak.customization.model.EntryKey;
import committee.nova.mods.avaritia_tweak.customization.model.IngredientSpec;
import committee.nova.mods.avaritia_tweak.customization.model.ItemStackSpec;
import committee.nova.mods.avaritia_tweak.customization.model.WorkspaceSnapshot;
import committee.nova.mods.avaritia_tweak.customization.render.ContentHashes;
import committee.nova.mods.avaritia_tweak.customization.render.NbtText;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

public final class WorkspaceDiffer {
    public WorkspaceDiff diff(WorkspaceSnapshot before, WorkspaceSnapshot after) {
        Objects.requireNonNull(before, "before");
        Objects.requireNonNull(after, "after");
        List<EntryChange> changes = new ArrayList<>();
        TreeSet<EntryKey> keys = new TreeSet<>(before.entries().keySet());
        keys.addAll(after.entries().keySet());
        for (EntryKey key : keys) {
            CustomizationEntry oldEntry = before.entries().get(key);
            CustomizationEntry newEntry = after.entries().get(key);
            if (oldEntry == null) {
                changes.add(new EntryChange(ChangeType.ADDED, key, Optional.empty(), Optional.of(newEntry),
                        List.of(new FieldChange("$", Optional.empty(), Optional.of(summary(newEntry))))));
            } else if (newEntry == null) {
                changes.add(new EntryChange(ChangeType.REMOVED, key, Optional.of(oldEntry), Optional.empty(),
                        List.of(new FieldChange("$", Optional.of(summary(oldEntry)), Optional.empty()))));
            } else if (!oldEntry.equals(newEntry)) {
                changes.add(new EntryChange(ChangeType.MODIFIED, key, Optional.of(oldEntry), Optional.of(newEntry),
                        changedFields(oldEntry, newEntry)));
            }
        }
        return new WorkspaceDiff(changes);
    }

    private static List<FieldChange> changedFields(CustomizationEntry before, CustomizationEntry after) {
        SortedMap<String, String> oldFields = fields(before);
        SortedMap<String, String> newFields = fields(after);
        TreeSet<String> paths = new TreeSet<>(oldFields.keySet());
        paths.addAll(newFields.keySet());
        List<FieldChange> changes = new ArrayList<>();
        for (String path : paths) {
            String oldValue = oldFields.get(path);
            String newValue = newFields.get(path);
            if (!Objects.equals(oldValue, newValue)) {
                changes.add(new FieldChange(path, Optional.ofNullable(oldValue), Optional.ofNullable(newValue)));
            }
        }
        return List.copyOf(changes);
    }

    private static SortedMap<String, String> fields(CustomizationEntry entry) {
        TreeMap<String, String> fields = new TreeMap<>();
        fields.put("target", entry.target().name());
        if (entry instanceof CustomizationEntry.ShapedTable shaped) {
            fields.put("tier", shaped.tier().name());
            shaped.ingredients().forEach((slot, ingredient) ->
                    fields.put("ingredients[" + slot + "]", ingredient(ingredient)));
            result(fields, shaped.result());
        } else if (entry instanceof CustomizationEntry.ShapelessTable shapeless) {
            fields.put("tier", shapeless.tier().name());
            ingredients(fields, shapeless.ingredients());
            result(fields, shapeless.result());
        } else if (entry instanceof CustomizationEntry.Compressor compressor) {
            fields.put("ingredient", ingredient(compressor.ingredient()));
            result(fields, compressor.result());
            fields.put("inputCount", Integer.toString(compressor.inputCount()));
            fields.put("timeCost", Integer.toString(compressor.timeCost()));
        } else if (entry instanceof CustomizationEntry.ExtremeSmithing smithing) {
            fields.put("template", ingredient(smithing.template()));
            fields.put("base", ingredient(smithing.base()));
            fields.put("addition", ingredient(smithing.addition()));
            result(fields, smithing.result());
        } else if (entry instanceof CustomizationEntry.InfinityCatalyst catalyst) {
            fields.put("group", catalyst.group());
            ingredients(fields, catalyst.ingredients());
            fields.put("count", Integer.toString(catalyst.count()));
        } else if (entry instanceof CustomizationEntry.EternalSingularity eternal) {
            ingredients(fields, eternal.ingredients());
            fields.put("count", Integer.toString(eternal.count()));
        } else if (entry instanceof CustomizationEntry.SingularityDefinition definition) {
            fields.put("displayName", definition.displayName());
            fields.put("overlayColor", Integer.toHexString(definition.overlayColor()));
            fields.put("underlayColor", Integer.toHexString(definition.underlayColor()));
            fields.put("count", Integer.toString(definition.count()));
            fields.put("timeCost", Integer.toString(definition.timeCost()));
            fields.put("ingredient", ingredient(definition.ingredient()));
            fields.put("enabled", Boolean.toString(definition.enabled()));
            fields.put("recipeEnabled", Boolean.toString(definition.recipeEnabled()));
        } else if (entry instanceof CustomizationEntry.SingularityOperation operation) {
            fields.put("action", operation.action().name());
            operation.singularityId().ifPresent(id -> fields.put("singularityId", id.toString()));
        }
        return fields;
    }

    private static void ingredients(Map<String, String> fields, List<IngredientSpec> ingredients) {
        for (int index = 0; index < ingredients.size(); index++) {
            fields.put("ingredients[" + index + "]", ingredient(ingredients.get(index)));
        }
    }

    private static void result(Map<String, String> fields, ItemStackSpec result) {
        fields.put("result.itemId", result.itemId().toString());
        fields.put("result.count", Integer.toString(result.count()));
        result.nbt().ifPresent(nbt -> fields.put("result.nbt", nbtSummary(nbt)));
    }

    private static String ingredient(IngredientSpec ingredient) {
        if (ingredient instanceof IngredientSpec.Choice choice) {
            return choice.alternatives().stream()
                    .map(WorkspaceDiffer::ingredient)
                    .collect(java.util.stream.Collectors.joining(" | ", "(", ")"));
        }
        if (ingredient instanceof IngredientSpec.Tag tag) {
            return "#" + tag.tagId();
        }
        IngredientSpec.Item item = (IngredientSpec.Item) ingredient;
        return item.itemId() + item.strictNbt().map(nbt -> " nbt:" + nbtSummary(nbt)).orElse("");
    }

    private static String nbtSummary(net.minecraft.nbt.CompoundTag nbt) {
        return ContentHashes.sha256(NbtText.canonical(nbt)).substring(0, 12);
    }

    private static String summary(CustomizationEntry entry) {
        return entry.kind().name() + " " + entry.id() + " -> " + entry.target().name();
    }
}
