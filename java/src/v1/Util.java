package v1;

import battlecode.common.*;

import static battlecode.common.UnitType.LEVEL_ONE_DEFENSE_TOWER;
import static battlecode.common.UnitType.LEVEL_ONE_PAINT_TOWER;

public class Util {
    
    public static boolean isMoneyTower(UnitType t) {
        return  t == UnitType.LEVEL_ONE_MONEY_TOWER ||
                t == UnitType.LEVEL_TWO_MONEY_TOWER ||
                t == UnitType.LEVEL_THREE_MONEY_TOWER;
    }

    public static boolean isPaintTower(UnitType t) {
        return  t == UnitType.LEVEL_ONE_PAINT_TOWER ||
                t == UnitType.LEVEL_THREE_PAINT_TOWER ||
                t == UnitType.LEVEL_TWO_PAINT_TOWER;
    }
}
