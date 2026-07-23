package committee.nova.mods.avaritia_tweak.client.customization.screen;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FocusedEditBoxKeyGuardTest {
    @Test
    void consumesInventoryKeyAndForwardsItToFocusedEditBox() {
        EditBox editBox = mock(EditBox.class);
        KeyMapping inventoryKey = mock(KeyMapping.class);
        when(inventoryKey.matches(69, 18)).thenReturn(true);

        assertThat(FocusedEditBoxKeyGuard.consume(editBox, inventoryKey, 69, 18, 0)).isTrue();
        verify(editBox).keyPressed(69, 18, 0);
    }

    @Test
    void letsOtherKeysAndOtherFocusedWidgetsFollowNormalScreenRouting() {
        EditBox editBox = mock(EditBox.class);
        GuiEventListener otherWidget = mock(GuiEventListener.class);
        KeyMapping inventoryKey = mock(KeyMapping.class);

        assertThat(FocusedEditBoxKeyGuard.consume(editBox, inventoryKey, 65, 30, 0)).isFalse();
        assertThat(FocusedEditBoxKeyGuard.consume(otherWidget, inventoryKey, 69, 18, 0)).isFalse();
        verify(editBox, never()).keyPressed(65, 30, 0);
    }
}
