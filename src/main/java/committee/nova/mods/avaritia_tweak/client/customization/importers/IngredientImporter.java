package committee.nova.mods.avaritia_tweak.client.customization.importers;

import com.google.gson.JsonElement;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import committee.nova.mods.avaritia_tweak.customization.model.IngredientSpec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.Optional;

public final class IngredientImporter {
    public IngredientImportResult importIngredient(Ingredient ingredient, String fieldPath) {
        JsonElement serialized;
        try {
            serialized = Ingredient.CODEC.encodeStart(JsonOps.INSTANCE, ingredient).getOrThrow();
        } catch (RuntimeException exception) {
            return failure(fieldPath, "ingredient.serialize.failed",
                    exception.getMessage() == null ? "Ingredient could not be serialized" : exception.getMessage());
        }
        return importSerialized(serialized, fieldPath);
    }

    IngredientImportResult importSerialized(JsonElement serialized, String fieldPath) {
        if (serialized.isJsonArray()) {
            return failure(fieldPath, "ingredient.or.unsupported",
                    "Multiple-choice and empty ingredients are not supported");
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
                case "neoforge:components" -> importComponentIngredient(json, fieldPath);
                case "forge:nbt" -> importLegacyNbtIngredient(json, fieldPath);
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

    private static IngredientImportResult importComponentIngredient(JsonObject json, String fieldPath) {
        if (!json.has("strict") || !json.get("strict").isJsonPrimitive()
                || !json.getAsJsonPrimitive("strict").isBoolean()
                || !json.get("strict").getAsBoolean()) {
            return failure(fieldPath, "ingredient.components.unsupported",
                    "Only strict NeoForge component ingredients are supported");
        }
        ResourceLocation itemId = singleItem(json.get("items"));
        if (itemId == null || !json.has("components") || !json.get("components").isJsonObject()) {
            return failure(fieldPath, "ingredient.components.invalid",
                    "Strict component ingredient requires one item and a components object");
        }
        JsonObject components = json.getAsJsonObject("components");
        if (components.size() != 1 || !components.has("minecraft:custom_data")) {
            return failure(fieldPath, "ingredient.components.unsupported",
                    "Only the minecraft:custom_data component can be edited");
        }
        return strictItem(itemId, components.get("minecraft:custom_data"), fieldPath);
    }

    private static IngredientImportResult importLegacyNbtIngredient(JsonObject json, String fieldPath) {
        ResourceLocation itemId = resourceLocation(json, "item");
        if (itemId == null || !json.has("nbt")) {
            return failure(fieldPath, "ingredient.nbt.invalid",
                    "Strict NBT ingredient requires item and nbt fields");
        }
        return strictItem(itemId, json.get("nbt"), fieldPath);
    }

    private static IngredientImportResult strictItem(ResourceLocation itemId, JsonElement encodedNbt,
                                                     String fieldPath) {
        try {
            String snbt = encodedNbt.isJsonPrimitive()
                    ? encodedNbt.getAsString()
                    : encodedNbt.toString();
            CompoundTag nbt = TagParser.parseTag(snbt);
            return new IngredientImportResult.Success(new IngredientSpec.Item(itemId, Optional.of(nbt)));
        } catch (Exception exception) {
            return failure(fieldPath, "ingredient.nbt.invalid",
                    exception.getMessage() == null ? "Invalid strict NBT" : exception.getMessage());
        }
    }

    private static ResourceLocation singleItem(JsonElement encodedItems) {
        if (encodedItems == null) {
            return null;
        }
        JsonElement item = encodedItems;
        if (encodedItems.isJsonArray()) {
            JsonArray items = encodedItems.getAsJsonArray();
            if (items.size() != 1) {
                return null;
            }
            item = items.get(0);
        }
        if (!item.isJsonPrimitive() || !item.getAsJsonPrimitive().isString()) {
            return null;
        }
        return ResourceLocation.tryParse(item.getAsString());
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
