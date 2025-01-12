package v1;

import battlecode.common.*;

public class Soldier {

    static int[][] moneyTowerPattern = {
            {1, 2, 2, 2, 1},
            {2, 2, 1, 2, 2},
            {2, 1, 0, 1, 2},
            {2, 2, 1, 2, 2},
            {1, 2, 2, 2, 1}
    };
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
    static String indicator = "";
    static MapInfo curRuin = null;
    static MapInfo[] nearbyTiles;
    static void run(RobotController rc) throws GameActionException {
        nearbyTiles = rc.senseNearbyMapInfos();
        indicator = "";
        if (curRuin == null) {
            for (MapInfo tile : nearbyTiles) {
                if (tile.hasRuin()) {
                    RobotInfo ri = rc.senseRobotAtLocation(tile.getMapLocation());
                    if (ri == null || !ri.getType().isTowerType()) {
                        curRuin = tile;
                        indicator = curRuin.toString();
                        rc.setTimelineMarker("RUIN DETECTED", 255, 255, 255);
                    }
                }
            }
        }

        if (curRuin != null) {
            RobotInfo ri = rc.senseRobotAtLocation(curRuin.getMapLocation());
            if (ri == null || !ri.getType().isTowerType()) {
                tryBuild(rc);
            } else {
                curRuin = null;
            }
        }
        else {
            Pathfinding.navigateRandomly(rc);
            if (rc.isActionReady()) {
                paintRandomly(rc);
            }
        }

        endTurn(rc);
    }

    static void tryBuild(RobotController rc) throws GameActionException {
        if (rc.canCompleteTowerPattern(UnitType.LEVEL_ONE_MONEY_TOWER, curRuin.getMapLocation())) {
            rc.completeTowerPattern(UnitType.LEVEL_ONE_MONEY_TOWER, curRuin.getMapLocation());
            curRuin = null;
            return;
        }

        Pathfinding.moveToward(rc, curRuin.getMapLocation());

        if (!rc.isActionReady()) return;

        // Paint under self first... then 5x5
        paintLoop:
        for (int i = -2; i <= 2; i++) {
            for (int j = -2; j <= 2; j++) {
                if (i == 0 && j == 0) continue;
                MapLocation loc = FastMath.addVec(curRuin.getMapLocation(), new MapLocation(i, j));
                if (rc.canPaint(loc)) {
                    MapInfo mi = rc.senseMapInfo(loc);
                    PaintType ideal = numToPaint(moneyTowerPattern[i + 2][j + 2]);
                    if (!mi.getPaint().equals(ideal)) {
                        System.out.println(rc.getLocation().toString() + " Painting at: " + loc);
                        rc.attack(loc, ideal.isSecondary());
                        break paintLoop;
                    }
                }
            }
        }
    }

    static void paintRandomly(RobotController rc) throws  GameActionException {
        if(rc.canPaint(rc.getLocation())) {
            System.out.println(rc.getLocation().toString() + " Painting at myself");
            rc.attack(rc.getLocation());
            return;
        }

        for(MapLocation loc : rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), UnitType.SOLDIER.actionRadiusSquared)) {

        }
    }

    static void endTurn(RobotController rc) throws GameActionException {
        rc.setIndicatorString(indicator);
    }
}
