package v2;

import battlecode.common.*;
import v2.fast.FastLocSet;

import java.util.HashMap;

public class Splasher {
    private static FastLocSet allyPaintTowers = new FastLocSet();
    private static FastLocSet allyMoneyTowers = new FastLocSet();
    private static MapLocation paintTower = null;
    private static MapLocation moneyTower = null;
    private static MapLocation target = null;
    private static MapInfo[] nearbyTiles;
    private static final HashMap<PaintType, Integer> paintPriorityValues = new HashMap<PaintType, Integer>(){{
        put(PaintType.ALLY_PRIMARY, 0);
        put(PaintType.ALLY_SECONDARY, 0);
        put(PaintType.ENEMY_PRIMARY, 2);
        put(PaintType.ENEMY_SECONDARY, 2);
        put(PaintType.EMPTY, 1);
    }};
    private static int worth = 5; // leaving this here: easier to see and tweak
    private static int range = UnitType.SPLASHER.actionRadiusSquared;
    private static boolean attacker;

    static void init(RobotController rc) {
        Message[] messages = rc.readMessages(-1);
        for (Message m: messages) {
            //System.out.println(Integer.toBinaryString(m.getBytes()));
            if ((m.getBytes() & 0b0001) == 1) {
                attacker = true;
                target = new MapLocation((m.getBytes() & 0x000CFF0) / 16, (m.getBytes() & 0xFFFC0000) / 262144);
            }
        }
    }

    static void run(RobotController rc) throws GameActionException {
        if (attacker) {
            //System.out.println("hello");
            attackTower(rc);
        } else {
            if (rc.isActionReady()) {
                performAttack(rc); // We want to move towards enemy towers: how?
            }
            v1.Pathfinding.navigateRandomly(rc);
        }
    }
    private static int worthQuota(RobotController rc, MapInfo[] sensed) throws GameActionException { // This Is this worth it ?
        int result = 0;
        for (MapInfo m : sensed)  {
            if (m != null && rc.onTheMap(m.getMapLocation())) {
                result += paintPriorityValues.get((m.getPaint()));
            }
        }
        return result;
    }
    private static void attackTower(RobotController rc) throws GameActionException {
        // System.out.println(target.toString());
        if (rc.canAttack(target) && rc.isActionReady()) {
            rc.attack(target);
        } else {
            Pathfinding.moveToward(rc, target);
        }

    }




    static void performAttack(RobotController rc) throws GameActionException {
            /*  RobotInfo[] enemies = rc.senseNearbyRobots(range, rc.getTeam().opponent());
            RobotInfo[] allies = rc.senseNearbyRobots(range, rc.getTeam());
            I'm leaving enemies/allies here... even though it isn't used right now. */
        // TODO: Splashers deal 100 damage to towers (but not to robots)... so do we care about sensing nearby robots?

        nearbyTiles = rc.senseNearbyMapInfos();
        MapInfo[] attackTiles = rc.senseNearbyMapInfos(range);

        int i = 0;
        MapInfo[] group = new MapInfo[13];
        MapLocation loc;
        MapLocation checker;
        // Attempt 1: Hash tabling to save bytecode LMAO
        // Still on 15000 Bytecode/turn...
        HashMap<String, MapInfo> tileMap = new HashMap<>();
        for (MapInfo tile : nearbyTiles) {
            String key = tile.getMapLocation().x + "," + tile.getMapLocation().y;
            tileMap.put(key, tile);
        }
        int[][] coordinates = {
                {0, 0}, {-1, 0}, {1, 0}, {0, -1}, {0, 1}, {-1, -1}, {-1, 1}, {1, -1}, {1, 1}, {-2, 0}, {2, 0}, {0, -2}, {0, 2}
        }; // Evem if you change the coordinates to JUST {0, 2}, it still takes forever. Why?
        String c = "";
        for (MapInfo square: attackTiles) {


            loc = square.getMapLocation();

            // Solution 1: Hash Table
            for (int[] a : coordinates) {
                c = (loc.x + a[0]) + "," + (loc.y + a[1]);
                group[i] = tileMap.get(c);
                i++;
            }
            i = 0;
            /*
            group = rc.senseNearbyMapInfos(loc, 2);
            Easiest solution: probably the worst.
             */
            /*  Brute Force:
            checker = tile.getMapLocation();
            for (MapInfo tile : nearbyTiles) {
                if (loc.isWithinDistanceSquared(checker, 2)) {
                group[i] = tile;
                i++;
            }
            }


            */


            if (canPaintReal(rc, loc) && worthQuota(rc, group) > worth) {
                if (rc.canAttack(loc)) {
                    int secondary = v2.fast.FastMath.rand256() % 2;
                    rc.attack(loc, secondary == 1);
                    return;
                }
            }



        }
        return;

        //Attempt 2: Brute force (same thing? Why is it so expensive?)
        /*
        for (MapInfo square : attackTiles) {
            loc = square.getMapLocation();
            group[0] = square;

            if (Util.canPaintReal(rc, loc) && worthQuota(rc, group) > worth) {
                if (rc.canAttack(loc)) {
                    int secondary = FastMath.rand256() % 2;
                    rc.attack(loc, secondary == 1);
                }
            }
            i = 1;
        }

        */
    }
    static boolean canPaintReal(RobotController rc, MapLocation loc) throws GameActionException { // canPaint that checks for cost
        int paintCap = rc.getPaint();
        return rc.isActionReady() && paintCap > rc.getType().attackCost && rc.canPaint(loc);
    }

}
