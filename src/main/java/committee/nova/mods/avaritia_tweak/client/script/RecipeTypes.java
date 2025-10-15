package committee.nova.mods.avaritia_tweak.client.script;

import lombok.Getter;

import java.util.Arrays;

/**
 * @author: cnlimiter
 */
@Getter
public enum RecipeTypes {


    AVARITIA_SCULK_CRAFTING(11, "sculk_crafting_table", -19, 1, 235, 0),    // 无尽工作台 1
    AVARITIA_NETHER_CRAFTING(12, "nether_crafting_table", -19, 24, 235, 23),    // 无尽工作台 2
    AVARITIA_END_CRAFTING(13, "end_crafting_table", -19, 47, 235, 46),    // 无尽工作台 3
    AVARITIA_EXTREME_CRAFTING(14, "extreme_crafting_table", -19, 70, 235, 69),    // 无尽工作台 4
    AVARITIA_EXTREME_SMITHING(15, "extreme_smithing_table", -19, 93, 235, 92),    // 无尽锻造台
    AVARITIA_COMPRESSOR(16, "compressor", -19, 116, 235, 115),    // 无尽压缩机
    AVARITIA_SINGULARITY(16, "compressor", -19, 139, 235, 138),    // 无尽压缩机

    VANILLA_CRAFTING(1, "", 231, 1, 235, 161),      // 原版工作台
    VANILLA_SMITHING(2, "", 231, 24, 235, 184),    // 原版锻造台
    VANILLA_FURNACE(3, "", 231, 47, 235, 207),    // 原版熔炉
    VANILLA_STONECUTTING(4, "", 231, 70, 235, 230);    // 原版切石机

    final int code;
    final String name;
    final int x;
    final int y;
    final int xTex;
    final int yTex;

    RecipeTypes(int code, String name, int x, int y, int xTex, int yTex) {
        this.code = code;
        this.name = name;
        this.x = x;
        this.y = y;
        this.xTex = xTex;
        this.yTex = yTex;
    }

    static RecipeTypes valueOf(int code) {
        return Arrays.stream(values()).filter(v -> v.getCode() == code).findFirst().orElse(null);
    }
}
