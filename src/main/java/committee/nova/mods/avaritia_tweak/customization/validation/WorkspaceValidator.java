package committee.nova.mods.avaritia_tweak.customization.validation;

import committee.nova.mods.avaritia_tweak.customization.model.CraftingTier;
import committee.nova.mods.avaritia_tweak.customization.model.CustomizationEntry;
import committee.nova.mods.avaritia_tweak.customization.model.EntryKey;
import committee.nova.mods.avaritia_tweak.customization.model.IngredientSpec;
import committee.nova.mods.avaritia_tweak.customization.model.ItemStackSpec;
import committee.nova.mods.avaritia_tweak.customization.model.OutputTarget;
import committee.nova.mods.avaritia_tweak.customization.model.SingularityAction;
import committee.nova.mods.avaritia_tweak.customization.model.WorkspaceSnapshot;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;

public final class WorkspaceValidator {
    private final RegistryLookup registryLookup;

    public WorkspaceValidator(RegistryLookup registryLookup) {
        this.registryLookup = Objects.requireNonNull(registryLookup, "registryLookup");
    }

    public ValidationReport validate(WorkspaceSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        List<ValidationIssue> issues = new ArrayList<>();
        if (snapshot.schemaVersion() != WorkspaceSnapshot.CURRENT_SCHEMA_VERSION) {
            issues.add(ValidationIssue.error(null, "schemaVersion", "validation.schema.unsupported",
                    Integer.toString(snapshot.schemaVersion()),
                    Integer.toString(WorkspaceSnapshot.CURRENT_SCHEMA_VERSION)));
        }

        Map<ResourceLocation, List<EntryKey>> recipeIds = new HashMap<>();
        List<CustomizationEntry.SingularityDefinition> definitions = new ArrayList<>();
        List<CustomizationEntry.SingularityOperation> operations = new ArrayList<>();

        for (CustomizationEntry entry : snapshot.entries().values()) {
            validateTarget(entry, issues);
            validateEntry(entry, issues);
            if (entry.kind().isRecipe()) {
                recipeIds.computeIfAbsent(entry.id(), ignored -> new ArrayList<>()).add(entry.key());
            } else if (entry instanceof CustomizationEntry.SingularityDefinition definition) {
                definitions.add(definition);
            } else if (entry instanceof CustomizationEntry.SingularityOperation operation) {
                operations.add(operation);
            }
        }

        recipeIds.forEach((id, keys) -> {
            if (keys.size() > 1) {
                keys.forEach(key -> issues.add(ValidationIssue.error(key, "id",
                        "validation.recipe.id.duplicate", id.toString())));
            }
        });
        validateSingularityInteractions(definitions, operations, issues);
        return new ValidationReport(issues);
    }

    private void validateTarget(CustomizationEntry entry, List<ValidationIssue> issues) {
        if (!entry.target().supports(entry.kind())) {
            issues.add(ValidationIssue.error(entry.key(), "target", "validation.target.unsupported",
                    entry.target().name(), entry.kind().name()));
        }
        if (entry.target() == OutputTarget.CRAFTTWEAKER
                && containsComponents(entry)) {
            issues.add(ValidationIssue.error(entry.key(), "target",
                    "validation.target.components_unsupported"));
        }
    }

