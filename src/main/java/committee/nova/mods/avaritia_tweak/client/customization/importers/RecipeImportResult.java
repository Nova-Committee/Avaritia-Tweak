package committee.nova.mods.avaritia_tweak.client.customization.importers;

import committee.nova.mods.avaritia_tweak.customization.model.CustomizationEntry;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

public sealed interface RecipeImportResult permits RecipeImportResult.Success, RecipeImportResult.Failure {
    record Success(CustomizationEntry entry) implements RecipeImportResult {
        public Success {
            Objects.requireNonNull(entry, "entry");
        }
    }

    record Failure(ResourceLocation recipeId, String fieldPath, String code, String message)
            implements RecipeImportResult {
        public Failure {
            Objects.requireNonNull(recipeId, "recipeId");
            Objects.requireNonNull(fieldPath, "fieldPath");
            Objects.requireNonNull(code, "code");
            Objects.requireNonNull(message, "message");
        }
    }
}
