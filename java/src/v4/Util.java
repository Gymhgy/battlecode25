package v4;

import battlecode.common.*;

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

    static final Direction[] directions = {
            Direction.NORTH,
            Direction.NORTHEAST,
            Direction.EAST,
            Direction.SOUTHEAST,
            Direction.SOUTH,
            Direction.SOUTHWEST,
            Direction.WEST,
            Direction.NORTHWEST,
    };
    static final int SRP_BUILDER = 4;
    public static boolean isSrpBuilder(RobotController rc, int id) {
        if (rc.getNumberTowers() < 3) return false;
        double r = (double)(rc.getMapWidth() * rc.getMapHeight() - 400) / 3600;
        return rc.getRoundNum() > 30 + (1-r)*50 && id % SRP_BUILDER == 0;
    }

    public static boolean shouldKMS(RobotController rc) throws GameActionException {
        /*int crowded = 0;
        if (rc.getPaint() < rc.getType().paintCapacity * 0.3) {
            for (Direction d : directions) {
                MapLocation loc = rc.getLocation().add(d);
                if (rc.canSenseRobotAtLocation(loc)) {
                    RobotInfo r = rc.senseRobotAtLocation(loc);
                    if (r.getTeam() == rc.getTeam() && !r.getType().isTowerType()) crowded++;
                }
            }
        }
        return crowded >= 7;*/
        return false;
    }

}
