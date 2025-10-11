package committee.nova.mods.avaritia_tweak.client.script;

import committee.nova.mods.avaritia.api.client.screen.coordinate.Coordinate;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author cnlimiter
 */
@Getter
public enum OtherActionButtons {
    brush(1
            , List.of(new Coordinate().setU0(235).setV0(140).setUWidth(22).setVHeight(24))
            , null
            , List.of(new Coordinate().setU0(258).setV0(140).setUWidth(22).setVHeight(24))
    ),
    select(2
            , List.of(new Coordinate().setU0(235).setV0(115).setUWidth(22).setVHeight(24),
            new Coordinate().setU0(258).setV0(115).setUWidth(22).setVHeight(24))
            , null
            , null
    ),
    eraser(3
            , List.of(new Coordinate().setU0(235).setV0(165).setUWidth(22).setVHeight(24))
            , null
            , List.of(new Coordinate().setU0(258).setV0(165).setUWidth(22).setVHeight(24))
    ),
    out(4
            , List.of(new Coordinate().setU0(235).setV0(0).setUWidth(22).setVHeight(24)
            , new Coordinate().setU0(235).setV0(25).setUWidth(22).setVHeight(24)
            , new Coordinate().setU0(235).setV0(50).setUWidth(22).setVHeight(24))
            , null
            , null
    ),
    gen(5
            , List.of(new Coordinate().setU0(235).setV0(190).setUWidth(22).setVHeight(24))
            , null
            , List.of(new Coordinate().setU0(258).setV0(190).setUWidth(22).setVHeight(24))
            ),
//    back(5
//            , List.of(new Coordinate().setU0(235).setV0(215).setUWidth(22).setVHeight(24))
//            , null
//            , List.of(new Coordinate().setU0(258).setV0(215).setUWidth(22).setVHeight(24))
//            ),
    ;

    final int code;
    final List<Coordinate> areas;
    final List<Coordinate> hovers;
    final List<Coordinate> taps;

    OtherActionButtons(int code, List<Coordinate> areas, List<Coordinate> hovers, List<Coordinate> taps) {
        this.code = code;
        this.areas = areas;
        this.hovers = hovers;
        this.taps = taps;
    }

    static OtherActionButtons valueOf(int code) {
        return Arrays.stream(values()).filter(v -> v.getCode() == code).findFirst().orElse(null);
    }
}
