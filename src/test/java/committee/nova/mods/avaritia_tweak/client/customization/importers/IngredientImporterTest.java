package committee.nova.mods.avaritia_tweak.client.customization.importers;

import com.google.gson.JsonParser;
import committee.nova.mods.avaritia_tweak.customization.model.IngredientSpec;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.crafting.CompoundIngredient;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IngredientImporterTest {
    private final IngredientImporter importer = new IngredientImporter();

    @Test
    void importsSingleItemTagAndStrictNbtWithoutExpandingCandidates() {
        IngredientImportResult item = importJson("""
                {"item":"minecraft:stone"}
                """);
        IngredientImportResult tag = importJson("""
                {"tag":"forge:ingots/iron"}
                """);
        IngredientImportResult strict = importJson("""
                {"type":"forge:nbt","item":"minecraft:diamond","nbt":"{display:{Name:'strict'}}"}
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
    void importsRuntimeForgeCompoundIngredient() {
        Ingredient compound = CompoundIngredient.of(
                serializedIngredient("{\"item\":\"minecraft:stone\"}"),
                serializedIngredient("{\"item\":\"minecraft:dirt\"}"));

        IngredientImportResult result = this.importer.importIngredient(compound, "addition");

        assertThat(result).isInstanceOfSatisfying(IngredientImportResult.Success.class, success ->
                assertThat(((IngredientSpec.Choice) success.ingredient()).alternatives())
                        .containsExactly(new IngredientSpec.Item(net.minecraft.resources.ResourceLocation.tryParse(
                                        "minecraft:stone")),
                                new IngredientSpec.Item(net.minecraft.resources.ResourceLocation.tryParse(
                                        "minecraft:dirt"))));
    }

    @Test
    void importsMultipleChoiceAndStillRejectsUnknownCustomIngredientsWithFieldLocation() {
        IngredientImportResult multiple = importJson("""
                [{"item":"minecraft:stone"},{"item":"minecraft:dirt"}]
                """);
        IngredientImportResult custom = importJson("""
                {"type":"example:custom","value":"anything"}
                """);
        IngredientImportResult malformed = importJson("""
                {"item":{"not":"a string"}}
                """);

        assertThat(multiple).isInstanceOfSatisfying(IngredientImportResult.Success.class, success ->
                assertThat(((IngredientSpec.Choice) success.ingredient()).alternatives())
                        .containsExactly(new IngredientSpec.Item(net.minecraft.resources.ResourceLocation.tryParse(
                                        "minecraft:stone")),
                                new IngredientSpec.Item(net.minecraft.resources.ResourceLocation.tryParse(
                                        "minecraft:dirt"))));
        assertThat(custom).isInstanceOfSatisfying(IngredientImportResult.Failure.class, failure -> {
            assertThat(failure.fieldPath()).isEqualTo("ingredients[3]");
            assertThat(failure.code()).isEqualTo("ingredient.custom.unsupported");
        });
        assertThat(malformed).isInstanceOfSatisfying(IngredientImportResult.Failure.class,
                failure -> assertThat(failure.code()).isEqualTo("ingredient.id.invalid"));
    }

    private IngredientImportResult importJson(String json) {
        return this.importer.importIngredient(serializedIngredient(json), "ingredients[3]");
    }

    private static Ingredient serializedIngredient(String json) {
        Ingredient ingredient = mock(Ingredient.class);
        when(ingredient.toJson()).thenReturn(JsonParser.parseString(json));
        return ingredient;
    }
}
