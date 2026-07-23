package committee.nova.mods.avaritia_tweak.client.customization.screen;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;

/** Keeps the inventory key inside a focused text field before a container screen can close. */
final class FocusedEditBoxKeyGuard {
    private FocusedEditBoxKeyGuard() {
    }

    static boolean consume(GuiEventListener focused, KeyMapping inventoryKey,
                           int keyCode, int scanCode, int modifiers) {
        if (!(focused instanceof EditBox editBox)
                || !inventoryKey.matches(keyCode, scanCode)) {
            return false;
        }
        editBox.keyPressed(keyCode, scanCode, modifiers);
        return true;
    }
}
