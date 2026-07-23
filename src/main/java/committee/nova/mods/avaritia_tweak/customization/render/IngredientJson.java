package committee.nova.mods.avaritia_tweak.customization.render;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import committee.nova.mods.avaritia_tweak.customization.model.IngredientSpec;
import committee.nova.mods.avaritia_tweak.customization.model.ItemStackSpec;

/** Canonical Minecraft/NeoForge JSON projection shared by raw recipes and data packs. */
final class IngredientJson {
    private IngredientJson() {
    }

    static JsonElement encode(IngredientSpec ingredient) {
        if (ingredient instanceof IngredientSpec.Choice choice) {
            JsonArray alternatives = new JsonArray();
            choice.alternatives().forEach(value -> alternatives.add(encode(value)));
            return alternatives;
        }
        JsonObject json = new JsonObject();
        if (ingredient instanceof IngredientSpec.Tag tag) {
            json.addProperty("tag", tag.tagId().toString());
            return json;
        }
        IngredientSpec.Item item = (IngredientSpec.Item) ingredient;
        item.strictNbt().ifPresentOrElse(nbt -> {
            JsonArray items = new JsonArray();
            items.add(item.itemId().toString());
            JsonObject components = new JsonObject();
            components.addProperty("minecraft:custom_data", NbtText.canonical(nbt));
            json.addProperty("type", "neoforge:components");
            json.add("items", items);
            json.add("components", components);
            json.addProperty("strict", true);
        }, () -> json.addProperty("item", item.itemId().toString()));
        return json;
    }

    static JsonObject stack(ItemStackSpec stack) {
        JsonObject json = new JsonObject();
        json.addProperty("id", stack.itemId().toString());
        json.addProperty("count", stack.count());
        stack.nbt().ifPresent(nbt -> {
            JsonObject components = new JsonObject();
            components.addProperty("minecraft:custom_data", NbtText.canonical(nbt));
            json.add("components", components);
        });
        return json;
    }
}
