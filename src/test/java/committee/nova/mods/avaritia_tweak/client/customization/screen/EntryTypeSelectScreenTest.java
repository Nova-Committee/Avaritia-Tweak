package committee.nova.mods.avaritia_tweak.client.customization.screen;

import committee.nova.mods.avaritia_tweak.customization.model.EntryKind;
import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class EntryTypeSelectScreenTest {
    @Test
    void exposesEverySupportedKindInItsExpectedGroup() {
        assertThat(EntryTypeSelectScreen.recipeKinds())
                .isNotEmpty()
                .allMatch(EntryKind::isRecipe);
        assertThat(EntryTypeSelectScreen.singularityKinds())
                .isNotEmpty()
                .noneMatch(EntryKind::isRecipe);
        assertThat(Stream.concat(EntryTypeSelectScreen.recipeKinds().stream(),
                        EntryTypeSelectScreen.singularityKinds().stream()))
                .containsExactly(EntryKind.values());
    }

    @Test
    void minimumViewportKeepsGroupsAndActionsInsideTheFrame() {
        EntryTypeSelectScreen.Layout layout = EntryTypeSelectScreen.layout(320, 240);

        assertThat(layout.left()).isGreaterThanOrEqualTo(0);
        assertThat(layout.top()).isGreaterThanOrEqualTo(0);
        assertThat(layout.left() + layout.width()).isLessThanOrEqualTo(320);
        assertThat(layout.top() + layout.height()).isLessThanOrEqualTo(240);
        assertThat(layout.columnWidth()).isPositive();
        assertThat(layout.recipePanelBottom()).isLessThan(layout.singularityPanelTop());
        assertThat(layout.singularityPanelBottom()).isLessThan(layout.actionY());
        assertThat(layout.actionY() + 20).isLessThanOrEqualTo(layout.top() + layout.height());
    }
}