    private static boolean containsComponents(CustomizationEntry entry) {
        if (entry instanceof CustomizationEntry.ShapedTable shaped) {
            return shaped.ingredients().values().stream().anyMatch(WorkspaceValidator::containsComponents);
        }
        if (entry instanceof CustomizationEntry.NoConsumeCatalystShaped shaped) {
            return shaped.ingredients().values().stream().anyMatch(WorkspaceValidator::containsComponents);
        }
        if (entry instanceof CustomizationEntry.ShapelessTable shapeless) {
            return shapeless.ingredients().stream().anyMatch(WorkspaceValidator::containsComponents);
        }
        if (entry instanceof CustomizationEntry.Compressor compressor) {
            return containsComponents(compressor.ingredient());
        }
        if (entry instanceof CustomizationEntry.ExtremeSmithing smithing) {
            return containsComponents(smithing.template()) || containsComponents(smithing.base())
                    || smithing.additions().stream().anyMatch(WorkspaceValidator::containsComponents);
        }
        if (entry instanceof CustomizationEntry.InfinityCatalyst catalyst) {
            return catalyst.ingredients().stream().anyMatch(WorkspaceValidator::containsComponents);
        }
        if (entry instanceof CustomizationEntry.EternalSingularity eternal) {
            return eternal.ingredients().stream().anyMatch(WorkspaceValidator::containsComponents);
        }
        return entry instanceof CustomizationEntry.SingularityDefinition definition
                && containsComponents(definition.ingredient());
    }

    private static boolean containsComponents(IngredientSpec ingredient) {
        return ingredient instanceof IngredientSpec.Components
                || ingredient instanceof IngredientSpec.Choice choice
                && choice.alternatives().stream().anyMatch(WorkspaceValidator::containsComponents);
    }

    private void validateEntry(CustomizationEntry entry, List<ValidationIssue> issues) {
        if (entry instanceof CustomizationEntry.ShapedTable shaped) {
            validateShaped(shaped.key(), shaped.tier(), shaped.ingredients(), shaped.result(), issues);
        } else if (entry instanceof CustomizationEntry.NoConsumeCatalystShaped shaped) {
            validateShaped(shaped.key(), shaped.tier(), shaped.ingredients(), shaped.result(), issues);
        } else if (entry instanceof CustomizationEntry.ShapelessTable shapeless) {
            validateShapeless(shapeless, issues);
        } else if (entry instanceof CustomizationEntry.Compressor compressor) {
            validateIngredient(compressor.key(), "ingredient", compressor.ingredient(), issues);
            validateResult(compressor.key(), "result", compressor.result(), issues);
            positive(compressor.key(), "inputCount", compressor.inputCount(), issues);
            positive(compressor.key(), "timeCost", compressor.timeCost(), issues);
        } else if (entry instanceof CustomizationEntry.ExtremeSmithing smithing) {
            validateIngredient(smithing.key(), "template", smithing.template(), issues);
            validateIngredient(smithing.key(), "base", smithing.base(), issues);
            validateIngredientList(smithing.key(), "additions", smithing.additions(), issues);
            validateResult(smithing.key(), "result", smithing.result(), issues);
        } else if (entry instanceof CustomizationEntry.InfinityCatalyst catalyst) {
            validateIngredientList(catalyst.key(), "ingredients", catalyst.ingredients(), issues);
            if (catalyst.group().isBlank()) {
                issues.add(ValidationIssue.error(catalyst.key(), "group", "validation.required"));
            }
            positive(catalyst.key(), "count", catalyst.count(), issues);
        } else if (entry instanceof CustomizationEntry.EternalSingularity eternal) {
            validateIngredientList(eternal.key(), "ingredients", eternal.ingredients(), issues);
            positive(eternal.key(), "count", eternal.count(), issues);
        } else if (entry instanceof CustomizationEntry.SingularityDefinition definition) {
            validateSingularityDefinition(definition, issues);
        } else if (entry instanceof CustomizationEntry.SingularityOperation operation) {
            validateSingularityOperation(operation, issues);
        }
    }

    private void validateShaped(EntryKey key, CraftingTier tier,
                                SortedMap<Integer, IngredientSpec> ingredients, ItemStackSpec result,
                                List<ValidationIssue> issues) {
        if (ingredients.isEmpty()) {
            issues.add(ValidationIssue.error(key, "ingredients", "validation.ingredients.empty"));
        }
        ingredients.forEach((slot, ingredient) -> {
            if (slot < 0 || slot >= tier.capacity()) {
                issues.add(ValidationIssue.error(key, "ingredients[" + slot + "]",
                        "validation.grid.slot.out_of_bounds", Integer.toString(slot),
                        Integer.toString(tier.capacity())));
            } else {
                validateIngredient(key, "ingredients[" + slot + "]", ingredient, issues);
            }
        });
        validateResult(key, "result", result, issues);
    }

