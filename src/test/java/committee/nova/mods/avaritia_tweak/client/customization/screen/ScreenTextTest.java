package committee.nova.mods.avaritia_tweak.client.customization.screen;

import net.minecraft.client.gui.Font;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ScreenTextTest {
    @Test
    void fitKeepsTextThatAlreadyFits() {
        Font font = mock(Font.class);
        when(font.width("artifact.json")).thenReturn(48);

        assertThat(ScreenText.fit(font, "artifact.json", 64)).isEqualTo("artifact.json");
    }

    @Test
    void fitUsesPixelWidthAndAddsEllipsis() {
        Font font = mock(Font.class);
        when(font.width("managed-artifact.json")).thenReturn(120);
        when(font.width("...")).thenReturn(12);
        when(font.plainSubstrByWidth("managed-artifact.json", 52)).thenReturn("managed-");

        assertThat(ScreenText.fit(font, "managed-artifact.json", 64)).isEqualTo("managed-...");
    }

    @Test
    void fitHandlesEmptyViewport() {
        assertThat(ScreenText.fit(mock(Font.class), "artifact.json", 0)).isEmpty();
    }
}
