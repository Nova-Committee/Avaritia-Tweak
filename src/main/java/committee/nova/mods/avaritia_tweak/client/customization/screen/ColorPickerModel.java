package committee.nova.mods.avaritia_tweak.client.customization.screen;

final class ColorPickerModel {
    private ColorPickerModel() {
    }

    static Hsv fromRgb(int rgb) {
        float red = ((rgb >> 16) & 0xff) / 255.0f;
        float green = ((rgb >> 8) & 0xff) / 255.0f;
        float blue = (rgb & 0xff) / 255.0f;
        float maximum = Math.max(red, Math.max(green, blue));
        float minimum = Math.min(red, Math.min(green, blue));
        float delta = maximum - minimum;
        float hue;
        if (delta == 0.0f) {
            hue = 0.0f;
        } else if (maximum == red) {
            hue = ((green - blue) / delta) % 6.0f;
        } else if (maximum == green) {
            hue = (blue - red) / delta + 2.0f;
        } else {
            hue = (red - green) / delta + 4.0f;
        }
        hue /= 6.0f;
        if (hue < 0.0f) {
            hue += 1.0f;
        }
        float saturation = maximum == 0.0f ? 0.0f : delta / maximum;
        return new Hsv(hue, saturation, maximum);
    }

    static int toRgb(float hue, float saturation, float value) {
        float normalizedHue = normalizeHue(hue);
        float normalizedSaturation = clamp(saturation);
        float normalizedValue = clamp(value);
        float chroma = normalizedValue * normalizedSaturation;
        float section = normalizedHue * 6.0f;
        float intermediate = chroma * (1.0f - Math.abs(section % 2.0f - 1.0f));
        float red;
        float green;
        float blue;
        if (section < 1.0f) {
            red = chroma;
            green = intermediate;
            blue = 0.0f;
        } else if (section < 2.0f) {
            red = intermediate;
            green = chroma;
            blue = 0.0f;
        } else if (section < 3.0f) {
            red = 0.0f;
            green = chroma;
            blue = intermediate;
        } else if (section < 4.0f) {
            red = 0.0f;
            green = intermediate;
            blue = chroma;
        } else if (section < 5.0f) {
            red = intermediate;
            green = 0.0f;
            blue = chroma;
        } else {
            red = chroma;
            green = 0.0f;
            blue = intermediate;
        }
        float match = normalizedValue - chroma;
        return (channel(red + match) << 16)
                | (channel(green + match) << 8)
                | channel(blue + match);
    }

    static float clamp(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }

    private static float normalizeHue(float hue) {
        float normalized = hue % 1.0f;
        return normalized < 0.0f ? normalized + 1.0f : normalized;
    }

    private static int channel(float value) {
        return Math.round(clamp(value) * 255.0f);
    }

    record Hsv(float hue, float saturation, float value) {
    }
}
