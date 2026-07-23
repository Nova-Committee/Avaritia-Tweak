package committee.nova.mods.avaritia_tweak.client.customization.screen;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EditorContextMenuPlacementTest {
    private static final int ROW_HEIGHT = 18;
    private static final int PADDING = 3;
    private static final int MARGIN = 4;
    private static final int OFFSET = 2;

    @Test
    void opensBelowAndToTheRightWhenSpaceIsAvailable() {
        EditorContextMenuPlacement placement = placement(320, 240, 40, 50, 112, 3);

        assertThat(placement).isEqualTo(new EditorContextMenuPlacement(42, 52, 112, 60, 3));
    }

    @Test
    void flipsAboveAndToTheLeftNearBottomRightCorner() {
        EditorContextMenuPlacement placement = placement(320, 240, 310, 230, 112, 3);

        assertThat(placement.x()).isEqualTo(196);
        assertThat(placement.y()).isEqualTo(168);
        assertThat(placement.x() + placement.width()).isLessThanOrEqualTo(316);
        assertThat(placement.y() + placement.height()).isLessThanOrEqualTo(236);
    }

    @Test
    void clampsOversizedMenusAndExposesOnlyRowsThatFit() {
        EditorContextMenuPlacement placement = placement(100, 70, 50, 35, 180, 8);

        assertThat(placement.x()).isEqualTo(MARGIN);
        assertThat(placement.y()).isEqualTo(MARGIN);
        assertThat(placement.width()).isEqualTo(92);
        assertThat(placement.height()).isEqualTo(60);
        assertThat(placement.visibleRows()).isEqualTo(3);
    }

    @Test
    void hitTestingAccountsForPaddingAndScrollOffset() {
        EditorContextMenuPlacement placement = placement(320, 240, 40, 50, 112, 3);

        assertThat(placement.itemAt(50, 55, ROW_HEIGHT, PADDING, 2, 8)).isEqualTo(2);
        assertThat(placement.itemAt(50, 73, ROW_HEIGHT, PADDING, 2, 8)).isEqualTo(3);
        assertThat(placement.itemAt(41, 55, ROW_HEIGHT, PADDING, 2, 8)).isEqualTo(-1);
    }

    @Test
    void rejectsInvalidGeometry() {
        assertThatThrownBy(() -> placement(0, 240, 40, 50, 112, 3))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static EditorContextMenuPlacement placement(int screenWidth, int screenHeight,
                                                        int anchorX, int anchorY,
                                                        int desiredWidth, int itemCount) {
        return EditorContextMenuPlacement.calculate(screenWidth, screenHeight,
                anchorX, anchorY, desiredWidth, itemCount,
                ROW_HEIGHT, PADDING, MARGIN, OFFSET);
    }
}
