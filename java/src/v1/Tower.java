package v1;

import battlecode.common.*;

public class Tower {
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
    static void run(RobotController rc) throws GameActionException {
        if (rc.getChips() > 1500) {
            // build soldier
            Direction dir = directions[FastMath.rand256() % 8];
            MapLocation nextLoc = rc.getLocation().add(dir);
            if (rc.canBuildRobot(UnitType.SOLDIER, nextLoc)){
                rc.buildRobot(UnitType.SOLDIER, nextLoc);
            }
        }
    }

}
