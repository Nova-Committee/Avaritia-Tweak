package committee.nova.mods.avaritia_tweak.customization.history;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import committee.nova.mods.avaritia_tweak.customization.model.CraftingTier;
import committee.nova.mods.avaritia_tweak.customization.model.CustomizationEntry;
import committee.nova.mods.avaritia_tweak.customization.model.EntryKey;
import committee.nova.mods.avaritia_tweak.customization.model.EntryKind;
import committee.nova.mods.avaritia_tweak.customization.model.ExtremeSmithingInputs;
import committee.nova.mods.avaritia_tweak.customization.model.IngredientSpec;
import committee.nova.mods.avaritia_tweak.customization.model.ItemStackSpec;
import committee.nova.mods.avaritia_tweak.customization.model.OutputTarget;
import committee.nova.mods.avaritia_tweak.customization.model.SingularityAction;
import committee.nova.mods.avaritia_tweak.customization.model.WorkspaceSnapshot;
import committee.nova.mods.avaritia_tweak.customization.render.NbtText;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;

public final class WorkspaceCodec {
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    public String encode(WorkspaceSnapshot snapshot) {
        return encodeJson(toJson(snapshot));
    }

    String encodeJson(JsonObject snapshotJson) {
        return GSON.toJson(snapshotJson) + "\n";
    }

    public JsonObject toJson(WorkspaceSnapshot snapshot) {
        JsonObject root = new JsonObject();
        root.addProperty("schemaVersion", snapshot.schemaVersion());
        JsonArray entries = new JsonArray();
        snapshot.entries().values().forEach(entry -> entries.add(entry(entry)));
        root.add("entries", entries);
        return root;
    }

    public WorkspaceDecodeResult decode(String content) {
        try {
            JsonElement parsed = JsonParser.parseString(content);
            if (!parsed.isJsonObject()) {
                throw failure("workspace.root", "$", "Workspace root must be an object");
            }
            return decode(parsed.getAsJsonObject());
        } catch (DecodeException exception) {
            return new WorkspaceDecodeResult.Failure(exception.code, exception.path, exception.getMessage());
        } catch (JsonParseException | IllegalStateException exception) {
            return new WorkspaceDecodeResult.Failure("workspace.json.invalid", "$", safeMessage(exception));
        }
    }

    public WorkspaceDecodeResult decode(JsonObject root) {
        try {
            return new WorkspaceDecodeResult.Success(decodeWorkspace(root));
        } catch (DecodeException exception) {
            return new WorkspaceDecodeResult.Failure(exception.code, exception.path, exception.getMessage());
        } catch (RuntimeException exception) {
            return new WorkspaceDecodeResult.Failure("workspace.json.invalid", "$", safeMessage(exception));
        }
    }

    private static WorkspaceSnapshot decodeWorkspace(JsonObject root) {
        int schemaVersion = integer(root, "schemaVersion", "schemaVersion");
        if (schemaVersion != WorkspaceSnapshot.CURRENT_SCHEMA_VERSION) {
            throw failure("workspace.schema.unsupported", "schemaVersion",
                    "Unsupported workspace schema " + schemaVersion + "; expected "
                            + WorkspaceSnapshot.CURRENT_SCHEMA_VERSION);
        }
        JsonArray encodedEntries = array(root, "entries", "entries");
        SortedMap<EntryKey, CustomizationEntry> entries = new TreeMap<>();
        for (int index = 0; index < encodedEntries.size(); index++) {
            String path = "entries[" + index + "]";
            JsonElement element = encodedEntries.get(index);
            if (!element.isJsonObject()) {
                throw failure("workspace.entry.invalid", path, "Entry must be an object");
            }
            CustomizationEntry decoded = decodeEntry(element.getAsJsonObject(), path);
            if (entries.put(decoded.key(), decoded) != null) {
                throw failure("workspace.entry.duplicate", path + ".id",
                        "Duplicate entry key " + decoded.key());
            }
        }
        return new WorkspaceSnapshot(schemaVersion, entries);
    }

