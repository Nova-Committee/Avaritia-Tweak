package committee.nova.mods.avaritia_tweak.customization.diff;

import committee.nova.mods.avaritia_tweak.customization.model.CraftingTier;
import committee.nova.mods.avaritia_tweak.customization.model.CustomizationEntry;
import committee.nova.mods.avaritia_tweak.customization.model.IngredientSpec;
import committee.nova.mods.avaritia_tweak.customization.model.ItemStackSpec;
import committee.nova.mods.avaritia_tweak.customization.model.OutputTarget;
import committee.nova.mods.avaritia_tweak.customization.model.WorkspaceSnapshot;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WorkspaceDifferTest {
    @Test
    void reportsAddedModifiedAndRemovedEntriesWithFieldPaths() {
        CustomizationEntry removed = recipe("test:removed", OutputTarget.KUBEJS, 1);
        CustomizationEntry oldModified = recipe("test:modified", OutputTarget.KUBEJS, 1);
        CustomizationEntry newModified = recipe("test:modified", OutputTarget.CRAFTTWEAKER, 2,
                "调整后的主配方");
        CustomizationEntry added = recipe("test:added", OutputTarget.KUBEJS, 1);
        WorkspaceSnapshot before = WorkspaceSnapshot.empty().withEntry(removed).withEntry(oldModified);
        WorkspaceSnapshot after = WorkspaceSnapshot.empty().withEntry(newModified).withEntry(added);

        WorkspaceDiff diff = new WorkspaceDiffer().diff(before, after);

        assertThat(diff.count(ChangeType.ADDED)).isEqualTo(1);
        assertThat(diff.count(ChangeType.MODIFIED)).isEqualTo(1);
        assertThat(diff.count(ChangeType.REMOVED)).isEqualTo(1);
        assertThat(diff.entries()).filteredOn(change -> change.type() == ChangeType.MODIFIED)
                .singleElement().extracting(EntryChange::fields).asList()
                .extracting("fieldPath").contains("note", "target", "result.count");
    }

    private static CustomizationEntry recipe(String id, OutputTarget target, int count) {
        return recipe(id, target, count, "");
    }

    private static CustomizationEntry recipe(String id, OutputTarget target, int count, String note) {
        return new CustomizationEntry.ShapelessTable(ResourceLocation.tryParse(id), target, note,
                CraftingTier.SCULK, List.of(new IngredientSpec.Item(ResourceLocation.tryParse("minecraft:stone"))),
                new ItemStackSpec(ResourceLocation.tryParse("minecraft:diamond"), count));
    }
}
