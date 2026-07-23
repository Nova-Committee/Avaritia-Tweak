package committee.nova.mods.avaritia_tweak.customization.render;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import committee.nova.mods.avaritia_tweak.customization.model.CustomizationEntry;
import committee.nova.mods.avaritia_tweak.customization.model.IngredientSpec;
import committee.nova.mods.avaritia_tweak.customization.model.OutputTarget;
import committee.nova.mods.avaritia_tweak.customization.model.WorkspaceSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class SingularityDatapackRenderer implements ArtifactRenderer {
    public static final String ROOT = "avaritia_tweak_exports/avaritia_tweak_singularities/";
    public static final ArtifactPath PACK_METADATA = ArtifactPath.of(ROOT + "pack.mcmeta");
    static final int PACK_FORMAT = 48;
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    @Override
    public Set<OutputTarget> supportedTargets() {
        return Set.of(OutputTarget.DATAPACK);
    }

    @Override
    public List<RenderedArtifact> render(WorkspaceSnapshot snapshot) {
        List<RenderedArtifact> artifacts = new ArrayList<>();
        artifacts.add(RenderedArtifact.text(PACK_METADATA, GSON.toJson(packMetadata())));
        snapshot.entries().values().stream()
                .filter(entry -> entry.target() == OutputTarget.DATAPACK)
                .filter(CustomizationEntry.SingularityDefinition.class::isInstance)
                .map(CustomizationEntry.SingularityDefinition.class::cast)
                .forEach(definition -> artifacts.add(RenderedArtifact.text(path(definition),
                        GSON.toJson(singularity(definition)))));
        return List.copyOf(artifacts);
    }

    private static JsonObject packMetadata() {
        JsonObject pack = new JsonObject();
        pack.addProperty("pack_format", PACK_FORMAT);
        pack.addProperty("description", "Avaritia-Tweak generated singularities");
        JsonObject root = new JsonObject();
        root.add("pack", pack);
        return root;
    }

    private static ArtifactPath path(CustomizationEntry.SingularityDefinition definition) {
        return ArtifactPath.of(ROOT + "data/" + definition.id().getNamespace()
                + "/singularities/" + definition.id().getPath() + ".json");
    }

    private static JsonObject singularity(CustomizationEntry.SingularityDefinition definition) {
        JsonObject json = new JsonObject();
        json.addProperty("name", definition.id().toString());
        json.addProperty("displayName", definition.displayName());
        json.addProperty("overlayColor", definition.overlayColor());
        json.addProperty("underlayColor", definition.underlayColor());
        json.addProperty("count", definition.count());
        json.addProperty("timeCost", definition.timeCost());
        json.add("ingredient", ingredient(definition.ingredient()));
        json.addProperty("enabled", definition.enabled());
        json.addProperty("recipeEnabled", definition.recipeEnabled());
        return json;
    }

    static JsonObject ingredient(IngredientSpec ingredient) {
        JsonObject json = new JsonObject();
        if (ingredient instanceof IngredientSpec.Tag tag) {
            json.addProperty("tag", tag.tagId().toString());
            return json;
        }
        IngredientSpec.Item item = (IngredientSpec.Item) ingredient;
        item.strictNbt().ifPresent(nbt -> {
            JsonObject components = new JsonObject();
            JsonArray items = new JsonArray();
            items.add(item.itemId().toString());
            components.addProperty("minecraft:custom_data", NbtText.canonical(nbt));
            json.addProperty("type", "neoforge:components");
            json.add("items", items);
            json.add("components", components);
            json.addProperty("strict", true);
        });
        if (item.strictNbt().isEmpty()) {
            json.addProperty("item", item.itemId().toString());
        }
        return json;
    }

}