    private static JsonObject entry(CustomizationEntry entry) {
        JsonObject json = new JsonObject();
        json.addProperty("kind", entry.kind().name());
        json.addProperty("id", entry.id().toString());
        json.addProperty("target", entry.target().name());
        json.addProperty("note", entry.note());
        if (entry instanceof CustomizationEntry.ShapedTable shaped) {
            positionedRecipe(json, shaped.tier(), shaped.ingredients(), shaped.result());
        } else if (entry instanceof CustomizationEntry.NoConsumeCatalystShaped shaped) {
            positionedRecipe(json, shaped.tier(), shaped.ingredients(), shaped.result());
        } else if (entry instanceof CustomizationEntry.ShapelessTable shapeless) {
            json.addProperty("tier", shapeless.tier().name());
            json.add("ingredients", ingredients(shapeless.ingredients()));
            json.add("result", itemStack(shapeless.result()));
        } else if (entry instanceof CustomizationEntry.Compressor compressor) {
            json.add("ingredient", ingredient(compressor.ingredient()));
            json.add("result", itemStack(compressor.result()));
            json.addProperty("inputCount", compressor.inputCount());
            json.addProperty("timeCost", compressor.timeCost());
        } else if (entry instanceof CustomizationEntry.ExtremeSmithing smithing) {
            json.add("template", ingredient(smithing.template()));
            json.add("base", ingredient(smithing.base()));
            json.add("additions", ingredients(smithing.additions()));
            json.add("result", itemStack(smithing.result()));
        } else if (entry instanceof CustomizationEntry.InfinityCatalyst catalyst) {
            json.addProperty("group", catalyst.group());
            json.add("ingredients", ingredients(catalyst.ingredients()));
            json.addProperty("count", catalyst.count());
        } else if (entry instanceof CustomizationEntry.EternalSingularity eternal) {
            json.add("ingredients", ingredients(eternal.ingredients()));
            json.addProperty("count", eternal.count());
        } else if (entry instanceof CustomizationEntry.SingularityDefinition definition) {
            json.addProperty("displayName", definition.displayName());
            json.addProperty("overlayColor", definition.overlayColor());
            json.addProperty("underlayColor", definition.underlayColor());
            json.addProperty("count", definition.count());
            json.addProperty("timeCost", definition.timeCost());
            json.add("ingredient", ingredient(definition.ingredient()));
            json.addProperty("enabled", definition.enabled());
            json.addProperty("recipeEnabled", definition.recipeEnabled());
        } else if (entry instanceof CustomizationEntry.SingularityOperation operation) {
            json.addProperty("action", operation.action().name());
            operation.singularityId().ifPresent(id -> json.addProperty("singularityId", id.toString()));
        }
        return json;
    }

