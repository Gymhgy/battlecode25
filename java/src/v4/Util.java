package v4;

import battlecode.common.RobotController;
import battlecode.common.UnitType;

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

    static final int SRP_BUILDER = 4;
    public static boolean isSrpBuilder(RobotController rc, int id) {
        if (rc.getNumberTowers() < 3) return false;
        double r = (double)(rc.getMapWidth() * rc.getMapHeight() - 400) / 3600;
        return rc.getRoundNum() > 30 + (1-r)*50 && id % SRP_BUILDER == 0;
    }

}
