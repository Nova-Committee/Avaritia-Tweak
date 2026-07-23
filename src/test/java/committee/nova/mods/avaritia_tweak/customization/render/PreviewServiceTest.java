package committee.nova.mods.avaritia_tweak.customization.render;

import committee.nova.mods.avaritia_tweak.customization.diff.ChangeType;
import committee.nova.mods.avaritia_tweak.customization.model.CustomizationEntry;
import committee.nova.mods.avaritia_tweak.customization.model.IngredientSpec;
import committee.nova.mods.avaritia_tweak.customization.model.OutputTarget;
import committee.nova.mods.avaritia_tweak.customization.model.WorkspaceSnapshot;
import committee.nova.mods.avaritia_tweak.customization.validation.RegistryLookup;
import committee.nova.mods.avaritia_tweak.customization.validation.WorkspaceValidator;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.Objects;
import java.util.OptionalInt;

import static org.assertj.core.api.Assertions.assertThat;

class PreviewServiceTest {
    @Test
    void retainsBaselineArtifactContentForReadOnlyDeletionPreview() {
        CustomizationEntry definition = new CustomizationEntry.SingularityDefinition(
                id("test:old"), OutputTarget.DATAPACK, "Old Singularity",
                0x112233, 0x445566, 100, 20,
                new IngredientSpec.Item(id("minecraft:stone")), true, true);
        WorkspaceSnapshot committed = WorkspaceSnapshot.empty().withEntry(definition);
        PreviewService service = new PreviewService(
                new WorkspaceValidator(new AcceptingRegistry()),
                WorkspaceRenderService.standard());

        PreviewResult preview = service.preview(committed, WorkspaceSnapshot.empty());
        ArtifactPath removedPath = new ArtifactPath(
                "avaritia_tweak_exports/avaritia_tweak_singularities/data/test/singularities/old.json");

        assertThat(preview.validation().isValid()).isTrue();
        assertThat(preview.baselinePlan()).isPresent();
        assertThat(preview.renderPlan()).isPresent();
        assertThat(preview.artifactChanges())
                .anySatisfy(change -> {
                    assertThat(change.path()).isEqualTo(removedPath);
                    assertThat(change.type()).isEqualTo(ChangeType.REMOVED);
                });
        assertThat(preview.baselinePlan().orElseThrow().artifacts().get(removedPath).text())
                .contains("Old Singularity");
        assertThat(preview.renderPlan().orElseThrow().artifacts()).doesNotContainKey(removedPath);
    }

    private static ResourceLocation id(String value) {
        return Objects.requireNonNull(ResourceLocation.tryParse(value));
    }

    private static final class AcceptingRegistry implements RegistryLookup {
        @Override
        public OptionalInt itemMaxStackSize(ResourceLocation itemId) {
            return OptionalInt.of(64);
        }

        @Override
        public boolean itemTagExists(ResourceLocation tagId) {
            return true;
        }
    }
}
