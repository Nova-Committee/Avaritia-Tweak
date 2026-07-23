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
                OutputTarget.KUBEJS, CraftingTier.SCULK,
                List.of(new IngredientSpec.Item(id("minecraft:stone"), Optional.of(nbt))),
                new ItemStackSpec(id("minecraft:diamond"), 1, Optional.of(nbt)));
        WorkspaceSnapshot snapshot = WorkspaceSnapshot.empty().withEntry(recipe);

        String encoded = this.codec.encode(snapshot);
        WorkspaceDecodeResult result = this.codec.decode(encoded);

        assertThat(result).isInstanceOf(WorkspaceDecodeResult.Success.class);
        WorkspaceSnapshot decoded = ((WorkspaceDecodeResult.Success) result).snapshot();
        assertThat(decoded).isEqualTo(snapshot);
        assertThat(this.codec.encode(decoded)).isEqualTo(encoded);
        assertThat(encoded).contains("strictNbt", "a:", "z:2");
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
