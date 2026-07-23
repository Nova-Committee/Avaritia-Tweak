package committee.nova.mods.avaritia_tweak.customization.render;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import committee.nova.mods.avaritia_tweak.customization.model.IngredientSpec;
import committee.nova.mods.avaritia_tweak.customization.model.ItemStackSpec;

/** Canonical Minecraft/Forge 1.20.1 JSON projection for raw Avaritia recipes. */
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
        if (ingredient instanceof IngredientSpec.Components) {
            throw new IllegalArgumentException(
                    "Data-component predicates are unavailable on Minecraft 1.20.1");
        }
        IngredientSpec.Item item = (IngredientSpec.Item) ingredient;
        item.strictNbt().ifPresentOrElse(nbt -> {
            json.addProperty("type", "forge:nbt");
            json.addProperty("item", item.itemId().toString());
            json.addProperty("nbt", NbtText.canonical(nbt));
        }, () -> json.addProperty("item", item.itemId().toString()));
        return json;
    }

    static JsonObject stack(ItemStackSpec stack) {
        JsonObject json = new JsonObject();
        json.addProperty("item", stack.itemId().toString());
        json.addProperty("count", stack.count());
        stack.nbt().ifPresent(nbt -> json.addProperty("nbt", NbtText.canonical(nbt)));
        return json;
    }
}
