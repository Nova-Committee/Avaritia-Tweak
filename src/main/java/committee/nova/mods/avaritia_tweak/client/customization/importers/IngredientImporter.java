package committee.nova.mods.avaritia_tweak.client.customization.importers;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import committee.nova.mods.avaritia_tweak.customization.model.IngredientSpec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class IngredientImporter {
    public IngredientImportResult importIngredient(Ingredient ingredient, String fieldPath) {
        return importSerialized(ingredient.toJson(), fieldPath);
    }

    IngredientImportResult importSerialized(JsonElement serialized, String fieldPath) {
        if (serialized.isJsonArray()) {
            JsonArray encodedAlternatives = serialized.getAsJsonArray();
            if (encodedAlternatives.size() == 0) {
                return failure(fieldPath, "ingredient.empty.unsupported",
                        "Empty ingredients are not supported");
            }
            List<IngredientSpec> alternatives = new ArrayList<>();
            for (int index = 0; index < encodedAlternatives.size(); index++) {
                IngredientImportResult imported = importSerialized(encodedAlternatives.get(index),
                        fieldPath + "[" + index + "]");
                if (imported instanceof IngredientImportResult.Failure) {
                    return imported;
                }
                IngredientSpec ingredient = ((IngredientImportResult.Success) imported).ingredient();
                if (ingredient instanceof IngredientSpec.Choice nested) {
                    alternatives.addAll(nested.alternatives());
                } else {
                    alternatives.add(ingredient);
                }
            }
            return new IngredientImportResult.Success(alternatives.size() == 1
                    ? alternatives.get(0)
                    : new IngredientSpec.Choice(alternatives));
        }
        if (!serialized.isJsonObject()) {
            return failure(fieldPath, "ingredient.format.unsupported",
                    "Ingredient is not represented by a supported object");
        }
        JsonObject json = serialized.getAsJsonObject();
        if (json.has("type")) {
            String type = stringValue(json, "type");
            if (type == null) {
                return failure(fieldPath, "ingredient.custom.invalid",
                        "Custom ingredient type must be a string");
            }
            return switch (type) {
                case "forge:nbt" -> importNbtIngredient(json, fieldPath);
                case "forge:compound" -> importCompoundIngredient(json, fieldPath);
                default -> failure(fieldPath, "ingredient.custom.unsupported",
                        "Unsupported custom ingredient type " + type);
            };
        }
        boolean hasItem = json.has("item");
        boolean hasTag = json.has("tag");
        if (hasItem == hasTag) {
            return failure(fieldPath, "ingredient.format.unsupported",
                    "Ingredient must contain exactly one item or tag");
        }
        ResourceLocation id = resourceLocation(json, hasItem ? "item" : "tag");
        if (id == null) {
            return failure(fieldPath, "ingredient.id.invalid", "Ingredient resource ID is invalid");
        }
        return new IngredientImportResult.Success(hasItem
                ? new IngredientSpec.Item(id)
                : new IngredientSpec.Tag(id));
    }

    private IngredientImportResult importCompoundIngredient(JsonObject json, String fieldPath) {
        JsonElement children = json.has("children") ? json.get("children") : json.get("ingredients");
        if (children == null || !children.isJsonArray()) {
            return failure(fieldPath, "ingredient.compound.invalid",
                    "Compound ingredient requires a children array");
        }
        return importSerialized(children, fieldPath);
    }

    private static IngredientImportResult importNbtIngredient(JsonObject json, String fieldPath) {
        ResourceLocation itemId = resourceLocation(json, "item");
        if (itemId == null || !json.has("nbt")) {
            return failure(fieldPath, "ingredient.nbt.invalid",
                    "Strict NBT ingredient requires item and nbt fields");
        }
        try {
            JsonElement encodedNbt = json.get("nbt");
            String snbt = encodedNbt.isJsonPrimitive()
                    ? encodedNbt.getAsString()
                    : encodedNbt.toString();
            CompoundTag nbt = TagParser.parseTag(snbt);
            return new IngredientImportResult.Success(
                    new IngredientSpec.Item(itemId, Optional.of(nbt)));
        } catch (Exception exception) {
            return failure(fieldPath, "ingredient.nbt.invalid",
                    exception.getMessage() == null ? "Invalid strict NBT" : exception.getMessage());
        }
    }

    private static ResourceLocation resourceLocation(JsonObject json, String field) {
        String value = stringValue(json, field);
        return value == null ? null : ResourceLocation.tryParse(value);
    }

    private static String stringValue(JsonObject json, String field) {
        if (!json.has(field) || !json.get(field).isJsonPrimitive()
                || !json.getAsJsonPrimitive(field).isString()) {
            return null;
        }
        return json.get(field).getAsString();
    }

    private static IngredientImportResult.Failure failure(String fieldPath, String code, String message) {
        return new IngredientImportResult.Failure(fieldPath, code, message);
    }
}
