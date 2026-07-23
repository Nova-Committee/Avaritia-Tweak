package committee.nova.mods.avaritia_tweak.client.customization;

import committee.nova.mods.avaritia_tweak.customization.model.CustomizationEntry;
import committee.nova.mods.avaritia_tweak.customization.model.OutputTarget;
import committee.nova.mods.avaritia_tweak.customization.model.SingularityAction;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EntryNavigationTest {
    @Test
    void searchesIdsKindsAndTargetsInStableOrder() {
        CustomizationEntry beta = operation("test:beta", OutputTarget.KUBEJS);
        CustomizationEntry alpha = operation("test:alpha", OutputTarget.CRAFTTWEAKER, "主线配方");

        assertEquals(List.of(alpha, beta), EntryNavigation.search(List.of(beta, alpha), ""));
        assertEquals(List.of(alpha), EntryNavigation.search(List.of(beta, alpha), "ALPHA"));
        assertEquals(List.of(alpha), EntryNavigation.search(List.of(beta, alpha), "crafttweaker"));
        assertEquals(List.of(alpha), EntryNavigation.search(List.of(beta, alpha), "主线"));
        assertEquals(List.of(alpha, beta), EntryNavigation.search(List.of(beta, alpha), "operation"));
    }

    @Test
    void duplicatesIntoTheNextUnusedStableResourceId() throws EntryFormException {
        CustomizationEntry source = operation("test:alpha", OutputTarget.KUBEJS);
        List<CustomizationEntry> existing = List.of(
                source,
                operation("test:alpha_copy", OutputTarget.KUBEJS),
                operation("test:alpha_copy_2", OutputTarget.CRAFTTWEAKER));

        CustomizationEntry duplicate = EntryNavigation.duplicateForEditing(source, existing).build();

        assertEquals(id("test:alpha_copy_3"), duplicate.id());
        assertEquals(source.kind(), duplicate.kind());
        assertEquals(source.target(), duplicate.target());
        assertEquals(source.note(), duplicate.note());
        assertEquals(id("test:alpha"), source.id());
    }

    private static CustomizationEntry operation(String id, OutputTarget target) {
        return operation(id, target, "");
    }

    private static CustomizationEntry operation(String id, OutputTarget target, String note) {
        return new CustomizationEntry.SingularityOperation(id(id), target, note, SingularityAction.REMOVE,
                Optional.of(id("avaritia:coal")));
    }

    private static ResourceLocation id(String value) {
        return ResourceLocation.tryParse(value);
    }
}
