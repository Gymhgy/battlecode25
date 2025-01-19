package v5pathfind;

import battlecode.common.*;
import v5pathfind.fast.FastLocIntMap;
import v5pathfind.fast.FastLocSet;
import v5pathfind.fast.FastMath;

public class Splasher {
    private static FastLocSet allyPaintTowers = new FastLocSet();
    private static FastLocSet allyMoneyTowers = new FastLocSet();

    static MapLocation target = null;

    private static int range = UnitType.SPLASHER.actionRadiusSquared;

    static void init(RobotController rc) {
        Refill.init(80);
    }

    static void run(RobotController rc) throws GameActionException {
        Communicator.update(rc);
        Communicator.relayEnemyTower(rc);
        boolean refilling = Refill.refill(rc);
        if (refilling) {
            rc.setIndicatorDot(rc.getLocation(), 255, 0, 125);
            rc.setIndicatorString("refilling");
            return;
        }
        if (target != null && !Communicator.enemyTowers.contains(target)) target = null;
        if (target == null) target = Communicator.enemyTowers.closest(rc.getLocation());
        if (target!=null) {
            attackTower(rc);
            attackTower(rc);
            if(rc.isActionReady()) performAttack(rc);
        }
        else {
            if(rc.isActionReady()) performAttack(rc);
            Explorer.smartExplore(rc);
        }

        if (target != null)
            rc.setIndicatorLine(rc.getLocation(), target, 255, 255, 255);
    }

    private static void attackTower(RobotController rc) throws GameActionException {
        // System.out.println(target.toString());
        if (FastMath.manhattan(rc.getLocation(), target) <= 4 && rc.isActionReady()) {
            MapLocation bestAttackTile = null;
            int bestValue = Integer.MIN_VALUE;

            for (MapLocation loc : rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 4)) {
                if (FastMath.manhattan(loc, target) <= 2 && rc.canAttack(loc)) {
                    rc.attack(loc);
                    break;
                }
            }
        } else {
            if (FastMath.manhattan(rc.getLocation(), target) > 4) {
                Pathfinding.moveToward(rc, target);
            }
            else {
                // TODO: Splasher micro
                //kite(rc, target);
            }
        }
    }


    private static int getWorth(MapInfo mi) {
        PaintType pt = mi.getPaint();
        //TODO: tweak this...
        if (mi.hasRuin()) return 0;
        if (pt.isAlly()) return -1;
        if (pt.isEnemy()) return 4;
        return 1;
    }
    private static int worthThreshold = 12; // leaving this here: easier to see and tweak

    static int[][] attackTiles2 = {
            {-2, 0}, {2, 0}, {0, -2}, {0, 2}
    };
    static int[][] coordinates = {
            {0, 0}, {-1, 0}, {1, 0}, {0, -1}, {0, 1},
            {-1, -1}, {-1, 1}, {1, -1}, {1, 1}, {-2, 0},
            {2, 0}, {0, -2}, {0, 2}
    };
    static void performAttack(RobotController rc) throws GameActionException {
            /*  RobotInfo[] enemies = rc.senseNearbyRobots(range, rc.getTeam().opponent());
            RobotInfo[] allies = rc.senseNearbyRobots(range, rc.getTeam());
            I'm leaving enemies/allies here... even though it isn't used right now. */
        // TODO: Splashers deal 100 damage to towers (but not to robots)... so do we care about sensing nearby robots?
        //MapInfo[] attackTiles = rc.senseNearbyMapInfos(range);

        int bestWorth = 0;
        MapLocation best = null;
        FastLocIntMap cache = new FastLocIntMap();
        String ind = "";
        for (int[] square2 : attackTiles2) {
            MapLocation squareMapLoc = rc.getLocation().translate(square2[0], square2[1]);
            if (!rc.canSenseLocation(squareMapLoc)) continue;
            MapInfo square = rc.senseMapInfo(squareMapLoc);
            int worth = 0;
            for (int i = coordinates.length; i-->0;){
                MapLocation loc = square.getMapLocation().translate(coordinates[i][0], coordinates[i][1]);
                if (!rc.canSenseLocation(loc)) continue;
                MapInfo mi = rc.senseMapInfo(loc);
                if (cache.contains(loc)) {
                    worth += cache.getVal(loc);
                }
                else {
                    int val = getWorth(mi);
                    cache.add(loc, val);
                    worth += val;
                }
            }
            if (worth > worthThreshold) {
                best = square.getMapLocation();
                bestWorth = worth;
                break;
            }
            ind+=Clock.getBytecodeNum()+" | ";
        }
        rc.setIndicatorString(ind);
        if (best != null && canPaintReal(rc, best) && bestWorth > worthThreshold) {
            if (rc.canAttack(best)) {
                rc.attack(best, false);
            }
        }
    }
    static boolean canPaintReal(RobotController rc, MapLocation loc) throws GameActionException { // canPaint that checks for cost
        int paintCap = rc.getPaint();
        return rc.isActionReady() && paintCap > rc.getType().attackCost && rc.canPaint(loc);
    }

}
