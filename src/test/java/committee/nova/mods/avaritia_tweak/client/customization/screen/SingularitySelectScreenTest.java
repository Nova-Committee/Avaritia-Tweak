package committee.nova.mods.avaritia_tweak.client.customization.screen;

import committee.nova.mods.avaritia_tweak.customization.model.CustomizationEntry;
import committee.nova.mods.avaritia_tweak.customization.model.IngredientSpec;
import committee.nova.mods.avaritia_tweak.customization.model.OutputTarget;
import committee.nova.mods.avaritia_tweak.customization.model.WorkspaceSnapshot;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class SingularitySelectScreenTest {
    @Test
    void mergesEffectiveAndDraftSingularitiesWithDraftTakingPrecedence() {
        ResourceLocation coal = id("avaritia:coal");
        ResourceLocation iron = id("avaritia:iron");
        Map<ResourceLocation, SingularitySelectScreen.Choice> effective = Map.of(
                iron, new SingularitySelectScreen.Choice(iron, "effective.iron",
                        0x111111, 0x222222, SingularitySelectScreen.Source.EFFECTIVE),
                coal, new SingularitySelectScreen.Choice(coal, "effective.coal",
                        0x333333, 0x444444, SingularitySelectScreen.Source.EFFECTIVE));
        CustomizationEntry.SingularityDefinition stagedIron =
                new CustomizationEntry.SingularityDefinition(iron, OutputTarget.DATAPACK,
                        "draft.iron", 0xabcdef, 0x123456, 1000, 240,
                        new IngredientSpec.Item(id("minecraft:iron_ingot"), Optional.empty()),
                        true, true);
        WorkspaceSnapshot draft = WorkspaceSnapshot.empty().withEntry(stagedIron);

        var choices = SingularitySelectScreen.collectChoices(effective, draft);

        assertThat(choices).extracting(SingularitySelectScreen.Choice::id)
                .containsExactly(coal, iron);
        assertThat(choices.get(1).displayName()).isEqualTo("draft.iron");
        assertThat(choices.get(1).source()).isEqualTo(SingularitySelectScreen.Source.DRAFT);
        assertThat(choices.get(1).overlayColor()).isEqualTo(0xabcdef);
    }

    private static ResourceLocation id(String value) {
        return java.util.Objects.requireNonNull(ResourceLocation.tryParse(value));
    }
}
