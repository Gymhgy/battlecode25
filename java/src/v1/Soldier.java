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
    };
    static int[][] paintTowerPattern = {
            {2, 1, 1, 1, 2},
            {1, 2, 1, 2, 1},
            {1, 1, 0, 1, 1},
            {1, 2, 1, 2, 1},
            {2, 1, 1, 1, 2},
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
    static UnitType curRuinType = null;
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
                    if (tile.getMark().isSecondary()) {
                        curRuinType = UnitType.LEVEL_ONE_PAINT_TOWER;
                    } else {
                        curRuinType = (FastMath.rand256() % 3 < 1) ?
                                UnitType.LEVEL_ONE_PAINT_TOWER :
                                UnitType.LEVEL_ONE_MONEY_TOWER;
                    }
                    if (curRuinType == UnitType.LEVEL_ONE_PAINT_TOWER && !tile.getMark().isSecondary()) {
                        if (rc.canMark(tile.getMapLocation())) rc.mark(tile.getMapLocation(), true);
                    }
                }
            }
        }

        if (curRuin != null) {
            if (ruinCheck(rc, curRuin)) {
                tryBuild(rc);
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

    static void tryBuild(RobotController rc) throws GameActionException {
        if (rc.getChips() > 1000 && rc.canCompleteTowerPattern(curRuinType, curRuin.getMapLocation())) {
            rc.completeTowerPattern(curRuinType, curRuin.getMapLocation());
            curRuin = null;
            return;
        }
        Pathfinding.moveToward(rc, curRuin.getMapLocation());

        if (!rc.isActionReady()) return;
        int[][] tower = curRuinType == UnitType.LEVEL_ONE_MONEY_TOWER ? moneyTowerPattern : paintTowerPattern;
        // Paint under self first... then 5x5
        // Paint under self
        MapLocation myLoc = rc.getLocation();
        if (Util.canPaintReal(rc, myLoc) && myLoc.isWithinDistanceSquared(curRuin.getMapLocation(), 8)){
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
                if (Util.canPaintReal(rc, loc)) {
                    MapInfo mi = rc.senseMapInfo(loc);
                    PaintType ideal = numToPaint(tower[i + 2][j + 2]);
                    if (!mi.getPaint().equals(ideal) && rc.isActionReady()) {
                        rc.attack(loc, ideal.isSecondary());
                        break paintLoop;
                    }
                }
            }
        }
    }


    static int[][] SRP = {
           /* {2, 1, 2, 1, 2},
            {1, 2, 1, 2, 1},
            {2, 1, 1, 1, 2},
            {1, 2, 1, 2, 1},
            {2, 1, 2, 1, 2}*/
            {1, 2, 1, 2, 1},
            {2, 1, 2, 1, 2},
            {1, 2, 2, 2, 1},
            {2, 1, 2, 1, 2},
            {1, 2, 1, 2, 1},
    };
    // true is secondary
    static boolean trySRP(MapLocation loc) {
        return SRP[loc.x % 5][loc.y % 5] == 2;
    }

    static void paintRandomly(RobotController rc) throws  GameActionException {
        MapLocation myLoc = rc.getLocation();
        if(Util.canPaintReal(rc, myLoc) && !rc.senseMapInfo(myLoc).getPaint().isAlly()) {
            rc.attack(rc.getLocation(), trySRP(myLoc));
            return;
        }

        MapLocation[] ruins = rc.senseNearbyRuins(-1);
        MapLocation[] nearby = rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), UnitType.SOLDIER.actionRadiusSquared);
        for (MapLocation loc : nearby) {
            if (!Util.canPaintReal(rc, loc)) continue;
            if (rc.senseMapInfo(loc).getPaint() == PaintType.EMPTY) {
                rc.attack(loc, trySRP(loc));
                return;
            }
        }
        for(MapLocation loc : nearby) {
            boolean canOverwrite = true;
            if (!Util.canPaintReal(rc, loc)) continue;
            boolean ideal = trySRP(loc);
            if (rc.senseMapInfo(loc).getPaint().isSecondary() == ideal) {
                continue;
            }
            for (MapLocation r : ruins) {
                RobotInfo ri = rc.senseRobotAtLocation(r);
                if (!(ri != null && ri.getType().isTowerType() && ri.getTeam() == rc.getTeam())) {
                    canOverwrite = false;
                }
            }
            if(canOverwrite) {
                rc.attack(rc.getLocation(), trySRP(loc));
                return;
            }

        }
    }

    static void endTurn(RobotController rc) throws GameActionException {
        if (curRuin != null) {
            rc.setIndicatorLine(rc.getLocation(), curRuin.getMapLocation(), 255, 0, 0);
        }
        rc.setIndicatorString(indicator);
    }
}
