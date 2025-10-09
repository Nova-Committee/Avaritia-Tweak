package committee.nova.mods.avaritia_tweak.client.script;

import committee.nova.mods.avaritia.api.client.screen.coordinate.Coordinate;
import lombok.Getter;

import java.util.Arrays;

/**
 * @author: cnlimiter
 */
@Getter
public enum RecipeType {
    VANILLA_CRAFTING(1
            , ""
            , new Coordinate().setX(0).setY(0).setWidth(234).setHeight(278)
            , new Coordinate().setU0(348).setV0(0).setUWidth(22).setVHeight(22)
            , new Coordinate().setU0(326).setV0(0).setUWidth(22).setVHeight(22)),      // 原版工作台
    VANILLA_SMITHING(2
            , ""
            , new Coordinate().setX(0).setY(0).setWidth(234).setHeight(278)
            , new Coordinate().setU0(348).setV0(23).setUWidth(22).setVHeight(22)
            , new Coordinate().setU0(326).setV0(23).setUWidth(22).setVHeight(22)),    // 原版锻造台
    VANILLA_FURNACE(3
            , ""
            , new Coordinate().setX(0).setY(0).setWidth(234).setHeight(278)
            , new Coordinate().setU0(348).setV0(23).setUWidth(22).setVHeight(22)
            , new Coordinate().setU0(326).setV0(23).setUWidth(22).setVHeight(22)),    // 原版熔炉
    VANILLA_STONECUTTING(4
            , ""
            , new Coordinate().setX(0).setY(0).setWidth(234).setHeight(278)
            , new Coordinate().setU0(348).setV0(46).setUWidth(22).setVHeight(22)
            , new Coordinate().setU0(326).setV0(46).setUWidth(22).setVHeight(22)),    // 原版切石机

    AVARITIA_SCULK_CRAFTING(11
            , "sculk_crafting_table"
            , new Coordinate().setX(0).setY(0).setWidth(234).setHeight(278)
            , new Coordinate().setU0(281).setV0(0).setUWidth(22).setVHeight(22)
            , new Coordinate().setU0(303).setV0(0).setUWidth(22).setVHeight(22)),    // 无尽工作台 1
    AVARITIA_NETHER_CRAFTING(12
            , "nether_crafting_table"
            , new Coordinate().setX(0).setY(0).setWidth(234).setHeight(278)
            , new Coordinate().setU0(281).setV0(23).setUWidth(22).setVHeight(22)
            , new Coordinate().setU0(303).setV0(23).setUWidth(22).setVHeight(22)),    // 无尽工作台 2
    AVARITIA_END_CRAFTING(13
            , "end_crafting_table"
            , new Coordinate().setX(0).setY(0).setWidth(234).setHeight(278)
            , new Coordinate().setU0(281).setV0(46).setUWidth(22).setVHeight(22)
            , new Coordinate().setU0(303).setV0(46).setUWidth(22).setVHeight(22)),    // 无尽工作台 3
    AVARITIA_EXTREME_CRAFTING(14
            , "extreme_crafting_table"
            , new Coordinate().setX(0).setY(0).setWidth(234).setHeight(278)
            , new Coordinate().setU0(281).setV0(69).setUWidth(22).setVHeight(22)
            , new Coordinate().setU0(303).setV0(69).setUWidth(22).setVHeight(22)),    // 无尽工作台 4
    AVARITIA_EXTREME_SMITHING(15
            , "extreme_smithing_table"
            , new Coordinate().setX(0).setY(0).setWidth(234).setHeight(278)
            , new Coordinate().setU0(281).setV0(92).setUWidth(22).setVHeight(22)
            , new Coordinate().setU0(303).setV0(92).setUWidth(22).setVHeight(22)),    // 无尽锻造台
    AVARITIA_COMPRESSOR(16
            , "compressor"
            , new Coordinate().setX(0).setY(0).setWidth(234).setHeight(278)
            , new Coordinate().setU0(281).setV0(115).setUWidth(22).setVHeight(22)
            , new Coordinate().setU0(303).setV0(115).setUWidth(22).setVHeight(22));    // 无尽压缩机

    final int code;
    final String name;
    final Coordinate area;
    final Coordinate normal;
    final Coordinate tap;

    RecipeType(int code, String name, Coordinate area, Coordinate normal, Coordinate tap) {
        this.code = code;
        this.name = name;
        this.area = area;
        this.normal = normal;
        this.tap = tap;
    }

    static RecipeType valueOf(int code) {
        return Arrays.stream(values()).filter(v -> v.getCode() == code).findFirst().orElse(null);
    }
}
