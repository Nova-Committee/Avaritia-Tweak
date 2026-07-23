package committee.nova.mods.avaritia_tweak.client.customization.screen;

/**
 * Logical-pixel layout helpers for Minecraft GUI scaling.
 *
 * <p>Minecraft applies the user's GUI scale before screens receive their width and height. Keeping
 * all editor layouts in this class makes the same screens reflow safely when that logical viewport
 * becomes smaller at a larger GUI scale.</p>
 */
final class EditorUiScale {
    private static final int EDGE_MARGIN = 6;
    private static final int GHOST_SLOT_SIZE = 18;
    private static final int RESULT_GAP = 10;
    private static final int EDITOR_HEADER_INSET = 8;

    private EditorUiScale() {
    }

    static Frame fit(int screenWidth, int screenHeight, int preferredWidth, int preferredHeight) {
        int availableWidth = Math.max(1, screenWidth - EDGE_MARGIN * 2);
        int availableHeight = Math.max(1, screenHeight - EDGE_MARGIN * 2);
        int width = Math.min(preferredWidth, availableWidth);
        int height = Math.min(preferredHeight, availableHeight);
        return new Frame((screenWidth - width) / 2, (screenHeight - height) / 2, width, height);
    }

    static boolean isWide(int width, int height, int minimumWidth, int minimumHeight) {
        return width >= minimumWidth && height >= minimumHeight;
    }

    static EditorHeader editorHeader(int screenWidth) {
        int innerWidth = Math.max(2, screenWidth - EDITOR_HEADER_INSET * 2);
        int preferredThemeWidth = Math.min(124, Math.max(84, screenWidth / 4));
        int themeWidth = Math.min(preferredThemeWidth, Math.max(1, innerWidth / 2));
        int themeX = Math.max(EDITOR_HEADER_INSET, screenWidth - themeWidth - EDITOR_HEADER_INSET);
        int tabWidth = Math.max(1, Math.max(3, themeX - 12) / 3);
        return new EditorHeader(themeX, themeWidth, tabWidth);
    }

    static ShapedGrid shapedGrid(int x, int y, int width, int availableHeight, int gridSize) {
        int resultX = x + Math.max(0, width - GHOST_SLOT_SIZE);
        int availableGridWidth = Math.max(1, resultX - RESULT_GAP - x);
        int cellByWidth = Math.max(1, availableGridWidth / gridSize);
        int cellByHeight = Math.max(1, Math.max(1, availableHeight) / gridSize);
        int cellSize = Math.min(GHOST_SLOT_SIZE, Math.min(cellByWidth, cellByHeight));
        int gridPixels = cellSize * gridSize;
        int resultY = y + Math.max(0, (gridPixels - GHOST_SLOT_SIZE) / 2);
        return new ShapedGrid(x, y, cellSize, gridPixels, resultX, resultY);
    }

    static SplitPane recipeSelectorSplit(int panelWidth) {
        int availableWidth = Math.max(2, panelWidth - 18);
        int minimumDetailWidth = Math.min(118, Math.max(1, availableWidth / 3));
        int maximumListWidth = Math.max(1, availableWidth - minimumDetailWidth);
        int preferredListWidth = panelWidth >= 560
                ? Math.min(410, panelWidth * 3 / 5)
                : Math.max(148, panelWidth * 55 / 100);
        int listWidth = Math.max(1, Math.min(preferredListWidth, maximumListWidth));
        return new SplitPane(listWidth, Math.max(1, availableWidth - listWidth));
    }

    static int listPageSize(int listTop, int listBottom, int rowHeight, int footerHeight) {
        if (rowHeight <= 0) {
            throw new IllegalArgumentException("List row height must be positive");
        }
        int availableHeight = Math.max(0, listBottom - listTop - Math.max(0, footerHeight));
        return Math.max(1, availableHeight / rowHeight);
    }

    record Frame(int left, int top, int width, int height) {
    }

    record EditorHeader(int themeX, int themeWidth, int tabWidth) {
        boolean tabsOverlapTheme() {
            int finalTabRight = EDITOR_HEADER_INSET + this.tabWidth * 2
                    + Math.max(1, this.tabWidth - 2);
            return finalTabRight > this.themeX;
        }
    }

    record ShapedGrid(int gridX, int gridY, int cellSize, int gridPixels,
                      int resultX, int resultY) {
        boolean outputOverlapsGrid() {
            return this.resultX < this.gridX + this.gridPixels;
        }

        int slotAt(double mouseX, double mouseY, int gridSize) {
            double localX = mouseX - this.gridX;
            double localY = mouseY - this.gridY;
            if (localX < 0 || localY < 0
                    || localX >= this.gridPixels || localY >= this.gridPixels) {
                return -1;
            }
            int column = (int) (localX / this.cellSize);
            int row = (int) (localY / this.cellSize);
            return row < gridSize && column < gridSize ? row * gridSize + column : -1;
        }
    }

    record SplitPane(int listWidth, int detailWidth) {
    }
}