    private void validateShapeless(CustomizationEntry.ShapelessTable entry, List<ValidationIssue> issues) {
        validateIngredientList(entry.key(), "ingredients", entry.ingredients(), issues);
        if (entry.ingredients().size() > entry.tier().capacity()) {
            issues.add(ValidationIssue.error(entry.key(), "ingredients", "validation.ingredients.too_many",
                    Integer.toString(entry.ingredients().size()), Integer.toString(entry.tier().capacity())));
        }
        validateResult(entry.key(), "result", entry.result(), issues);
    }

    private void validateSingularityDefinition(CustomizationEntry.SingularityDefinition entry,
                                               List<ValidationIssue> issues) {
        if (entry.displayName().isBlank()) {
            issues.add(ValidationIssue.error(entry.key(), "displayName", "validation.required"));
        }
        rgb(entry.key(), "overlayColor", entry.overlayColor(), issues);
        rgb(entry.key(), "underlayColor", entry.underlayColor(), issues);
        positive(entry.key(), "count", entry.count(), issues);
        positive(entry.key(), "timeCost", entry.timeCost(), issues);
        validateIngredient(entry.key(), "ingredient", entry.ingredient(), issues);
    }

    private void validateSingularityOperation(CustomizationEntry.SingularityOperation entry,
                                              List<ValidationIssue> issues) {
        if (entry.action().requiresTarget() != entry.singularityId().isPresent()) {
            issues.add(ValidationIssue.error(entry.key(), "singularityId",
                    entry.action().requiresTarget()
                            ? "validation.singularity.target.required"
                            : "validation.singularity.target.forbidden",
                    entry.action().name()));
        }
    }

    private void validateIngredientList(EntryKey key, String field, List<IngredientSpec> ingredients,
                                        List<ValidationIssue> issues) {
        if (ingredients.isEmpty()) {
            issues.add(ValidationIssue.error(key, field, "validation.ingredients.empty"));
        }
        for (int index = 0; index < ingredients.size(); index++) {
            validateIngredient(key, field + "[" + index + "]", ingredients.get(index), issues);
        }
    }

    private void validateIngredient(EntryKey key, String field, IngredientSpec ingredient,
                                    List<ValidationIssue> issues) {
        if (ingredient instanceof IngredientSpec.Choice choice) {
            for (int index = 0; index < choice.alternatives().size(); index++) {
                validateIngredient(key, field + ".alternatives[" + index + "]",
                        choice.alternatives().get(index), issues);
            }
        } else if (ingredient instanceof IngredientSpec.Item item) {
            if (this.registryLookup.itemMaxStackSize(item.itemId()).isEmpty()) {
                issues.add(ValidationIssue.error(key, field + ".itemId", "validation.item.missing",
                        item.itemId().toString()));
            }
        } else if (ingredient instanceof IngredientSpec.Components components) {
            if (this.registryLookup.itemMaxStackSize(components.itemId()).isEmpty()) {
                issues.add(ValidationIssue.error(key, field + ".itemId", "validation.item.missing",
                        components.itemId().toString()));
            }
        } else if (ingredient instanceof IngredientSpec.Tag tag
                && !this.registryLookup.itemTagExists(tag.tagId())) {
            issues.add(ValidationIssue.error(key, field + ".tagId", "validation.tag.missing",
                    tag.tagId().toString()));
        }
    }

    private void validateResult(EntryKey key, String field, ItemStackSpec result,
                                List<ValidationIssue> issues) {
        var maximum = this.registryLookup.itemMaxStackSize(result.itemId());
        if (maximum.isEmpty()) {
            issues.add(ValidationIssue.error(key, field + ".itemId", "validation.item.missing",
                    result.itemId().toString()));
        }
        positive(key, field + ".count", result.count(), issues);
        if (maximum.isPresent() && result.count() > maximum.getAsInt()) {
            issues.add(ValidationIssue.error(key, field + ".count", "validation.stack.too_large",
                    Integer.toString(result.count()), Integer.toString(maximum.getAsInt())));
        }
    }

