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

    record Frame(int left, int top, int width, int height) {
    }

    record ShapedGrid(int gridX, int gridY, int cellSize, int gridPixels,
                      int resultX, int resultY) {
        boolean outputOverlapsGrid() {
            return this.resultX < this.gridX + this.gridPixels;
        }
    }
}