    private static CustomizationEntry decodeEntry(JsonObject json, String path) {
        EntryKind kind = enumeration(EntryKind.class, string(json, "kind", path + ".kind"), path + ".kind");
        ResourceLocation id = resourceLocation(string(json, "id", path + ".id"), path + ".id");
        OutputTarget target = enumeration(OutputTarget.class,
                string(json, "target", path + ".target"), path + ".target");
        String note = optionalString(json, "note").orElse("");
        return switch (kind) {
            case SHAPED_TABLE -> new CustomizationEntry.ShapedTable(id, target, note,
                    enumeration(CraftingTier.class, string(json, "tier", path + ".tier"), path + ".tier"),
                    positionedIngredients(json, path), itemStack(object(json, "result", path + ".result"),
                    path + ".result"));
            case NO_CONSUME_CATALYST_SHAPED -> new CustomizationEntry.NoConsumeCatalystShaped(
                    id, target, note,
                    enumeration(CraftingTier.class, string(json, "tier", path + ".tier"), path + ".tier"),
                    positionedIngredients(json, path), itemStack(object(json, "result", path + ".result"),
                    path + ".result"));
            case SHAPELESS_TABLE -> new CustomizationEntry.ShapelessTable(id, target, note,
                    enumeration(CraftingTier.class, string(json, "tier", path + ".tier"), path + ".tier"),
                    ingredients(json, path), itemStack(object(json, "result", path + ".result"),
                    path + ".result"));
            case COMPRESSOR -> new CustomizationEntry.Compressor(id, target, note,
                    ingredient(object(json, "ingredient", path + ".ingredient"), path + ".ingredient"),
                    itemStack(object(json, "result", path + ".result"), path + ".result"),
                    integer(json, "inputCount", path + ".inputCount"),
                    integer(json, "timeCost", path + ".timeCost"));
            case EXTREME_SMITHING -> new CustomizationEntry.ExtremeSmithing(id, target, note,
                    ingredient(object(json, "template", path + ".template"), path + ".template"),
                    ingredient(object(json, "base", path + ".base"), path + ".base"),
                    smithingAdditions(json, path),
                    itemStack(object(json, "result", path + ".result"), path + ".result"));
            case INFINITY_CATALYST -> new CustomizationEntry.InfinityCatalyst(id, target, note,
                    string(json, "group", path + ".group"), ingredients(json, path),
                    integer(json, "count", path + ".count"));
            case ETERNAL_SINGULARITY -> new CustomizationEntry.EternalSingularity(id, target, note,
                    ingredients(json, path), integer(json, "count", path + ".count"));
            case SINGULARITY_DEFINITION -> new CustomizationEntry.SingularityDefinition(id, target, note,
                    string(json, "displayName", path + ".displayName"),
                    integer(json, "overlayColor", path + ".overlayColor"),
                    integer(json, "underlayColor", path + ".underlayColor"),
                    integer(json, "count", path + ".count"),
                    integer(json, "timeCost", path + ".timeCost"),
                    ingredient(object(json, "ingredient", path + ".ingredient"), path + ".ingredient"),
                    bool(json, "enabled", path + ".enabled"),
                    bool(json, "recipeEnabled", path + ".recipeEnabled"));
            case SINGULARITY_OPERATION -> new CustomizationEntry.SingularityOperation(id, target, note,
                    enumeration(SingularityAction.class, string(json, "action", path + ".action"),
                            path + ".action"),
                    optionalString(json, "singularityId")
                            .map(value -> resourceLocation(value, path + ".singularityId")));
        };
    }

    private static void positionedRecipe(JsonObject json, CraftingTier tier,
                                         SortedMap<Integer, IngredientSpec> positionedIngredients,
                                         ItemStackSpec result) {
        json.addProperty("tier", tier.name());
        JsonArray ingredients = new JsonArray();
        positionedIngredients.forEach((slot, ingredient) -> {
            JsonObject positioned = new JsonObject();
            positioned.addProperty("slot", slot);
            positioned.add("ingredient", ingredient(ingredient));
            ingredients.add(positioned);
        });
        json.add("ingredients", ingredients);
        json.add("result", itemStack(result));
    }

    private static JsonObject ingredient(IngredientSpec ingredient) {
        JsonObject json = new JsonObject();
        if (ingredient instanceof IngredientSpec.Choice choice) {
            json.addProperty("type", "choice");
            json.add("alternatives", ingredients(choice.alternatives()));
        } else if (ingredient instanceof IngredientSpec.Tag tag) {
            json.addProperty("type", "tag");
            json.addProperty("id", tag.tagId().toString());
        } else if (ingredient instanceof IngredientSpec.Components components) {
            json.addProperty("type", "components");
            json.addProperty("id", components.itemId().toString());
            json.add("components", JsonParser.parseString(components.componentsJson()));
            json.addProperty("strict", components.strict());
        } else {
            IngredientSpec.Item item = (IngredientSpec.Item) ingredient;
            json.addProperty("type", "item");
            json.addProperty("id", item.itemId().toString());
            item.strictNbt().ifPresent(nbt -> json.addProperty("strictNbt", NbtText.canonical(nbt)));
        }
        return json;
    }

