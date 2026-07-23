package committee.nova.mods.avaritia_tweak.client.customization.screen;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

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
        EditorUiScale.ShapedGrid seven = EditorUiScale.shapedGrid(18, 96, 284, 96, 7);
        EditorUiScale.ShapedGrid nine = EditorUiScale.shapedGrid(18, 96, 284, 96, 9);

        assertThat(seven.gridPixels()).isLessThanOrEqualTo(96);
        assertThat(nine.gridPixels()).isLessThanOrEqualTo(96);
        assertThat(seven.resultX()).isEqualTo(nine.resultX());
        assertThat(seven.outputOverlapsGrid()).isFalse();
        assertThat(nine.outputOverlapsGrid()).isFalse();
        assertThat(nine.cellSize()).isPositive();
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
}
