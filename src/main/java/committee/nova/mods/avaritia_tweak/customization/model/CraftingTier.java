package committee.nova.mods.avaritia_tweak.customization.model;

import java.util.Arrays;
import java.util.Optional;

public enum CraftingTier {
    SCULK(1, 3),
    NETHER(2, 5),
    END(3, 7),
    EXTREME(4, 9);

    private final int value;
    private final int gridSize;

    CraftingTier(int value, int gridSize) {
        this.value = value;
        this.gridSize = gridSize;
    }

    public int value() {
        return this.value;
    }

    public int gridSize() {
        return this.gridSize;
    }

    public int capacity() {
        return this.gridSize * this.gridSize;
    }

    public static Optional<CraftingTier> fromValue(int value) {
        return Arrays.stream(values()).filter(tier -> tier.value == value).findFirst();
    }
}
