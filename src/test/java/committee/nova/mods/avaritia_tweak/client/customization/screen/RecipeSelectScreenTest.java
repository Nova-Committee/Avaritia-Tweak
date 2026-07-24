package committee.nova.mods.avaritia_tweak.client.customization.screen;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.Objects;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RecipeSelectScreenTest {
    @Test
    void hidesOnlyGeneratedRecipeForRuntimeSingularity() {
        ResourceLocation singularityId = id("avaritia:obsidian");
        ResourceLocation generatedRecipeId = RecipeSelectScreen.generatedSingularityRecipeId(singularityId);
        Set<ResourceLocation> generatedRecipeIds = Set.of(generatedRecipeId);

        assertThat(generatedRecipeId).isEqualTo(id("avaritia:obsidian_singularity"));
        assertThat(RecipeSelectScreen.isGeneratedSingularityRecipe(
                generatedRecipeId, generatedRecipeIds)).isTrue();
        assertThat(RecipeSelectScreen.isGeneratedSingularityRecipe(
                singularityId, generatedRecipeIds)).isFalse();
        assertThat(RecipeSelectScreen.isGeneratedSingularityRecipe(
                id("example:ordinary_compressor"), generatedRecipeIds)).isFalse();
    }

    private static ResourceLocation id(String value) {
        return Objects.requireNonNull(ResourceLocation.tryParse(value));
    }
}
