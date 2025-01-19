package v7;
import v7.fast.*;
import battlecode.common.*;

public class SplasherMicro {

    // What do we want Splasher Micro to do?
    // Notes first:
    // Big Idea: Best case scenario, Splashers should NEVER wander.
    // Keep track of 4 'Enemy Paint clumps" At all times. Add to heuristic for enemy detected, "Amount possible"
    public int INF = 100000;
    public MicroInfo[] targetInfo = new MicroInfo[5]; // 5 is arbitrary, might put more
    public boolean canAttack;
    boolean shouldPlaySafe = false; // We should probably use this sometime
    static int myVisionRange = GameConstants.VISION_RADIUS_SQUARED;
    static final int RANGE = UnitType.SPLASHER.actionRadiusSquared;
    final static int MAX_MICRO_BYTECODE_REMAINING = 5000;
    static RobotController rc;
    boolean greedyMicro(RobotController rc, int scanInfo) {
        // Why pass in scanInfo? -> We have current information for our attack scan already.
        try {
            shouldPlaySafe = false;
            RobotInfo[] units = rc.senseNearbyRobots(myVisionRange, rc.getTeam().opponent());
            canAttack = rc.isActionReady(); // Why is this here? Doesn anyone know
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
                }
            }

            // If current location works better as a clump center... replace the worst clump in the list
            if (replaceTarget(rc, scanInfo)) {
                return false; // for no real reason honestly
            } else {
                MicroInfo bestTarget = findBestTarget(rc);
                if (rc.isMovementReady()) {
                    Pathfinding.moveToward(rc, bestTarget.location);
                }
            }


        } catch (Exception e){
            e.printStackTrace();
        }
        return false;
    }
    public boolean replaceTarget(RobotController rc, int scanInfo) {
        // Fuck. Isn't this going to be
        // V1: Only current Splasher location.
        for (MicroInfo m: targetInfo) {
        }
        return true;
    }
    public MicroInfo findBestTarget(RobotController rc) {
        MicroInfo bestClump = null;
        int val = INF;
        for (MicroInfo m: targetInfo) {
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
        for(MicroInfo m: targetInfo) {
            rc.setIndicatorLine(rc.getLocation(), m.location, 0, 0, 255);
        }
    }

        class MicroInfo {
            MapLocation location;
            int value = 0;
            int roundCreated = 0;
            public MicroInfo(MapLocation coordinate, int val) throws GameActionException {
                location = coordinate;
                value = val;
            }
            // Avoid Soldiers, other splashers/
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
            public int findValue() {
                return 0;
            }
            void deteriorateValue () {
                value = value - 10;
                // If value drops to.. below
            }



        }

    }
    // Big Idea: Splashers should ALWAYS be moving towards paint targets, if not enemy towers.
    // Because you can get Soldiers to attack enemy towers instead!
    // In Communicator: Keep Paint clump



