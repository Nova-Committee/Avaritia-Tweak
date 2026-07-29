package committee.nova.mods.avaritia_tweak.customization.history;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import committee.nova.mods.avaritia_tweak.customization.model.CustomizationEntry;
import committee.nova.mods.avaritia_tweak.customization.model.IngredientSpec;
import committee.nova.mods.avaritia_tweak.customization.render.ContentHashes;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RevisionCodecTest {
    private static final Gson JSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();
    private final RevisionCodec codec = new RevisionCodec(new WorkspaceCodec());

    @Test
    void validatesLegacySnapshotBeforeAddingTheDefaultNote() {
        JsonObject snapshot = workspace("""
                {
                  "schemaVersion": 1,
                  "entries": [{
                    "kind": "COMPRESSOR",
                    "id": "test:legacy",
                    "target": "KUBEJS",
                    "ingredient": {"type":"item","id":"minecraft:stone"},
                    "result": {"itemId":"minecraft:diamond","count":1},
                    "inputCount": 1,
                    "timeCost": 1
                  }]
                }
                """);

        RevisionDecodeResult result = this.codec.decode(revision(snapshot, snapshotHash(snapshot)));

        assertThat(result).isInstanceOfSatisfying(RevisionDecodeResult.Success.class, success -> {
            assertThat(success.revision().version()).isEqualTo(4);
            assertThat(success.revision().snapshot().entries().values())
                    .singleElement()
                    .extracting(CustomizationEntry::note)
                    .isEqualTo("");
        });
    }

    @Test
    void validatesLegacySmithingSnapshotBeforeExpandingItsAddition() {
        JsonObject snapshot = workspace("""
                {
                  "schemaVersion": 1,
                  "entries": [{
                    "kind": "EXTREME_SMITHING",
                    "id": "test:legacy_smithing",
                    "target": "KUBEJS",
                    "note": "",
                    "template": {"type":"item","id":"minecraft:stone"},
                    "base": {"type":"item","id":"minecraft:diamond"},
                    "addition": {"type":"choice","alternatives":[
                      {"type":"item","id":"minecraft:iron_ingot"},
                      {"type":"item","id":"minecraft:gold_ingot"},
                      {"type":"item","id":"minecraft:netherite_ingot"}
                    ]},
                    "result": {"itemId":"minecraft:diamond","count":1}
                  }]
                }
                """);

        RevisionDecodeResult result = this.codec.decode(revision(snapshot, snapshotHash(snapshot)));

        assertThat(result).isInstanceOfSatisfying(RevisionDecodeResult.Success.class, success -> {
            CustomizationEntry.ExtremeSmithing smithing = (CustomizationEntry.ExtremeSmithing)
                    success.revision().snapshot().entries().values().iterator().next();
            assertThat(smithing.additions()).containsExactly(
                    item("minecraft:iron_ingot"),
                    item("minecraft:gold_ingot"),
                    item("minecraft:netherite_ingot"));
        });
    }

    @Test
    void rejectsChangedSnapshotContentWithTheOriginalChecksum() {
        JsonObject snapshot = workspace("""
                {
                  "schemaVersion": 1,
                  "entries": [{
                    "kind": "COMPRESSOR",
                    "id": "test:legacy",
                    "target": "KUBEJS",
                    "ingredient": {"type":"item","id":"minecraft:stone"},
                    "result": {"itemId":"minecraft:diamond","count":1},
                    "inputCount": 1,
                    "timeCost": 1
                  }]
                }
                """);
        String originalHash = snapshotHash(snapshot);
        snapshot.getAsJsonArray("entries").get(0).getAsJsonObject()
                .getAsJsonObject("ingredient").addProperty("id", "minecraft:dirt");

        RevisionDecodeResult result = this.codec.decode(revision(snapshot, originalHash));

        assertThat(result).isInstanceOfSatisfying(RevisionDecodeResult.Failure.class, failure -> {
            assertThat(failure.code()).isEqualTo("revision.snapshot.checksum");
            assertThat(failure.fieldPath()).isEqualTo("snapshotSha256");
        });
    }

    private static JsonObject workspace(String content) {
        return JsonParser.parseString(content).getAsJsonObject();
    }

    private static String snapshotHash(JsonObject snapshot) {
        return ContentHashes.sha256(JSON.toJson(snapshot) + "\n");
    }

    private static String revision(JsonObject snapshot, String snapshotHash) {
        JsonObject root = new JsonObject();
        root.addProperty("revisionSchemaVersion", RevisionCodec.CURRENT_SCHEMA_VERSION);
        root.addProperty("version", 4);
        root.addProperty("parentVersion", 3);
        root.addProperty("committedAt", "2026-07-23T16:40:06Z");
        root.addProperty("message", "legacy revision");
        JsonObject summary = new JsonObject();
        summary.addProperty("added", 0);
        summary.addProperty("modified", 1);
        summary.addProperty("removed", 0);
        root.add("summary", summary);
        root.addProperty("snapshotSha256", snapshotHash);
        root.add("snapshot", snapshot);
        root.add("artifacts", new JsonArray());
        root.add("deletedPaths", new JsonArray());
        return JSON.toJson(root) + "\n";
    }

    private static IngredientSpec.Item item(String id) {
        return new IngredientSpec.Item(ResourceLocation.tryParse(id));
    }
}
