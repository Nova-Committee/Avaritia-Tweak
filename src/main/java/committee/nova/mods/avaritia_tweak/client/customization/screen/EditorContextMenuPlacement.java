package committee.nova.mods.avaritia_tweak.client.customization.screen;

/** Pure placement and hit-testing rules for an editor context menu. */
record EditorContextMenuPlacement(int x, int y, int width, int height, int visibleRows) {
    static EditorContextMenuPlacement calculate(int screenWidth, int screenHeight,
                                                int anchorX, int anchorY, int desiredWidth,
                                                int itemCount, int rowHeight, int padding,
                                                int margin, int offset) {
        if (screenWidth <= 0 || screenHeight <= 0 || desiredWidth <= 0
                || itemCount <= 0 || rowHeight <= 0 || padding < 0
                || margin < 0 || offset < 0) {
            throw new IllegalArgumentException("Context menu dimensions must be positive");
        }

        int usableWidth = Math.max(1, screenWidth - margin * 2);
        int usableHeight = Math.max(1, screenHeight - margin * 2);
        int width = Math.min(desiredWidth, usableWidth);
        int availableRows = Math.max(1, (usableHeight - padding * 2) / rowHeight);
        int visibleRows = Math.min(itemCount, availableRows);
        int height = Math.min(usableHeight, padding * 2 + visibleRows * rowHeight);

        int x = anchorX + offset;
        if (x + width + margin > screenWidth) {
            x = anchorX - width - offset;
        }
        int y = anchorY + offset;
        if (y + height + margin > screenHeight) {
            y = anchorY - height - offset;
        }
        x = clamp(x, margin, Math.max(margin, screenWidth - width - margin));
        y = clamp(y, margin, Math.max(margin, screenHeight - height - margin));
        return new EditorContextMenuPlacement(x, y, width, height, visibleRows);
    }

    boolean contains(double mouseX, double mouseY) {
        return mouseX >= this.x && mouseX < this.x + this.width
                && mouseY >= this.y && mouseY < this.y + this.height;
    }

    int itemAt(double mouseX, double mouseY, int rowHeight, int padding,
               int scrollOffset, int itemCount) {
        if (!contains(mouseX, mouseY)) {
            return -1;
        }
        int relativeY = (int) mouseY - this.y - padding;
        if (relativeY < 0 || relativeY >= this.visibleRows * rowHeight) {
            return -1;
        }
        int index = scrollOffset + relativeY / rowHeight;
        return index < itemCount ? index : -1;
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(value, maximum));
    }
}
