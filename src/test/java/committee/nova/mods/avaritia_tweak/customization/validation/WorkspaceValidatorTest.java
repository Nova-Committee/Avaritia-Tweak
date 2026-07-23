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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;

class WorkspaceValidatorTest {
    private TestRegistryLookup registries;
    private WorkspaceValidator validator;

    @BeforeEach
    void setUp() {
        this.registries = new TestRegistryLookup();
        this.registries.item("minecraft:stone", 64)
                .item("minecraft:diamond", 64)
                .item("minecraft:netherite_ingot", 64)
                .tag("forge:ingots/iron");
        this.validator = new WorkspaceValidator(this.registries);
    }

    @Test
    void acceptsEverySupportedEntryKind() {
        WorkspaceSnapshot snapshot = snapshot(
                new CustomizationEntry.ShapedTable(id("test:shaped"), OutputTarget.KUBEJS,
                        CraftingTier.SCULK, new TreeMap<>(Map.of(0, item("minecraft:stone"))), result()),
                new CustomizationEntry.NoConsumeCatalystShaped(id("test:keep_catalyst"), OutputTarget.KUBEJS,
                        CraftingTier.SCULK, new TreeMap<>(Map.of(0, item("minecraft:stone"))), result()),
                new CustomizationEntry.ShapelessTable(id("test:shapeless"), OutputTarget.CRAFTTWEAKER,
                        CraftingTier.NETHER, List.of(tag("forge:ingots/iron")), result()),
                new CustomizationEntry.Compressor(id("test:compressor"), OutputTarget.KUBEJS,
                        item("minecraft:stone"), result(), 1000, 240),
                new CustomizationEntry.ExtremeSmithing(id("test:smithing"), OutputTarget.CRAFTTWEAKER,
                        item("minecraft:stone"), item("minecraft:diamond"),
                        item("minecraft:netherite_ingot"), result()),
                new CustomizationEntry.InfinityCatalyst(id("test:catalyst"), OutputTarget.KUBEJS,
                        "default", List.of(item("minecraft:stone")), 1),
                new CustomizationEntry.EternalSingularity(id("test:eternal"), OutputTarget.CRAFTTWEAKER,
                        List.of(item("minecraft:diamond")), 1),
                new CustomizationEntry.SingularityDefinition(id("test:singularity"), OutputTarget.DATAPACK,
                        "singularity.test", 0x123456, 0x654321, 1000, 240,
                        tag("forge:ingots/iron"), true, true),
                new CustomizationEntry.SingularityOperation(id("test:remove_old"), OutputTarget.KUBEJS,
                        SingularityAction.REMOVE, Optional.of(id("test:old")))
        );

        ValidationReport report = this.validator.validate(snapshot);

        assertThat(report.isValid()).isTrue();
        assertThat(report.errors()).isEmpty();
    }

    @Test
    void catalystPreservingRecipeOnlyAcceptsKubeJsTarget() {
        CustomizationEntry recipe = new CustomizationEntry.NoConsumeCatalystShaped(
                id("test:keep_catalyst"), OutputTarget.CRAFTTWEAKER, CraftingTier.SCULK,
                new TreeMap<>(Map.of(0, item("minecraft:stone"))), result());

        assertThat(this.validator.validate(snapshot(recipe)).errors())
                .extracting(ValidationIssue::fieldPath, ValidationIssue::messageKey)
                .contains(org.assertj.core.groups.Tuple.tuple(
                        "target", "validation.target.unsupported"));
    }

    @Test
    void rejectsDuplicateRecipeIdsAcrossRecipeTypes() {
        ResourceLocation duplicate = id("test:duplicate");
        WorkspaceSnapshot snapshot = snapshot(
                new CustomizationEntry.ShapedTable(duplicate, OutputTarget.KUBEJS, CraftingTier.SCULK,
                        new TreeMap<>(Map.of(0, item("minecraft:stone"))), result()),
                new CustomizationEntry.ShapelessTable(duplicate, OutputTarget.CRAFTTWEAKER, CraftingTier.SCULK,
                        List.of(item("minecraft:stone")), result())
        );

        assertThat(this.validator.validate(snapshot).errors())
                .filteredOn(issue -> issue.messageKey().equals("validation.recipe.id.duplicate"))
                .hasSize(2);
    }

    @Test
    void reportsGridCountsTargetsAndMissingRegistryObjects() {
        TreeMap<Integer, IngredientSpec> grid = new TreeMap<>();
        grid.put(9, item("minecraft:missing"));
        WorkspaceSnapshot snapshot = snapshot(
                new CustomizationEntry.ShapedTable(id("test:bad"), OutputTarget.DATAPACK,
                        CraftingTier.SCULK, grid,
                        new ItemStackSpec(id("minecraft:diamond"), 65)),
                new CustomizationEntry.Compressor(id("test:bad_compressor"), OutputTarget.KUBEJS,
                        item("minecraft:stone"), result(), 0, -1)
        );

        assertThat(this.validator.validate(snapshot).errors())
                .extracting(ValidationIssue::messageKey)
                .contains("validation.target.unsupported", "validation.grid.slot.out_of_bounds",
                        "validation.stack.too_large", "validation.number.positive");
    }

