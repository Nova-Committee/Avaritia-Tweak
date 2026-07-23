package committee.nova.mods.avaritia_tweak.client.customization.screen;

/** Tracks a press-and-hold gesture without owning recipe or screen state. */
final class TableSlotDragGesture {
    static final long HOLD_MILLIS = 350L;

    private int sourceSlot = -1;
    private int targetSlot = -1;
    private long pressedAtMillis;

    boolean begin(int slot, boolean enabled, long nowMillis) {
        cancel();
        if (!enabled || slot < 0) {
            return false;
        }
        this.sourceSlot = slot;
        this.targetSlot = slot;
        this.pressedAtMillis = nowMillis;
        return true;
    }

    boolean armed() {
        return this.sourceSlot >= 0;
    }

    boolean active(long nowMillis) {
        return armed() && Math.max(0L, nowMillis - this.pressedAtMillis) >= HOLD_MILLIS;
    }

    int sourceSlot() {
        return this.sourceSlot;
    }

    int targetSlot() {
        return this.targetSlot;
    }

    void updateTarget(int slot) {
        if (armed()) {
            this.targetSlot = slot;
        }
    }

    Release release(int slot, long nowMillis) {
        if (!armed()) {
            return Release.none();
        }
        int source = this.sourceSlot;
        boolean longPress = active(nowMillis);
        cancel();
        if (longPress && slot >= 0 && slot != source) {
            return new Release(Action.SWAP, source, slot);
        }
        if (!longPress && slot == source) {
            return new Release(Action.CLICK, source, source);
        }
        return Release.none();
    }

    void cancel() {
        this.sourceSlot = -1;
        this.targetSlot = -1;
        this.pressedAtMillis = 0L;
    }

    enum Action {
        NONE,
        CLICK,
        SWAP
    }

    record Release(Action action, int sourceSlot, int targetSlot) {
        private static Release none() {
            return new Release(Action.NONE, -1, -1);
        }
    }
}
