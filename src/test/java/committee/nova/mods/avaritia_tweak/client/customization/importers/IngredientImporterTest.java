package committee.nova.mods.avaritia_tweak.client.customization.importers;

import com.google.gson.JsonParser;
import committee.nova.mods.avaritia_tweak.customization.model.IngredientSpec;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.crafting.DataComponentIngredient;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IngredientImporterTest {
    private final IngredientImporter importer = new IngredientImporter();

    @Test
    void importsSingleItemTagAndNeoForgeStrictComponentsWithoutExpandingCandidates() {
        IngredientImportResult item = importJson("""
                {"item":"minecraft:stone"}
                """);
        IngredientImportResult tag = importJson("""
                {"tag":"forge:ingots/iron"}
                """);
        IngredientImportResult strict = importJson("""
                {"type":"neoforge:components","items":"minecraft:diamond","components":{"minecraft:custom_data":"{display:{Name:'strict'}}"},"strict":true}
                """);

        assertThat(((IngredientImportResult.Success) item).ingredient())
                .isEqualTo(new IngredientSpec.Item(
                        net.minecraft.resources.ResourceLocation.tryParse("minecraft:stone")));
        assertThat(((IngredientImportResult.Success) tag).ingredient())
                .isEqualTo(new IngredientSpec.Tag(
                        net.minecraft.resources.ResourceLocation.tryParse("forge:ingots/iron")));
        IngredientSpec.Item strictItem = (IngredientSpec.Item)
                ((IngredientImportResult.Success) strict).ingredient();
        assertThat(strictItem.itemId().toString()).isEqualTo("minecraft:diamond");
        assertThat(strictItem.strictNbt()).isPresent();
        assertThat(strictItem.strictNbt().orElseThrow().getCompound("display").getString("Name"))
                .isEqualTo("strict");
    }

    @Test
    void keepsLegacyForgeNbtImportCompatibility() {
        IngredientImportResult strict = importJson("""
                {"type":"forge:nbt","item":"minecraft:diamond","nbt":"{legacy:1b}"}
                """);

        IngredientSpec.Item strictItem = (IngredientSpec.Item)
                ((IngredientImportResult.Success) strict).ingredient();
        assertThat(strictItem.strictNbt().orElseThrow().getBoolean("legacy")).isTrue();
    }

    @Test
    void importsIngredientProducedByNeoForgeRuntimeCodec() {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("mode", "runtime");
        Ingredient encoded = DataComponentIngredient.of(true, DataComponents.CUSTOM_DATA,
                CustomData.of(nbt), Items.DIAMOND);

        IngredientImportResult result = this.importer.importIngredient(encoded, "ingredient");

        assertThat(result).isInstanceOfSatisfying(IngredientImportResult.Success.class, success -> {
            IngredientSpec.Item item = (IngredientSpec.Item) success.ingredient();
            assertThat(item.itemId().toString()).isEqualTo("minecraft:diamond");
            assertThat(item.strictNbt().orElseThrow().getString("mode")).isEqualTo("runtime");
        });
    }

    @Test
    void rejectsMultipleChoiceAndUnknownCustomIngredientsWithFieldLocation() {
        IngredientImportResult multiple = importJson("""
                [{"item":"minecraft:stone"},{"item":"minecraft:dirt"}]
                """);
        IngredientImportResult custom = importJson("""
                {"type":"example:custom","value":"anything"}
                """);
        IngredientImportResult malformed = importJson("""
                {"item":{"not":"a string"}}
                """);
        IngredientImportResult partialComponents = importJson("""
                {"type":"neoforge:components","items":"minecraft:stone","components":{"minecraft:custom_data":"{value:1}"}}
                """);

        assertThat(multiple).isInstanceOfSatisfying(IngredientImportResult.Failure.class, failure -> {
            assertThat(failure.fieldPath()).isEqualTo("ingredients[3]");
            assertThat(failure.code()).isEqualTo("ingredient.or.unsupported");
        });
        assertThat(custom).isInstanceOfSatisfying(IngredientImportResult.Failure.class, failure -> {
            assertThat(failure.fieldPath()).isEqualTo("ingredients[3]");
            assertThat(failure.code()).isEqualTo("ingredient.custom.unsupported");
        });
        assertThat(malformed).isInstanceOfSatisfying(IngredientImportResult.Failure.class,
                failure -> assertThat(failure.code()).isEqualTo("ingredient.id.invalid"));
        assertThat(partialComponents).isInstanceOfSatisfying(IngredientImportResult.Failure.class,
                failure -> assertThat(failure.code()).isEqualTo("ingredient.components.unsupported"));
    }

    private IngredientImportResult importJson(String json) {
        return this.importer.importSerialized(JsonParser.parseString(json), "ingredients[3]");
    }
}
