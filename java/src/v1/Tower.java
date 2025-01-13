package v1;

import battlecode.common.*;
import v1.fast.FastMath;

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
    static boolean og = false;
    static void init(RobotController rc) throws GameActionException {
        if (rc.getRoundNum() == 1) {
            og = true;
        }
    }

    static int servicerId = 0;
    static int lastSeenServicer = 0;

    static void run(RobotController rc) throws GameActionException {
        RobotInfo[] allies = rc.senseNearbyRobots(-1, rc.getTeam());
        for (RobotInfo ally : allies) {
            // ...
            if (ally.getID() == servicerId) {
                lastSeenServicer = rc.getRoundNum();
            }
        }
        if (rc.getRoundNum() - lastSeenServicer > 50) {
            servicerId = 0;
        }

        if (rc.getChips() > 1500) {
            Direction dir = directions[FastMath.rand256() % 8];
            MapLocation nextLoc = rc.getLocation().add(dir);

            UnitType type = UnitType.SOLDIER;
            if (og && rc.getRoundNum() < 3) {
                type = UnitType.SOLDIER;
            }
            else if (rc.getRoundNum() < 100 && FastMath.rand256() % 4 < 1) {
                type = UnitType.MOPPER;
            }
            else if (FastMath.rand256() % 3 < 2) {
                type = UnitType.MOPPER;
            }

            if (rc.canBuildRobot(type, nextLoc)) {
                if (type == UnitType.MOPPER) {
                    if (servicerId == 0) {
                        buildServicer(rc, nextLoc);
                    }
                    else {
                        rc.buildRobot(type, nextLoc);
                    }
                }

                if (type == UnitType.SOLDIER) {
                    rc.buildRobot(type, nextLoc);
                }
            }
        }

        attackNearby(rc);
    }

    static void buildServicer(RobotController rc, MapLocation loc) throws GameActionException {
        rc.buildRobot(UnitType.MOPPER, loc);
        RobotInfo ri = rc.senseRobotAtLocation(loc);
        servicerId = ri.getID();
        lastSeenServicer = rc.getRoundNum();
        rc.sendMessage(loc, 1 << 31);
    }

    static void attackNearby(RobotController rc) throws GameActionException {
        RobotInfo[] enemies = rc.senseNearbyRobots(rc.getType().actionRadiusSquared, rc.getTeam().opponent());
        // TODO: attack tile that will do the most damage
        for (RobotInfo enemy : enemies) {
            if (rc.isActionReady() && rc.canAttack(enemy.getLocation())) {
                rc.attack(enemy.getLocation());
                return;
            }
        }
    }
}