    private static IngredientSpec ingredient(JsonObject json, String path) {
        String type = string(json, "type", path + ".type");
        return switch (type) {
            case "item" -> new IngredientSpec.Item(
                    resourceLocation(string(json, "id", path + ".id"), path + ".id"),
                    optionalString(json, "strictNbt")
                            .map(value -> nbt(value, path + ".strictNbt")));
            case "tag" -> {
                if (json.has("strictNbt")) {
                    throw failure("workspace.ingredient.tag_nbt", path + ".strictNbt",
                            "Tags cannot carry strict NBT");
                }
                yield new IngredientSpec.Tag(
                        resourceLocation(string(json, "id", path + ".id"), path + ".id"));
            }
            case "components" -> new IngredientSpec.Components(
                    resourceLocation(string(json, "id", path + ".id"), path + ".id"),
                    object(json, "components", path + ".components").toString(),
                    bool(json, "strict", path + ".strict"));
            case "choice" -> {
                JsonArray alternatives = array(json, "alternatives", path + ".alternatives");
                if (alternatives.size() < 2) {
                    throw failure("workspace.ingredient.choice.size", path + ".alternatives",
                            "Choice ingredients require at least two alternatives");
                }
                List<IngredientSpec> decoded = new ArrayList<>();
                for (int index = 0; index < alternatives.size(); index++) {
                    String alternativePath = path + ".alternatives[" + index + "]";
                    JsonElement alternative = alternatives.get(index);
                    if (!alternative.isJsonObject()) {
                        throw failure("workspace.ingredient.invalid", alternativePath,
                                "Choice alternative must be an object");
                    }
                    decoded.add(ingredient(alternative.getAsJsonObject(), alternativePath));
                }
                yield new IngredientSpec.Choice(decoded);
            }
            default -> throw failure("workspace.ingredient.type", path + ".type",
                    "Unsupported ingredient type " + type);
        };
    }

    private static JsonArray ingredients(List<IngredientSpec> ingredients) {
        JsonArray json = new JsonArray();
        ingredients.forEach(value -> json.add(ingredient(value)));
        return json;
    }

    private static List<IngredientSpec> ingredients(JsonObject parent, String path) {
        return ingredientList(array(parent, "ingredients", path + ".ingredients"),
                path + ".ingredients");
    }

    private static List<IngredientSpec> smithingAdditions(JsonObject parent, String path) {
        if (!parent.has("additions")) {
            IngredientSpec legacy = ingredient(object(parent, "addition", path + ".addition"),
                    path + ".addition");
            return ExtremeSmithingInputs.expandSerializedAddition(legacy);
        }
        JsonArray values = array(parent, "additions", path + ".additions");
        if (values.size() != ExtremeSmithingInputs.ADDITION_SLOT_COUNT) {
            throw failure("workspace.smithing.additions.size", path + ".additions",
                    "Extreme smithing requires exactly three addition inputs");
        }
        return ingredientList(values, path + ".additions");
    }

    private static List<IngredientSpec> ingredientList(JsonArray values, String path) {
        List<IngredientSpec> result = new ArrayList<>();
        for (int index = 0; index < values.size(); index++) {
            String itemPath = path + "[" + index + "]";
            if (!values.get(index).isJsonObject()) {
                throw failure("workspace.ingredient.invalid", itemPath, "Ingredient must be an object");
            }
            result.add(ingredient(values.get(index).getAsJsonObject(), itemPath));
        }
        return List.copyOf(result);
    }

    private static SortedMap<Integer, IngredientSpec> positionedIngredients(JsonObject parent, String path) {
        JsonArray values = array(parent, "ingredients", path + ".ingredients");
        TreeMap<Integer, IngredientSpec> result = new TreeMap<>();
        for (int index = 0; index < values.size(); index++) {
            String itemPath = path + ".ingredients[" + index + "]";
            if (!values.get(index).isJsonObject()) {
                throw failure("workspace.ingredient.invalid", itemPath, "Positioned ingredient must be an object");
            }
            JsonObject positioned = values.get(index).getAsJsonObject();
            int slot = integer(positioned, "slot", itemPath + ".slot");
            IngredientSpec decoded = ingredient(object(positioned, "ingredient", itemPath + ".ingredient"),
                    itemPath + ".ingredient");
            if (result.put(slot, decoded) != null) {
                throw failure("workspace.grid.slot.duplicate", itemPath + ".slot",
                        "Duplicate grid slot " + slot);
            }
        }
        return result;
    }

