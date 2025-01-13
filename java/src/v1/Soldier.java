package v1;

import battlecode.common.*;
import v1.fast.FastMath;

public class Soldier {

    static int[][] moneyTowerPattern = {
            {1, 2, 2, 2, 1},
            {2, 2, 1, 2, 2},
            {2, 1, 0, 1, 2},
            {2, 2, 1, 2, 2},
            {1, 2, 2, 2, 1},
            {1}
    };
    static int[][] paintTowerPattern = {
            {2, 1, 1, 1, 2},
            {1, 2, 1, 2, 1},
            {1, 1, 0, 1, 1},
            {1, 2, 1, 2, 1},
            {2, 1, 1, 1, 2},
            {2}
    }; // I dont even know what pattern the defensive tower is LMAO
    public static UnitType iToTower(int t) {
        switch(t) {
            case 1:
                return UnitType.LEVEL_ONE_MONEY_TOWER;
            case 2:
                return UnitType.LEVEL_ONE_PAINT_TOWER;
        }
        return null;
    }
    public static int[][] towerPattern(int t){
        switch(t) {
            case 1:
                //System.out.println("Money");
                return moneyTowerPattern;
            case 0:
                //System.out.println("Paint");
                return paintTowerPattern;
        }
        return null;
    }
    static PaintType numToPaint(int num) {
        switch (num) {
            case 0:
                return PaintType.EMPTY;
            case 1:
                return PaintType.ALLY_PRIMARY;
            case 2:
                return PaintType.ALLY_SECONDARY;
        }
        return PaintType.EMPTY;
    }
    static int buildCost = 24 * 5 + 40; // How much it costs to build a tower + 40 for traversal
    static int paintCap = 200;
    static int sel = 1;
    static String indicator = "";
    static MapInfo curRuin = null;
    static MapInfo[] nearbyTiles;
    static RobotInfo[] nearbyRobots;

    static void init(RobotController rc) throws GameActionException {

    }

    static void run(RobotController rc) throws GameActionException {
        nearbyTiles = rc.senseNearbyMapInfos();
        indicator = "";
        if (curRuin == null) {
            for (MapInfo tile : nearbyTiles) {
                if (tile.getMapLocation().isWithinDistanceSquared(rc.getLocation(), 2)) {
                    continue;
                }
                if (ruinCheck(rc, tile)) {
                    curRuin = tile;
                }
            }
        }

        if (curRuin != null) {
            if (ruinCheck(rc, curRuin)) {
                tryBuild(rc, towerPattern(sel));
            } else {
                curRuin = null;
            }
        }

        if (curRuin == null) {
            Pathfinding.navigateRandomly(rc);
            if (rc.isActionReady()) {
                paintRandomly(rc);
            }
        }

        endTurn(rc);
    }

    static boolean ruinCheck(RobotController rc, MapInfo ruin) throws GameActionException {
        if (!ruin.hasRuin()) return false;
        MapLocation ruinLoc = ruin.getMapLocation();

        if (!rc.canSenseLocation(ruinLoc)) {
            // uh we can't see the damn ruin... return true to be safe
            return true;
        }
        RobotInfo ri = rc.senseRobotAtLocation(ruinLoc);
        if (ri != null && ri.getType().isTowerType()) {
            return false;
        }
        if (!rc.getLocation().isWithinDistanceSquared(ruinLoc, 2)) {
            int soldierCount = 0;

            // TODO: unroll this loop, uses so much bytecode rn
            MapLocation[] surroundingLocations = rc.getAllLocationsWithinRadiusSquared(ruinLoc, 2);
            for (MapLocation loc : surroundingLocations) {
                if (rc.canSenseLocation(loc) && rc.isLocationOccupied(loc)) {
                    RobotInfo robot = rc.senseRobotAtLocation(loc);
                    if (robot.getType() == UnitType.SOLDIER && robot.getTeam() == rc.getTeam()) {
                        soldierCount++;
                    }
                }
                if (soldierCount >= 2) {
                    return false;
                }
            }
        }

        return true;
    }

    static void tryBuild(RobotController rc, int[][] tower) throws GameActionException {
        if (rc.getChips() > 1000 && rc.canCompleteTowerPattern(iToTower(tower[5][0]), curRuin.getMapLocation())) {
            rc.completeTowerPattern(iToTower(tower[5][0]), curRuin.getMapLocation());
            System.out.println("Trying to complete");
            curRuin = null;
            return;
        }
        Pathfinding.moveToward(rc, curRuin.getMapLocation());

        if (!rc.isActionReady()) return;

        // Paint under self first... then 5x5
        // Paint under self
        MapLocation myLoc = rc.getLocation();
        if (canPaintReal(rc, myLoc) && myLoc.isWithinDistanceSquared(curRuin.getMapLocation(), 8)){
            MapLocation delta = FastMath.minusVec(curRuin.getMapLocation(), myLoc);
            PaintType ideal = numToPaint(tower[delta.x + 2][delta.y + 2]);
            if (!rc.senseMapInfo(myLoc).getPaint().equals(ideal)) {
                //System.out.println(rc.getLocation().toString() + " Painting at my position: " + myLoc);
                rc.attack(myLoc, ideal.isSecondary());
            }
        }
        // else find 5x5
        paintLoop:
        for (int i = -2; i <= 2; i++) {
            for (int j = -2; j <= 2; j++) {
                if (i == 0 && j == 0) continue;
                MapLocation loc = FastMath.addVec(curRuin.getMapLocation(), new MapLocation(i, j));
                if (canPaintReal(rc, loc)) {
                    MapInfo mi = rc.senseMapInfo(loc);
                    PaintType ideal = numToPaint(tower[i + 2][j + 2]);
                    if (!mi.getPaint().equals(ideal) && rc.isActionReady()) { // I dont understnad why is Action Ready needs to be checked here but
                        //System.out.println(rc.getLocation().toString() + " Painting at: " + loc);
                        rc.attack(loc, ideal.isSecondary());
                        break paintLoop;
                    }
                } else if (rc.getPaint() < 6) {
                    rc.disintegrate();
                }
            }
        }
    }

    static void paintRandomly(RobotController rc) throws  GameActionException {
        if(canPaintReal(rc, rc.getLocation()) && !rc.senseMapInfo(rc.getLocation()).getPaint().isAlly()) {
            //System.out.println(rc.getLocation().toString() + " Painting at myself");
            rc.attack(rc.getLocation());
            return;
        }

        for(MapLocation loc : rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), UnitType.SOLDIER.actionRadiusSquared)) {

            //TODO: intelligent painting behavior
        }
    }

    static void endTurn(RobotController rc) throws GameActionException {
        if (curRuin != null) {
            rc.setIndicatorLine(rc.getLocation(), curRuin.getMapLocation(), 255, 0, 0);
        }
        rc.setIndicatorString(indicator);
    }
    static boolean canPaintReal(RobotController rc, MapLocation loc) throws GameActionException { // canPaint that checks for cost
        int paintCap = rc.getPaint();
        return paintCap > rc.getType().attackCost && rc.canPaint(loc);

    }
}
