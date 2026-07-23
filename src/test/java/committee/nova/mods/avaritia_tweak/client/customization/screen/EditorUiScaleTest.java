package committee.nova.mods.avaritia_tweak.client.customization.screen;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EditorUiScaleTest {
    @Test
    void fitsEveryScreenInsideLargeGuiScaleViewport() {
        EditorUiScale.Frame frame = EditorUiScale.fit(320, 240, 720, 390);

        assertThat(frame.left()).isEqualTo(6);
        assertThat(frame.top()).isEqualTo(6);
        assertThat(frame.width()).isEqualTo(308);
        assertThat(frame.height()).isEqualTo(228);
        assertThat(frame.left() + frame.width()).isLessThanOrEqualTo(320);
        assertThat(frame.top() + frame.height()).isLessThanOrEqualTo(240);
    }

    @Test
    void sevenAndNineSquareGridsStayOnOnePageWithFixedOutputColumn() {
        EditorUiScale.ShapedGrid seven = EditorUiScale.shapedGrid(18, 120, 284, 72, 7);
        EditorUiScale.ShapedGrid nine = EditorUiScale.shapedGrid(18, 120, 284, 72, 9);

        assertThat(seven.gridPixels()).isLessThanOrEqualTo(72);
        assertThat(nine.gridPixels()).isLessThanOrEqualTo(72);
        assertThat(seven.resultX()).isEqualTo(nine.resultX());
        assertThat(seven.outputOverlapsGrid()).isFalse();
        assertThat(nine.outputOverlapsGrid()).isFalse();
        assertThat(nine.cellSize()).isPositive();
    }

    @Test
    void brushDragHitTestingCoversScaledGridWithoutReachingOutput() {
        EditorUiScale.ShapedGrid layout = EditorUiScale.shapedGrid(18, 120, 284, 72, 9);

        assertThat(layout.slotAt(18, 120, 9)).isZero();
        assertThat(layout.slotAt(18 + layout.gridPixels() - 0.1,
                120 + layout.gridPixels() - 0.1, 9)).isEqualTo(80);
        assertThat(layout.slotAt(layout.resultX(), layout.resultY(), 9)).isEqualTo(-1);
        assertThat(layout.slotAt(17.9, 120, 9)).isEqualTo(-1);
    }

    @Test
    void regularViewportKeepsNativeGhostSlotSize() {
        EditorUiScale.ShapedGrid layout = EditorUiScale.shapedGrid(10, 120, 260, 180, 9);

        assertThat(layout.cellSize()).isEqualTo(18);
        assertThat(layout.gridPixels()).isEqualTo(162);
        assertThat(layout.outputOverlapsGrid()).isFalse();
    }

    @Test
    void compactHeaderKeepsThemeSwitcherSeparateFromPanelTabs() {
        EditorUiScale.EditorHeader standard = EditorUiScale.editorHeader(320);
        EditorUiScale.EditorHeader narrow = EditorUiScale.editorHeader(240);

        assertThat(standard.tabsOverlapTheme()).isFalse();
        assertThat(narrow.tabsOverlapTheme()).isFalse();
        assertThat(standard.themeX() + standard.themeWidth()).isEqualTo(312);
        assertThat(narrow.themeX() + narrow.themeWidth()).isEqualTo(232);
        assertThat(standard.tabWidth()).isPositive();
        assertThat(narrow.tabWidth()).isPositive();
    }

    @Test
    void recipeSelectorKeepsInspectorUsableAtEverySupportedScale() {
        EditorUiScale.SplitPane standard = EditorUiScale.recipeSelectorSplit(720);
        EditorUiScale.SplitPane scaled = EditorUiScale.recipeSelectorSplit(308);
        EditorUiScale.SplitPane narrow = EditorUiScale.recipeSelectorSplit(100);

        assertThat(standard).isEqualTo(new EditorUiScale.SplitPane(410, 292));
        assertThat(scaled).isEqualTo(new EditorUiScale.SplitPane(169, 121));
        assertThat(narrow.listWidth()).isPositive();
        assertThat(narrow.detailWidth()).isPositive();
        assertThat(narrow.listWidth() + narrow.detailWidth()).isEqualTo(82);
    }

    @Test
    void listPaginationUsesEveryRowThatFitsAboveItsFooter() {
        assertThat(EditorUiScale.listPageSize(134, 492, 20, 26)).isEqualTo(16);
        assertThat(EditorUiScale.listPageSize(134, 280, 20, 26)).isEqualTo(6);
        assertThat(EditorUiScale.listPageSize(134, 150, 20, 26)).isEqualTo(1);
        assertThatThrownBy(() -> EditorUiScale.listPageSize(0, 100, 0, 20))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
