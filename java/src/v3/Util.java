package v3;

import battlecode.common.*;
import battlecode.common.UnitType;
import v3.fast.*;

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

    static final int SRP_BUILDER = 5;
    public static boolean isSrpBuilder(RobotController rc, int id) {
        return rc.getRoundNum() > 50 && id % SRP_BUILDER == 0;
    }

}
