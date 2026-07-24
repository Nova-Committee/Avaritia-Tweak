package committee.nova.mods.avaritia_tweak.customization.history;

import committee.nova.mods.avaritia_tweak.customization.model.CraftingTier;
import committee.nova.mods.avaritia_tweak.customization.model.CustomizationEntry;
import committee.nova.mods.avaritia_tweak.customization.model.IngredientSpec;
import committee.nova.mods.avaritia_tweak.customization.model.ItemStackSpec;
import committee.nova.mods.avaritia_tweak.customization.model.OutputTarget;
import committee.nova.mods.avaritia_tweak.customization.model.WorkspaceSnapshot;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;

class WorkspaceCodecTest {
    private final WorkspaceCodec codec = new WorkspaceCodec();

    @Test
    void roundTripsNbtAndKeepsEncodingDeterministic() {
        CompoundTag nbt = new CompoundTag();
        nbt.putInt("z", 2);
        nbt.putString("a", "value");
        CustomizationEntry recipe = new CustomizationEntry.ShapelessTable(id("test:recipe"),
                OutputTarget.KUBEJS, "核心材料替换", CraftingTier.SCULK,
                List.of(new IngredientSpec.Item(id("minecraft:stone"), Optional.of(nbt))),
                new ItemStackSpec(id("minecraft:diamond"), 1, Optional.of(nbt)));
        WorkspaceSnapshot snapshot = WorkspaceSnapshot.empty().withEntry(recipe);

        String encoded = this.codec.encode(snapshot);
        WorkspaceDecodeResult result = this.codec.decode(encoded);

        assertThat(result).isInstanceOf(WorkspaceDecodeResult.Success.class);
        WorkspaceSnapshot decoded = ((WorkspaceDecodeResult.Success) result).snapshot();
        assertThat(decoded).isEqualTo(snapshot);
        assertThat(this.codec.encode(decoded)).isEqualTo(encoded);
        assertThat(encoded).contains("strictNbt", "a:", "z:2", "核心材料替换");
    }

    @Test
    void roundTripsChoiceIngredientsWithoutChangingTheWorkspaceSchema() {
        IngredientSpec.Choice choice = new IngredientSpec.Choice(List.of(
                new IngredientSpec.Item(id("minecraft:stone")),
                new IngredientSpec.Tag(id("forge:ingots/iron"))));
        CustomizationEntry recipe = new CustomizationEntry.Compressor(id("test:choice"),
                OutputTarget.KUBEJS, choice, new ItemStackSpec(id("minecraft:diamond"), 1), 1, 1);
        WorkspaceSnapshot snapshot = WorkspaceSnapshot.empty().withEntry(recipe);

        String encoded = this.codec.encode(snapshot);
        WorkspaceDecodeResult result = this.codec.decode(encoded);

        assertThat(result).isEqualTo(new WorkspaceDecodeResult.Success(snapshot));
        assertThat(encoded).contains("\"schemaVersion\": 1", "\"type\": \"choice\"",
                "\"alternatives\"");
    }

    @Test
    void roundTripsDataComponentPredicatesForCrossVersionCompatibility() {
        IngredientSpec.Components potion = new IngredientSpec.Components(id("minecraft:potion"),
                "{\"minecraft:potion_contents\":{\"potion\":\"minecraft:swiftness\"}}", false);
        CustomizationEntry recipe = new CustomizationEntry.ExtremeSmithing(id("test:horse_armor"),
                OutputTarget.KUBEJS, new IngredientSpec.Item(id("minecraft:stone")),
                new IngredientSpec.Item(id("minecraft:diamond_horse_armor")), List.of(potion,
                new IngredientSpec.Item(id("avaritia:enhancement_core")),
                new IngredientSpec.Item(id("minecraft:blue_ice"))),
                new ItemStackSpec(id("minecraft:diamond"), 1));
        WorkspaceSnapshot snapshot = WorkspaceSnapshot.empty().withEntry(recipe);

        String encoded = this.codec.encode(snapshot);

        assertThat(this.codec.decode(encoded)).isEqualTo(new WorkspaceDecodeResult.Success(snapshot));
        assertThat(encoded).contains("\"type\": \"components\"", "minecraft:potion_contents",
                "\"strict\": false", "\"additions\"");
    }

    @Test
    void migratesLegacySmithingAdditionChoiceToThreePhysicalInputs() {
        String content = """
                {
                  "schemaVersion": 1,
                  "entries": [{
                    "kind": "EXTREME_SMITHING",
                    "id": "test:legacy_smithing",
                    "target": "KUBEJS",
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
                """;

        WorkspaceDecodeResult.Success decoded = (WorkspaceDecodeResult.Success) this.codec.decode(content);
        CustomizationEntry.ExtremeSmithing smithing = (CustomizationEntry.ExtremeSmithing)
                decoded.snapshot().entries().values().iterator().next();

        assertThat(smithing.additions()).containsExactly(
                new IngredientSpec.Item(id("minecraft:iron_ingot")),
                new IngredientSpec.Item(id("minecraft:gold_ingot")),
                new IngredientSpec.Item(id("minecraft:netherite_ingot")));
        assertThat(this.codec.encode(decoded.snapshot()))
                .contains("\"additions\"")
                .doesNotContain("\"addition\":");
    }

