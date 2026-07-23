package committee.nova.mods.avaritia_tweak.client.customization.importers;

import committee.nova.mods.avaritia_tweak.customization.model.IngredientSpec;

import java.util.Objects;

public sealed interface IngredientImportResult
        permits IngredientImportResult.Success, IngredientImportResult.Failure {
    record Success(IngredientSpec ingredient) implements IngredientImportResult {
        public Success {
            Objects.requireNonNull(ingredient, "ingredient");
        }
    }

    record Failure(String fieldPath, String code, String message) implements IngredientImportResult {
        public Failure {
            Objects.requireNonNull(fieldPath, "fieldPath");
            Objects.requireNonNull(code, "code");
            Objects.requireNonNull(message, "message");
        }
    }
}