    private static void positive(EntryKey key, String field, int value, List<ValidationIssue> issues) {
        if (value <= 0) {
            issues.add(ValidationIssue.error(key, field, "validation.number.positive",
                    Integer.toString(value)));
        }
    }

    private static void rgb(EntryKey key, String field, int value, List<ValidationIssue> issues) {
        if (value < 0 || value > 0xFFFFFF) {
            issues.add(ValidationIssue.error(key, field, "validation.color.rgb",
                    Integer.toString(value)));
        }
    }

    private static void validateSingularityInteractions(
            List<CustomizationEntry.SingularityDefinition> definitions,
            List<CustomizationEntry.SingularityOperation> operations,
            List<ValidationIssue> issues) {
        Map<OperationIdentity, List<CustomizationEntry.SingularityOperation>> identical = new HashMap<>();
        for (CustomizationEntry.SingularityOperation operation : operations) {
            OperationIdentity identity = new OperationIdentity(operation.target(), operation.action(),
                    operation.singularityId());
            identical.computeIfAbsent(identity, ignored -> new ArrayList<>()).add(operation);
        }
        identical.values().stream().filter(entries -> entries.size() > 1).forEach(entries ->
                entries.forEach(entry -> issues.add(ValidationIssue.error(entry.key(), "action",
                        "validation.singularity.operation.duplicate", entry.action().name(),
                        entry.singularityId().map(ResourceLocation::toString).orElse("*")))));

        for (CustomizationEntry.SingularityDefinition definition : definitions) {
            for (CustomizationEntry.SingularityOperation operation : operations) {
                if (operation.singularityId().filter(definition.id()::equals).isEmpty()) {
                    continue;
                }
                if (definition.target() == operation.target()) {
                    issues.add(ValidationIssue.error(operation.key(), "singularityId",
                            "validation.singularity.definition_operation.conflict",
                            definition.id().toString(), definition.target().name()));
                } else {
                    issues.add(ValidationIssue.warning(operation.key(), "target",
                            "validation.singularity.cross_script.priority",
                            definition.id().toString(), definition.target().name(), operation.target().name()));
                }
            }
        }

        Map<ResourceLocation, Set<OutputTarget>> targetsBySingularity = new HashMap<>();
        for (CustomizationEntry.SingularityOperation operation : operations) {
            operation.singularityId().ifPresent(id -> targetsBySingularity
                    .computeIfAbsent(id, ignored -> new HashSet<>()).add(operation.target()));
        }
        targetsBySingularity.forEach((id, targets) -> {
            if (targets.size() > 1) {
                operations.stream()
                        .filter(operation -> operation.singularityId().filter(id::equals).isPresent())
                        .forEach(operation -> issues.add(ValidationIssue.warning(operation.key(), "target",
                                "validation.singularity.cross_script.priority", id.toString(),
                                targets.toString())));
            }
        });

        for (SingularityAction globalAction : List.of(
                SingularityAction.REMOVE_ALL, SingularityAction.REMOVE_ALL_RECIPES)) {
            Set<OutputTarget> targets = new HashSet<>();
            operations.stream().filter(operation -> operation.action() == globalAction)
                    .forEach(operation -> targets.add(operation.target()));
            if (targets.size() > 1) {
                operations.stream().filter(operation -> operation.action() == globalAction)
                        .forEach(operation -> issues.add(ValidationIssue.warning(operation.key(), "target",
                                "validation.singularity.global.cross_script", globalAction.name())));
            }
        }
    }

    private record OperationIdentity(OutputTarget target, SingularityAction action,
                                     Optional<ResourceLocation> singularityId) {
    }
}
