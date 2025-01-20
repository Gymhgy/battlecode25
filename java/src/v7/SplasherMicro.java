package v7;
import v7.fast.*;
import battlecode.common.*;

public class SplasherMicro {

    // What do we want Splasher Micro to do?
    // Notes first:
    // Big Idea: Best case scenario, Splashers should NEVER wander.
    // Keep track of 5 potential targets. Add to heuristic for enemy detected, "Amount possible"
    public int INF = 100000;
    public boolean canAttack;
    static double currentActionRadius = UnitType.SPLASHER.actionRadiusSquared;

    public TargetInfo[] targetInfo = new TargetInfo[5]; // 5 is arbitrary, might put more
    public int targetCount;
    boolean shouldPlaySafe = false; // We should probably use this sometime
    static int myVisionRange = GameConstants.VISION_RADIUS_SQUARED;
    static final int RANGE = UnitType.SPLASHER.actionRadiusSquared;
    final static int MAX_MICRO_BYTECODE_REMAINING = 5000;
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
    public static RobotController rc;
    SplasherMicro(RobotController rc) {
        SplasherMicro.rc = rc;
    }
    void greedyMicro(RobotController rc, int scanInfo, MapLocation lastAttack) {
        // Why pass in scanInfo? -> We have current information around target from our attack scan already.
        // lastAttack can be null.
        try {
            replaceTargets(rc, scanInfo);
            if (targetCount == 0) normalMicro(rc, lastAttack);
            shouldPlaySafe = false;
            RobotInfo[] units = rc.senseNearbyRobots(myVisionRange, rc.getTeam().opponent());
            canAttack = rc.isActionReady(); // Why is this here? Does anyone know
            for (RobotInfo unit : units) {
                if (Clock.getBytecodesLeft() < MAX_MICRO_BYTECODE_REMAINING) break;
                for (int i = 0; i < 5; i++) {
                    if (targetInfo[i] != null) {
                        targetInfo[i].updateEnemy(unit);
                    }
                }
            }
            MapLocation loc = rc.getLocation();
            for (int i = 0; i < 5; i++) {
                if (targetInfo[i] != null) {
                    targetInfo[i].updateSquares(scanInfo, loc);
                    targetInfo[i].deteriorateValues(rc);
                }
            }

            if (targetCount == 0) {
                // Pointless check but like
            } else {
                TargetInfo bestTarget = findBestTarget(rc);
                if (rc.isMovementReady()) {
                    Pathfinding.moveToward(rc, bestTarget.location);
                }
            }

        } catch (Exception e){
            e.printStackTrace();
        }
    }
    public void replaceTargets(RobotController rc, int scanInfo) throws GameActionException {
        // When do we want to replace targets? W
        for (int i = 0; i < 5; i++) {
            if (targetInfo[i] == null || targetInfo[i].value < -20) { // Or some arbitrary amount: Figure it out later!
                if (Communicator.targets.size > 0) {
                    MapLocation newTarget = Communicator.relayTarget(rc);
                    targetInfo[i] = new TargetInfo(newTarget, 0, rc.getRoundNum()); // Give it high priority?
                } else if (targetInfo[i] != null && targetInfo[i].value < -20) {
                    targetInfo[i] = null;
                    targetCount -= 1;
                }
            }
        }
    }
    public TargetInfo findBestTarget(RobotController rc) {
        TargetInfo bestClump = null;
        int val = INF;
        for (TargetInfo m: targetInfo) {
            if (m != null) {
                if (val > m.value + rc.getLocation().distanceSquaredTo(m.location)) {
                    bestClump = m;
                    val = bestClump.value;
                }
            }
        }
        return bestClump;
    }
    void showTargets(RobotController rc) throws GameActionException {
        for(TargetInfo m: targetInfo) {
            rc.setIndicatorLine(rc.getLocation(), m.location, 0, 0, 255);
        }
    }
    public FastLocIntMap cache;
    boolean normalMicro(RobotController rc, MapLocation target) {
        try {
            cache = new FastLocIntMap();
            if (!rc.isMovementReady()) return false;
            shouldPlaySafe = false;
            RobotInfo[] units = rc.senseNearbyRobots(myVisionRange, rc.getTeam().opponent());
            if (units.length == 0) return false;
            if(units.length == 1 &&  target != null && rc.getLocation().isWithinDistanceSquared(target, 20)) {
                if (!units[0].getLocation().isWithinDistanceSquared(rc.getLocation(), 6))
                    return false;
            }
            canAttack = rc.isActionReady();

            SplasherMicro.MicroInfo[] microInfo = new SplasherMicro.MicroInfo[9];
            for (int i = 0; i < 9; ++i) {
                microInfo[i] = new SplasherMicro.MicroInfo(rc, dirs[i], target);
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
            SplasherMicro.MicroInfo bestMicro = microInfo[8];
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

        class TargetInfo {
            MapLocation location;
            int value = 0;
            int roundCreated = 0;
            // If we even care about roundCreated.
            public TargetInfo(MapLocation coordinate, int val, int roundCreated) throws GameActionException {
                location = coordinate;
                value = val;
                roundCreated = roundCreated;
            }
            // Avoid Soldiers, other splashers?
            void updateEnemy(RobotInfo unit){
                if (!unit.getLocation().isWithinDistanceSquared(location, 20)) {
                    if (unit.getType() == UnitType.SOLDIER) {
                        value = value + 2;
                    } else {
                        value++;
                    }
                }
            }
            void updateSquares(int scanInfo, MapLocation loc) {
                if(!loc.isWithinDistanceSquared(location, 5)) {
                    value = value + scanInfo;
                }
            }
            void deteriorateValues(RobotController rc) {
                this.value = value - (rc.getRoundNum() -roundCreated)/10 ;
            }


        }
    public int paintLoss(RobotController rc, MapLocation n) throws GameActionException {
        int squareLoss;
        PaintType squarePaint = rc.senseMapInfo(n).getPaint();
        if (squarePaint.isEnemy()) {
            squareLoss = 2;
        } else if (squarePaint.isAlly()) {
            squareLoss = 0;
        } else {
            squareLoss = 1;
        }
        return squareLoss;
    }
    static int[][] coordinates = {
            {0, 0}, {-1, 0}, {1, 0}, {0, -1}, {0, 1},
            {-1, -1}, {-1, 1}, {1, -1}, {1, 1},
    };
    public int paintScan(RobotController rc, MapLocation l) throws GameActionException {
        int nearbyEnemyPaint = 0;
        for (int i = coordinates.length; i-->0;) { // Fuck, this
            MapLocation loc = l.translate(coordinates[i][0], coordinates[i][1]);
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
    class MicroInfo {
        Direction dir;
        MapLocation location;
        double minDistanceToEnemy = currentActionRadius;
        boolean canMove = true;
        int penalty = 0;
        PaintType pt;
        boolean inTowerRange = false;
        int distanceToTarget;

        public MicroInfo(RobotController rc, Direction dir, MapLocation target) throws GameActionException {
            this.dir = dir;
            this.location = rc.getLocation().add(dir);
            distanceToTarget = target.distanceSquaredTo(this.location);
            pt = rc.senseMapInfo(location).getPaint();
            if (dir != Direction.CENTER && !rc.canMove(dir)) {
                canMove = false;
                return;
            }
            paintScan(rc, location);
            penalty = paintLoss(rc, location);
        }

        void updateEnemy(RobotInfo unit){
            if (!canMove) return;
            if (unit.getType() == UnitType.SPLASHER) {
                int dist = unit.getLocation().distanceSquaredTo(location);
                if (dist < minDistanceToEnemy) minDistanceToEnemy = dist;
            }
            if (unit.getType().isTowerType()) inTowerRange = true;
        }

        void updateAlly(RobotInfo unit){
            if (!canMove) return;
            if(unit.getLocation().isWithinDistanceSquared(location, 2))
                penalty += pt.isEnemy() ? 2 : 1;
        }

        //equal => true
        public boolean isBetter (SplasherMicro.MicroInfo M) throws GameActionException {
            if(canMove && !M.canMove) return true;
            if(!canMove && M.canMove) return false;
            return valueHeuristic(this) < valueHeuristic(M);

            //if (inTowerRange && !M.inTowerRange) return false;
            //if (!inTowerRange && M.inTowerRange) return true;

            //if (penalty < M.penalty) return true;
            //if (M.penalty < penalty) return false;

        }
        public double valueHeuristic (SplasherMicro.MicroInfo M) throws GameActionException {
            return M.penalty - M.minDistanceToEnemy - paintScan(rc, M.location);
        }
        // Prioritise low distance to Target, low paint penalty, low distance to Enemy
        // tweak numbers later?
    }

    }
    // Big Idea: Splashers should ALWAYS be moving towards paint targets, if not enemy towers.
    // Because you can get Soldiers to attack enemy towers instead!
    // In Communicator: Keep Paint clump