    private static JsonObject itemStack(ItemStackSpec stack) {
        JsonObject json = new JsonObject();
        json.addProperty("itemId", stack.itemId().toString());
        json.addProperty("count", stack.count());
        stack.nbt().ifPresent(nbt -> json.addProperty("nbt", NbtText.canonical(nbt)));
        return json;
    }

    private static ItemStackSpec itemStack(JsonObject json, String path) {
        return new ItemStackSpec(
                resourceLocation(string(json, "itemId", path + ".itemId"), path + ".itemId"),
                integer(json, "count", path + ".count"),
                optionalString(json, "nbt").map(value -> nbt(value, path + ".nbt")));
    }

    private static CompoundTag nbt(String value, String path) {
        try {
            return TagParser.parseTag(value);
        } catch (Exception exception) {
            throw failure("workspace.nbt.invalid", path, "Invalid SNBT: " + safeMessage(exception));
        }
    }

    private static ResourceLocation resourceLocation(String value, String path) {
        ResourceLocation parsed = ResourceLocation.tryParse(value);
        if (parsed == null) {
            throw failure("workspace.resource_location.invalid", path, "Invalid resource location " + value);
        }
        return parsed;
    }

    private static <E extends Enum<E>> E enumeration(Class<E> type, String value, String path) {
        try {
            return Enum.valueOf(type, value);
        } catch (IllegalArgumentException exception) {
            throw failure("workspace.enum.invalid", path, "Unsupported " + type.getSimpleName() + " " + value);
        }
    }

    private static String string(JsonObject parent, String name, String path) {
        JsonElement value = required(parent, name, path);
        try {
            return value.getAsString();
        } catch (RuntimeException exception) {
            throw failure("workspace.field.string", path, "Expected a string");
        }
    }

    private static Optional<String> optionalString(JsonObject parent, String name) {
        if (!parent.has(name) || parent.get(name).isJsonNull()) {
            return Optional.empty();
        }
        try {
            return Optional.of(parent.get(name).getAsString());
        } catch (RuntimeException exception) {
            throw failure("workspace.field.string", name, "Expected a string");
        }
    }

    private static int integer(JsonObject parent, String name, String path) {
        JsonElement value = required(parent, name, path);
        try {
            return value.getAsInt();
        } catch (RuntimeException exception) {
            throw failure("workspace.field.integer", path, "Expected an integer");
        }
    }

    private static boolean bool(JsonObject parent, String name, String path) {
        JsonElement value = required(parent, name, path);
        try {
            return value.getAsBoolean();
        } catch (RuntimeException exception) {
            throw failure("workspace.field.boolean", path, "Expected a boolean");
        }
    }

    private static JsonArray array(JsonObject parent, String name, String path) {
        JsonElement value = required(parent, name, path);
        if (!value.isJsonArray()) {
            throw failure("workspace.field.array", path, "Expected an array");
        }
        return value.getAsJsonArray();
    }

    private static JsonObject object(JsonObject parent, String name, String path) {
        JsonElement value = required(parent, name, path);
        if (!value.isJsonObject()) {
            throw failure("workspace.field.object", path, "Expected an object");
        }
        return value.getAsJsonObject();
    }

    private static JsonElement required(JsonObject parent, String name, String path) {
        if (!parent.has(name) || parent.get(name).isJsonNull()) {
            throw failure("workspace.field.missing", path, "Missing required field " + name);
        }
        return parent.get(name);
    }

    private static DecodeException failure(String code, String path, String message) {
        return new DecodeException(code, path, message);
    }

    private static String safeMessage(Throwable throwable) {
        return throwable.getMessage() == null ? throwable.getClass().getSimpleName() : throwable.getMessage();
    }

    private static final class DecodeException extends RuntimeException {
        private final String code;
        private final String path;

        private DecodeException(String code, String path, String message) {
            super(message);
            this.code = code;
            this.path = path;
        }
    }
}
