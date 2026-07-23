package committee.nova.mods.avaritia_tweak.client.customization.screen;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TableSlotDragGestureTest {
    @Test
    void shortPressOnTheSourceRemainsANormalClick() {
        TableSlotDragGesture gesture = new TableSlotDragGesture();

        assertThat(gesture.begin(4, true, 1_000L)).isTrue();
        assertThat(gesture.release(4, 1_349L).action())
                .isEqualTo(TableSlotDragGesture.Action.CLICK);
        assertThat(gesture.armed()).isFalse();
    }

    @Test
    void holdThresholdEnablesSwapWithAnotherSlot() {
        TableSlotDragGesture gesture = new TableSlotDragGesture();
        gesture.begin(2, true, 5_000L);
        gesture.updateTarget(7);

        assertThat(gesture.active(5_349L)).isFalse();
        assertThat(gesture.active(5_350L)).isTrue();
        TableSlotDragGesture.Release release = gesture.release(7, 5_350L);
        assertThat(release.action()).isEqualTo(TableSlotDragGesture.Action.SWAP);
        assertThat(release.sourceSlot()).isEqualTo(2);
        assertThat(release.targetSlot()).isEqualTo(7);
    }

    @Test
    void disabledSourceAndInvalidReleaseNeverTriggerAnAction() {
        TableSlotDragGesture gesture = new TableSlotDragGesture();

        assertThat(gesture.begin(0, false, 100L)).isFalse();
        assertThat(gesture.release(0, 1_000L).action())
                .isEqualTo(TableSlotDragGesture.Action.NONE);

        gesture.begin(0, true, 1_000L);
        gesture.updateTarget(-1);
        assertThat(gesture.targetSlot()).isEqualTo(-1);
        assertThat(gesture.release(-1, 1_500L).action())
                .isEqualTo(TableSlotDragGesture.Action.NONE);
    }

    @Test
    void holdingWithoutMovingDoesNotOpenTheIngredientEditor() {
        TableSlotDragGesture gesture = new TableSlotDragGesture();
        gesture.begin(3, true, 2_000L);

        assertThat(gesture.release(3, 2_350L).action())
                .isEqualTo(TableSlotDragGesture.Action.NONE);
    }
}