    @Test
    void validatesEveryChoiceAlternativeAtItsOwnFieldPath() {
        IngredientSpec.Choice choice = new IngredientSpec.Choice(List.of(
                item("minecraft:stone"), item("minecraft:missing"), tag("forge:missing")));
        WorkspaceSnapshot snapshot = snapshot(new CustomizationEntry.Compressor(
                id("test:choice"), OutputTarget.KUBEJS, choice, result(), 1, 1));

        assertThat(this.validator.validate(snapshot).errors())
                .extracting(ValidationIssue::fieldPath, ValidationIssue::messageKey)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple(
                                "ingredient.alternatives[1].itemId", "validation.item.missing"),
                        org.assertj.core.groups.Tuple.tuple(
                                "ingredient.alternatives[2].tagId", "validation.tag.missing"));
    }

    @Test
    void rejectsCrossVersionDataComponentPredicates() {
        IngredientSpec.Components components = new IngredientSpec.Components(id("minecraft:stone"),
                "{\"minecraft:custom_data\":{\"mode\":\"exact\"}}", false);
        CustomizationEntry recipe = new CustomizationEntry.Compressor(
                id("test:components"), OutputTarget.KUBEJS, components, result(), 1, 1);

        assertThat(this.validator.validate(snapshot(recipe)).errors())
                .extracting(ValidationIssue::messageKey)
                .contains("validation.target.components_version_unsupported");
    }

    @Test
    void rejectsSameTargetSingularityConflictAndWarnsAcrossScripts() {
        CustomizationEntry.SingularityDefinition definition = new CustomizationEntry.SingularityDefinition(
                id("test:iron"), OutputTarget.KUBEJS, "singularity.iron", 0, 0,
                1000, 240, item("minecraft:stone"), true, true);
        CustomizationEntry.SingularityOperation sameTarget = new CustomizationEntry.SingularityOperation(
                id("test:remove_iron"), OutputTarget.KUBEJS, SingularityAction.REMOVE,
                Optional.of(definition.id()));
        CustomizationEntry.SingularityOperation otherTarget = new CustomizationEntry.SingularityOperation(
                id("test:disable_iron_recipe"), OutputTarget.CRAFTTWEAKER, SingularityAction.REMOVE_RECIPE,
                Optional.of(definition.id()));

        ValidationReport report = this.validator.validate(snapshot(definition, sameTarget, otherTarget));

        assertThat(report.errors()).extracting(ValidationIssue::messageKey)
                .contains("validation.singularity.definition_operation.conflict");
        assertThat(report.warnings()).extracting(ValidationIssue::messageKey)
                .contains("validation.singularity.cross_script.priority");
    }

    @Test
    void rejectsDuplicateGlobalOperationsAndMalformedTargets() {
        WorkspaceSnapshot snapshot = snapshot(
                new CustomizationEntry.SingularityOperation(id("test:all_a"), OutputTarget.KUBEJS,
                        SingularityAction.REMOVE_ALL, Optional.empty()),
                new CustomizationEntry.SingularityOperation(id("test:all_b"), OutputTarget.KUBEJS,
                        SingularityAction.REMOVE_ALL, Optional.empty()),
                new CustomizationEntry.SingularityOperation(id("test:target_missing"), OutputTarget.KUBEJS,
                        SingularityAction.REMOVE, Optional.empty())
        );

        assertThat(this.validator.validate(snapshot).errors()).extracting(ValidationIssue::messageKey)
                .contains("validation.singularity.operation.duplicate",
                        "validation.singularity.target.required");
    }

    private static WorkspaceSnapshot snapshot(CustomizationEntry... entries) {
        TreeMap<EntryKey, CustomizationEntry> indexed = new TreeMap<>();
        Arrays.stream(entries).forEach(entry -> indexed.put(entry.key(), entry));
        return new WorkspaceSnapshot(WorkspaceSnapshot.CURRENT_SCHEMA_VERSION, indexed);
    }

    private static IngredientSpec.Item item(String value) {
        return new IngredientSpec.Item(id(value));
    }

    private static IngredientSpec.Tag tag(String value) {
        return new IngredientSpec.Tag(id(value));
    }

    private static ItemStackSpec result() {
        return new ItemStackSpec(id("minecraft:diamond"), 1);
    }

    private static ResourceLocation id(String value) {
        return ResourceLocation.tryParse(value);
    }

    private static final class TestRegistryLookup implements RegistryLookup {
        private final Map<ResourceLocation, Integer> items = new HashMap<>();
        private final Set<ResourceLocation> tags = new HashSet<>();

        TestRegistryLookup item(String id, int maxStackSize) {
            this.items.put(ResourceLocation.tryParse(id), maxStackSize);
            return this;
        }

        TestRegistryLookup tag(String id) {
            this.tags.add(ResourceLocation.tryParse(id));
            return this;
        }

        @Override
        public OptionalInt itemMaxStackSize(ResourceLocation itemId) {
            Integer maximum = this.items.get(itemId);
            return maximum == null ? OptionalInt.empty() : OptionalInt.of(maximum);
        }

        @Override
        public boolean itemTagExists(ResourceLocation tagId) {
            return this.tags.contains(tagId);
        }
    }
}