    @Test
    void rejectsSmithingWorkspaceWithoutExactlyThreeAdditionInputs() {
        String content = """
                {
                  "schemaVersion": 1,
                  "entries": [{
                    "kind": "EXTREME_SMITHING",
                    "id": "test:invalid_smithing",
                    "target": "KUBEJS",
                    "template": {"type":"item","id":"minecraft:stone"},
                    "base": {"type":"item","id":"minecraft:diamond"},
                    "additions": [
                      {"type":"item","id":"minecraft:iron_ingot"},
                      {"type":"item","id":"minecraft:gold_ingot"}
                    ],
                    "result": {"itemId":"minecraft:diamond","count":1}
                  }]
                }
                """;

        assertThat(this.codec.decode(content)).isInstanceOfSatisfying(
                WorkspaceDecodeResult.Failure.class, failure -> {
                    assertThat(failure.code()).isEqualTo("workspace.smithing.additions.size");
                    assertThat(failure.fieldPath()).isEqualTo("entries[0].additions");
                });
    }

    @Test
    void decodesLegacyEntriesWithoutANoteAsBlank() {
        String content = """
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
                """;

        WorkspaceDecodeResult.Success decoded = (WorkspaceDecodeResult.Success) this.codec.decode(content);

        assertThat(decoded.snapshot().entries().values().iterator().next().note()).isEmpty();
    }

    @Test
    void roundTripsCatalystPreservingShapedRecipeWithoutSchemaMigration() {
        CustomizationEntry recipe = new CustomizationEntry.NoConsumeCatalystShaped(
                id("test:keep_catalyst"), OutputTarget.KUBEJS, CraftingTier.END,
                new TreeMap<>(java.util.Map.of(
                        0, new IngredientSpec.Item(id("avaritia:infinity_catalyst")),
                        48, new IngredientSpec.Item(id("minecraft:stone")))),
                new ItemStackSpec(id("minecraft:diamond"), 2));
        WorkspaceSnapshot snapshot = WorkspaceSnapshot.empty().withEntry(recipe);

        String encoded = this.codec.encode(snapshot);
        WorkspaceDecodeResult decoded = this.codec.decode(encoded);

        assertThat(decoded).isEqualTo(new WorkspaceDecodeResult.Success(snapshot));
        assertThat(encoded).contains("\"schemaVersion\": 1",
                "\"kind\": \"NO_CONSUME_CATALYST_SHAPED\"", "\"slot\": 48");
    }

    @Test
    void rejectsChoiceWithFewerThanTwoAlternativesAtThePreciseField() {
        String content = """
                {
                  "schemaVersion": 1,
                  "entries": [{
                    "kind": "COMPRESSOR",
                    "id": "test:choice",
                    "target": "KUBEJS",
                    "ingredient": {
                      "type": "choice",
                      "alternatives": [{"type":"item","id":"minecraft:stone"}]
                    },
                    "result": {"itemId":"minecraft:diamond","count":1},
                    "inputCount": 1,
                    "timeCost": 1
                  }]
                }
                """;

        WorkspaceDecodeResult result = this.codec.decode(content);

        assertThat(result).isInstanceOfSatisfying(WorkspaceDecodeResult.Failure.class, failure -> {
            assertThat(failure.code()).isEqualTo("workspace.ingredient.choice.size");
            assertThat(failure.fieldPath()).isEqualTo("entries[0].ingredient.alternatives");
        });
    }

    @Test
    void rejectsUnknownSchemaWithStructuredFailure() {
        WorkspaceDecodeResult result = this.codec.decode("{\"schemaVersion\":99,\"entries\":[]}");

        assertThat(result).isInstanceOf(WorkspaceDecodeResult.Failure.class);
        WorkspaceDecodeResult.Failure failure = (WorkspaceDecodeResult.Failure) result;
        assertThat(failure.code()).isEqualTo("workspace.schema.unsupported");
        assertThat(failure.fieldPath()).isEqualTo("schemaVersion");
    }

    @Test
    void rejectsInvalidSnbtWithoutProducingPartialWorkspace() {
        String content = """
                {
                  "schemaVersion": 1,
                  "entries": [{
                    "kind": "SHAPELESS_TABLE",
                    "id": "test:recipe",
                    "target": "KUBEJS",
                    "tier": "SCULK",
                    "ingredients": [{"type":"item","id":"minecraft:stone","strictNbt":"{"}],
                    "result": {"itemId":"minecraft:diamond","count":1}
                  }]
                }
                """;

        WorkspaceDecodeResult result = this.codec.decode(content);

        assertThat(result).isInstanceOf(WorkspaceDecodeResult.Failure.class);
        assertThat(((WorkspaceDecodeResult.Failure) result).code()).isEqualTo("workspace.nbt.invalid");
    }

    private static ResourceLocation id(String value) {
        return ResourceLocation.tryParse(value);
    }
}
