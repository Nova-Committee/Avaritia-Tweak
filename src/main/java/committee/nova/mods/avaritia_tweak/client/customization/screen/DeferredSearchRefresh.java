package committee.nova.mods.avaritia_tweak.client.customization.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/** Defers result replacement until after the active text-input callback has completed. */
final class DeferredSearchRefresh {
    private boolean queued;

    void request(Screen owner, Runnable refresh) {
        if (this.queued) {
            return;
        }
        this.queued = true;
        Minecraft.getInstance().tell(() -> {
            this.queued = false;
            if (Minecraft.getInstance().screen == owner) {
                refresh.run();
            }
        });
    }
}
