package v3;

import battlecode.common.*;
import v3.fast.FastLocIntMap;
import v3.fast.FastLocSet;

public class Splasher {
    private static FastLocSet allyPaintTowers = new FastLocSet();
    private static FastLocSet allyMoneyTowers = new FastLocSet();

    static MapLocation target = null;
    private static MapInfo[] nearbyTiles;

    private static int worthThreshold = 6; // leaving this here: easier to see and tweak
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
        if (rc.canAttack(target) && rc.isActionReady()) {
            rc.attack(target);
        } else {
            if (!target.isWithinDistanceSquared(rc.getLocation(), 4)) {
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
        if (pt.isEnemy()) return 2;
        return 1;
    }

    static void performAttack(RobotController rc) throws GameActionException {
            /*  RobotInfo[] enemies = rc.senseNearbyRobots(range, rc.getTeam().opponent());
            RobotInfo[] allies = rc.senseNearbyRobots(range, rc.getTeam());
            I'm leaving enemies/allies here... even though it isn't used right now. */
        // TODO: Splashers deal 100 damage to towers (but not to robots)... so do we care about sensing nearby robots?

        nearbyTiles = rc.senseNearbyMapInfos();
        MapInfo[] attackTiles = rc.senseNearbyMapInfos(range);

        int[][] coordinates = {
                {0, 0}, {-1, 0}, {1, 0}, {0, -1}, {0, 1},
                {-1, -1}, {-1, 1}, {1, -1}, {1, 1}, {-2, 0},
                {2, 0}, {0, -2}, {0, 2}
        };
        int bestWorth = 0;
        MapLocation best = null;
        FastLocIntMap cache = new FastLocIntMap();
        String ind = "";
        for (MapInfo square: attackTiles) {
            ind+=Clock.getBytecodeNum()+",";

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
