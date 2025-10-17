package committee.nova.mods.avaritia_tweak.client.script;

import committee.nova.mods.avaritia.api.client.screen.coordinate.TextureCoordinate;
import lombok.Getter;
import net.minecraft.network.chat.Component;

/**
 * @author cnlimiter
 */
@Getter
public enum OutType {
    JS(0, new TextureCoordinate(0, 404), new TextureCoordinate(0, 429)),
    ZS(1, new TextureCoordinate(23, 404), new TextureCoordinate(23, 429));
    private final int index;
    private final TextureCoordinate common;
    private final TextureCoordinate hover;
    private final Component name;

    private OutType(int index, TextureCoordinate common, TextureCoordinate hover) {
        this.index = index;
        this.common = common;
        this.hover = hover;
        this.name = Component.translatable(this.name() + "_out");
    }
}
