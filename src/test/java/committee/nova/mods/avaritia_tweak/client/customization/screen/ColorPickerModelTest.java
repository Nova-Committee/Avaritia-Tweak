package committee.nova.mods.avaritia_tweak.client.customization.screen;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ColorPickerModelTest {
    @Test
    void primaryColorsRoundTripThroughHsv() {
        for (int color : new int[]{0xff0000, 0x00ff00, 0x0000ff, 0xffffff, 0x000000, 0x3b2754}) {
            ColorPickerModel.Hsv hsv = ColorPickerModel.fromRgb(color);
            assertEquals(color, ColorPickerModel.toRgb(hsv.hue(), hsv.saturation(), hsv.value()));
        }
    }

    @Test
    void clampsOutOfRangeSaturationAndValue() {
        assertEquals(0xff0000, ColorPickerModel.toRgb(0.0f, 2.0f, 2.0f));
        assertEquals(0x000000, ColorPickerModel.toRgb(0.5f, 1.0f, -1.0f));
    }
}
