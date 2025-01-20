package v3;

import battlecode.common.*;

//TODO: revisit at some point
public class MopperMicro {

    final int INF = 1000000;
    boolean shouldPlaySafe = false;
    static int myRange;
    static int myVisionRange;

    static final int RANGE_LAUNCHER = 4;

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

    MopperMicro(RobotController rc){
        this.rc = rc;
        myRange = 2;
        myVisionRange = 20;
    }

    static double currentDPS = 0;
    static double currentActionRadius;
    static boolean canAttack;

    boolean doMicro(MapLocation target){
        try {
            if (!rc.isMovementReady()) return false;
            shouldPlaySafe = false;
            RobotInfo[] units = rc.senseNearbyRobots(myVisionRange, rc.getTeam().opponent());
            if (units.length == 0) return false;

            if(units.length == 1 &&  target != null && rc.getLocation().isWithinDistanceSquared(target, 20)) {
                if(!units[0].getLocation().isWithinDistanceSquared(rc.getLocation(), 6))
                    return false;
            }

            canAttack = rc.isActionReady();


            TargetInfo[] microInfo = new TargetInfo[9];
            for (int i = 0; i < 9; ++i) microInfo[i] = new TargetInfo(dirs[i]);

            for (RobotInfo unit : units) {
                if (Clock.getBytecodesLeft() < MAX_MICRO_BYTECODE_REMAINING) break;

                currentActionRadius = 4;
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

            TargetInfo bestMicro = microInfo[8];
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
        Direction dir;
        MapLocation location;
        int minDistanceToEnemy = INF;
        boolean canMove = true;
        int penalty = 0;
        PaintType pt;
        boolean inTowerRange = false;

        public TargetInfo(Direction dir) throws GameActionException {
            this.dir = dir;
            this.location = rc.getLocation().add(dir);


            if (dir != Direction.CENTER && !rc.canMove(dir)) {
                canMove = false;
                return;
            }
            MapInfo mi = rc.senseMapInfo(this.location);
            pt = mi.getPaint();
            if (pt.isEnemy()) penalty = 4;
            if (pt == PaintType.EMPTY) penalty = 2;
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
        boolean isBetter(TargetInfo M){
            if(canMove && !M.canMove) return true;
            if(!canMove && M.canMove) return false;

            if (inTowerRange && !M.inTowerRange) return false;
            if (!inTowerRange && M.inTowerRange) return true;

            if (penalty < M.penalty) return true;
            if (M.penalty < penalty) return false;



            return true;
        }
    }

}