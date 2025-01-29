package v9;

import battlecode.common.*;
import v9.fast.FastLocIntMap;

public class MopperMicro {

    final int INF = 1000000;
    static boolean shouldPlaySafe = false;
    static int myRange;
    static int myVisionRange;
    static final int RANGE_LAUNCHER = 4;

    //a) prioritize squares such that there are adjacent to enemy paint
    //b) prioritize minimizing paint penalty
    //c) prioritize staying on ally paint
    //d) prioritize stay on neutral paint
    //static double myDPS;
    //static double[] DPS = new double[]{0, 0, 0, 0, 0, 0, 0};
    //static int[] rangeExtended = new int[]{0, 0, 0, 0, 0, 0, 0};

    static final Direction[] dirs = {
            Direction.NORTH,
            Direction.NORTHEAST,
            Direction.EAST,
            Direction.SOUTHEAST,
            Direction.SOUTH,
            Direction.SOUTHWEST,
            Direction.WEST,
            Direction.NORTHWEST,
            Direction.CENTER
    };

    final static int MAX_MICRO_BYTECODE_REMAINING = 2000;

    static RobotController rc;
    static FastLocIntMap cache;
    static boolean canAttack;
    MopperMicro(RobotController rc){
        this.rc = rc;
    }

    public boolean doMicro(MapLocation target){
        try {
            cache = new FastLocIntMap();
            if (!rc.isMovementReady()) return false;
            shouldPlaySafe = false;
            RobotInfo[] units = rc.senseNearbyRobots(myVisionRange, rc.getTeam().opponent());

            canAttack = rc.isActionReady();

            MicroInfo[] microInfo = new MicroInfo[9];
            for (int i = 0; i < 9; ++i) {
                microInfo[i] = new MicroInfo(rc, dirs[i], target);
            }
            for (RobotInfo unit : units) {
                if (Clock.getBytecodesLeft() < MAX_MICRO_BYTECODE_REMAINING) break;
                microInfo[0].updateEnemy(unit);
                microInfo[1].updateEnemy(unit);
                microInfo[2].updateEnemy(unit);
                microInfo[3].updateEnemy(unit);
                microInfo[4].updateEnemy(unit);
                microInfo[5].updateEnemy(unit);
                microInfo[6].updateEnemy(unit);
                microInfo[7].updateEnemy(unit);
                microInfo[8].updateEnemy(unit);
            }

            units = rc.senseNearbyRobots(myVisionRange, rc.getTeam());
            for (RobotInfo unit : units) {
                if (Clock.getBytecodesLeft() < MAX_MICRO_BYTECODE_REMAINING) break;
                microInfo[0].updateAlly(unit);
                microInfo[1].updateAlly(unit);
                microInfo[2].updateAlly(unit);
                microInfo[3].updateAlly(unit);
                microInfo[4].updateAlly(unit);
                microInfo[5].updateAlly(unit);
                microInfo[6].updateAlly(unit);
                microInfo[7].updateAlly(unit);
                microInfo[8].updateAlly(unit);
            }
            MicroInfo bestMicro = microInfo[8];
            for (int i = 0; i < 8; ++i) {
                if (microInfo[i].isBetter(bestMicro)) bestMicro = microInfo[i];
            }
            if (bestMicro.dir == Direction.CENTER) return false;

            if (rc.canMove(bestMicro.dir)) {
                rc.move(bestMicro.dir);
                return true;
            }

        } catch (Exception e){
            e.printStackTrace();
        }
        return false;
    }

    public int paintScan(RobotController rc, MapLocation l) throws GameActionException {
        int nearbyEnemyPaint = 0;
        for (Direction dir : RobotPlayer.directions) {
            MapLocation loc = l.add(dir);
            if (!rc.canSenseLocation(loc)) continue;
            MapInfo mi = rc.senseMapInfo(loc);
            if (cache.contains(loc)) {
                nearbyEnemyPaint += cache.getVal(loc);
            }
            else {
                int val = mi.getPaint().isEnemy()? 1 : 0;
                cache.add(loc, val);
                nearbyEnemyPaint += val;
            }
        }
        return nearbyEnemyPaint;
    }

    // Factors:
    // Paint penalty: weighted as one
    // Nearby Enemy paint: We take 2*(#number of nearby enemy tiles)
    // minDistanceToEnemyPaint: weighted as 1* (negligible for nearby) EnemyPaint = target
    //
    class MicroInfo {
        Direction dir;
        MapLocation location;
        double minDistanceToEnemy = INF;
        boolean canMove = true;
        int penalty = 0;
        PaintType pt;
        boolean inTowerRange = false;
        int distanceToTarget;
        int numMoppers = 0;
        int pti = 0;

        public MicroInfo(RobotController rc, Direction dir, MapLocation target) throws GameActionException {
            this.dir = dir;
            this.location = rc.getLocation().add(dir);
            if (dir != Direction.CENTER && !rc.canMove(dir)) {
                canMove = false;
                return;
            }
            MapInfo mi = rc.senseMapInfo(this.location);
            distanceToTarget = target.distanceSquaredTo(this.location);
            pt = rc.senseMapInfo(location).getPaint();


            if (pt.isEnemy()) {
                penalty = 4;
                pti = 2;
            }
            if (pt == PaintType.EMPTY) {
                penalty = 2;
                pti = 1;
            }

            //paintScan(rc, location);
        }

        void updateEnemy(RobotInfo unit){
            if (!canMove) return;
            if (unit.getType() == UnitType.SPLASHER) {
                int dist = unit.getLocation().distanceSquaredTo(location);
                if (dist < minDistanceToEnemy) minDistanceToEnemy = dist;
            }
            if (unit.getType() == UnitType.MOPPER) {
                if (unit.getLocation().isAdjacentTo(location)) numMoppers++;
            }
            if (unit.getType().isTowerType() && Util.inTowerRange(location, unit)) {
                inTowerRange = true;
            }
        }

        void updateAlly(RobotInfo unit){
            if (!canMove) return;
            if(unit.getLocation().isWithinDistanceSquared(location, 2))
                penalty += pt.isEnemy() ? 2 : 1;
        }

        //equal => true
        public boolean isBetter (MicroInfo M) throws GameActionException {
            if(canMove && !M.canMove) return true;
            if(!canMove && M.canMove) return false;

            if (distanceToTarget > 2 && M.distanceToTarget > 2) {
                if (distanceToTarget < M.distanceToTarget) return true;
                if (M.distanceToTarget < distanceToTarget) return false;
            }

            if (inTowerRange && !M.inTowerRange) return false;
            if (!inTowerRange && M.inTowerRange) return true;

            if (numMoppers < M.numMoppers) return true;
            if (M.numMoppers < numMoppers) return false;

            if (penalty < M.penalty) return true;
            if (M.penalty < penalty) return false;

            if (pti < M.pti) return true;
            if (M.pti < pti) return false;

            return true;
        }
        public double valueHeuristic (MicroInfo M) throws GameActionException {
            return M.penalty - M.minDistanceToEnemy - paintScan(rc, M.location);
        }
        // Prioritise low distance to Target, low paint penalty, low distance to Enemy
        // tweak numbers later?
    }

}