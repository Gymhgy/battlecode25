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
    static boolean canPaintReal(RobotController rc, MapLocation loc) throws GameActionException { // canPaint that checks for cost
        int paintCap = rc.getPaint();
        return paintCap > rc.getType().attackCost && rc.canPaint(loc);
    } // We're gonna end up using this function twice: Moving this to utils

}
