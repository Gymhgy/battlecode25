package v8o1;

import battlecode.common.*;

public class SoldierMicro {

    final int INF = 1000000;
    boolean shouldPlaySafe = false;
    static int myVisionRange = GameConstants.VISION_RADIUS_SQUARED;
    static final int RANGE = UnitType.SOLDIER.actionRadiusSquared;

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

    SoldierMicro(RobotController rc){
        this.rc = rc;
    }

    static boolean canAttack;


    static MapLocation enemyTower;
    boolean doMicro(MapLocation target){
        try {
            if (!rc.isMovementReady()) return false;
            shouldPlaySafe = false;
            RobotInfo[] units = rc.senseNearbyRobots(myVisionRange, rc.getTeam().opponent());

            canAttack = rc.isActionReady();
            enemyTower = target;

            MicroInfo[] microInfo = new MicroInfo[9];
            for (int i = 0; i < 9; ++i) microInfo[i] = new MicroInfo(dirs[i]);

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



    class MicroInfo {
        Direction dir;
        MapLocation location;
        int numMoppers = 0;
        boolean canMove = true;
        int penalty = 0;
        PaintType pt;
        boolean inTowerRange = false;
        int distToTower = INF;
        int pti = 0;
        public MicroInfo(Direction dir) throws GameActionException {
            this.dir = dir;
            this.location = rc.getLocation().add(dir);

            if (dir != Direction.CENTER && !rc.canMove(dir)) {
                canMove = false;
                return;
            }
            MapInfo mi = rc.senseMapInfo(this.location);
            if (enemyTower.isWithinDistanceSquared(location, RANGE)) inTowerRange = true;
            distToTower = enemyTower.distanceSquaredTo(location);
            pt = mi.getPaint();
            if (pt.isEnemy()) {
                penalty = 4;
                pti = 2;
            }
            if (pt == PaintType.EMPTY) {
                penalty = 2;
                pti = 1;
            }

        }

        void updateEnemy(RobotInfo unit){
            if (!canMove) return;
            if (unit.getType() == UnitType.MOPPER) {
                if (unit.getLocation().isWithinDistanceSquared(location, 2)) numMoppers++;
            }
        }

        void updateAlly(RobotInfo unit){
            if (!canMove) return;
            if(unit.getLocation().isWithinDistanceSquared(location, 2))
                penalty += pt.isEnemy() ? 2 : 1;
        }

        // What info do we need to collect?
        // How far are we from target?
        // type of paint
        // how many enemy moppers
        // paint penalty from allies
        //equal => true
        boolean isBetter(MicroInfo M){
            if(canMove && !M.canMove) return true;
            if(!canMove && M.canMove) return false;

            if(canAttack) {
                if (inTowerRange && !M.inTowerRange) return true;
                if (!inTowerRange && M.inTowerRange) return false;
            }
            else {
                if (inTowerRange && !M.inTowerRange) return false;
                if (!inTowerRange && M.inTowerRange) return true;
            }

            //Aggressive...
            if (distToTower < M.distToTower) return true;
            if (M.distToTower < distToTower) return false;

            if (numMoppers < M.numMoppers) return true;
            if (M.numMoppers < numMoppers) return false;

            if (penalty < M.penalty) return true;
            if (M.penalty < penalty) return false;

            if (pti < M.pti) return true;
            if (M.pti < pti) return false;

            return true;
        }
    }

}