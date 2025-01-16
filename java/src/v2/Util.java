package v2;

import battlecode.common.*;
import battlecode.common.UnitType;
import v2.Pathfinding;

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
    static boolean refillPaint(RobotController rc, MapLocation loc, int amount, int threshold) throws GameActionException {
        if (rc.canSenseRobotAtLocation(loc)) { // So many checks:
            UnitType a = rc.senseRobotAtLocation(loc).getType();
            if (isPaintTower(a)) {
                if (rc.canTransferPaint(loc, amount)) {
                    rc.transferPaint(loc, amount);
                    return false;
                } else {
                    // wait?
                }

            }
            else if (isMoneyTower(a)){
                if (rc.canTransferPaint(loc, amount)) {
                    rc.transferPaint(loc, amount);
                    return false;
                } else {
                    v2.Pathfinding.moveToward(rc, loc); // Have to wait for mopper refill.
                }
            }
        }
        if (rc.getPaint() > threshold) {
            return false;
        }
        else {
            v2.Pathfinding.moveToward(rc, loc);
        }
        return true;
    }
}
