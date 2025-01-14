package v1;

import battlecode.common.*;
import v1.fast.FastLocSet;
import v1.fast.FastMath;

import java.util.*;

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
    static MapLocation curSRP = null;
    static FastLocSet possibleSRPs = new FastLocSet();
    static String possibleSRPstr = "^\u0005\u001F^\u0003#^!7^#)^\u00066^$\"^+\u0019^ \u0002^&(^\"\b^#\u0015^\u001C\r\n^\u001A6^\u001E\u0010^\u0013\r\n^\u0016*^\u0012\r\n^\r\n$^\u0016\f^9\u0007^\u00119^\u000E\b^02^28^6\u0012^\u0002\f^\u0006\u000E^\u00037^\u00040^\u0007%^* ^-\u0015^+-^\u0018\u001C^$\u0004^'\u0017^\u00135^\u001A\"^9/^\u001E$^\u0011\u0011^\u000B'^9\u0011^\f ^\r\n\u001A^\u000B\u0013^3\u0013^\r\n\u0019^\u000E&^\u0004&^+\u0005^\u0007\u001B^$,^,\u001C^\u001F\t^\u001B9^\u00177^99^\b(^8\"^\u0010\u0018^\u0017-^\u001E8^\u0011%^\u0016\u0002^\b\u0014^\u000E0^0\r\n^2\u0010^7\u0015^5\u0005^(\u0006^\u0005)^73^\u00053^7)^,\u0012^/\u0007^&2^\u0019\u001F^%\u0007^&\u0014^\u001B\u0011^\u001D5^\u0017#^\u0013\u0003^\u0012\u0014^\u0017\u0019^\u0010,^\r\n#^\t\u0003^\f\u0016^4\u0016^0\u001E^(\u001A^//^\"0^  ^!-^%/^#3^-\u001F^\u0018\u0012^\u0013+^\u00164^\u0010\u0004^\u001D!^\t+^\u0011\u001B^\t\r\n^\u0017\u0005^\t\u0017^\u0003\u000F^5\u0019^3\t^\u0004\b^4\u0002^6\b^.\"^5-^*\u0002^/\u001B^(.^.\u0004^)1^'5^&\r\n^\u001C\u0014^\u001A\u0004^\u0019)^\u001E\u0006^\u001C2^\u001B/^\u0015\u001D^\u001C(^8\u0018^\u0014\u0006^\u0011/^\u000B1^\r\n\u0006^\u000F\u001F^1!^\u00024^2\u001A^.,^!#^!\u0005^$6^'+^%\u0011^&\u001E^\u001B\u0007^\u0014.^\u001E\u001A^\u0011\u0007^\u001F\u0013^\u0015\t^\r\n.^\b2^\u0012\u001E^\r\n-^\u000E\u0012^0(^\u000F\u000B^15^4\f^\u0005\u000B^31^\u0002 ^4*^\u0005\u0015^(\u0010^/%^ \u0016^**^+#^ \f^\u00079^%9^*4^-)^\u001A\u0018^\u0018\b^'\r\n^\u001D\u0017^\u00151^\u0013!^\u001A,^\u0014\u001A^\u001E.^\u001D+^9\u001B^\u000E\u001C^\f4^\u000B\t^2$^\u0003\u0005^\u0007\u0011^/\u0011^($^\"&^\u0007/^+7^,0^'\u0003^\u001D\u0003^\u001F\u001D^9%^8\u000E^\f*^\r\n\u0010^\u000F\u0015^1+^\r\n\u0005^\u000B\u001D^\u000F3^1\r\n^\u0003\u0019^5\u000F^/9^\u0004\u0012^44^\u0007\u0007^)\t^\u0006\u0018^6&^+\u000F^)\u0013^,\b^(8^\"\u0012^#\u000B^,&^!\u000F^\u0019\u000B^\u00180^86^\u0014$^\u0016\u0016^\u0010\"^\r\n8^\u0012(^\r\n7^\f\f^\u0002\u0016^0\u0014^7\u001F^\u0003-^3'^60^\u0002*^4 ^)\u001D^\u0006,^)'^!\u0019^#\u001F^$\u0018^-3^\u0019\u0015^\u001B\u001B^\u0013\u0017^\u0016 ^\u0014\u0010^\u00106^\f\u0002^2.^1\u0003^\u0002\u0002^\u0006\u0004^\u0004\u001C^ 4^7\u000B^57^*\f^ *^.\u000E^*\u0016^-\u000B^%%^$\u000E^\u001A\u000E^\u00148^\u00193^\u001D\r\n^\u001B%^\u0010\u000E^\u0015'^\t!^\b\r\n^\u001F1^8\u0004^\u0017\u000F^3\u001D^\r\n\u000F^\u000F)^1\u0017^6\u001C^.6^.\u0018^5#^\u0006\"^\"\u001C^'!^%\u001B^\u001C\u001E^\u0018&^8,^\u0015\u0013^\u001F'^\t5^\b\u001E^\u00122^2\u0006";
    static void init(RobotController rc) throws GameActionException {
        possibleSRPs.replace(possibleSRPstr);
    }

    static void run(RobotController rc) throws GameActionException {
        nearbyTiles = rc.senseNearbyMapInfos();
        indicator = "";
        if (curRuin == null && rc.getNumberTowers() < 25) {
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
                }
            }
        }
        if (rc.getNumberTowers() == 25) curRuin = null;

        if (curRuin != null) {
            if (curRuinType == UnitType.LEVEL_ONE_PAINT_TOWER && !curRuin.getMark().isSecondary()) {
                MapLocation toMark = null;
                boolean alrMarked = false;
                for (MapLocation loc : rc.getAllLocationsWithinRadiusSquared(curRuin.getMapLocation(), 2)) {
                    if (rc.canSenseLocation(loc) && rc.senseMapInfo(loc).getMark().isSecondary()) {
                        alrMarked = true;
                        break;
                    }
                    if (rc.canMark(loc)) {
                        toMark = loc;
                    }
                }
                if (!alrMarked && toMark != null) {
                    rc.mark(toMark, true);
                }
            }
            if (ruinCheck(rc, curRuin)) {
                tryBuild(rc);
            } else {
                curRuin = null;
            }
        }

        if (curRuin == null) {
            if (rc.getID() % 3 == 1) {
                if (curSRP != null && !canSRP(rc, curSRP)) curSRP = null;
                if (curSRP == null)
                    acquireSRP(rc);
                if (curSRP != null) {
                    tryBuildSRP(rc);
                }

            }
            if(curSRP == null) {
                MapInfo[] nearby = rc.senseNearbyMapInfos(UnitType.SOLDIER.actionRadiusSquared);
                int cnt = nearby.length;
                for (MapInfo mi : nearby) {
                    if (mi.isWall() || mi.hasRuin()) cnt--;
                    if (mi.getPaint() != PaintType.EMPTY) cnt--;
                }
                if (cnt == 0)
                    Pathfinding.navigateRandomly(rc);
                if (rc.isActionReady()) {
                    paintRandomly(rc);
                }
                if (cnt == 0)
                    Pathfinding.navigateRandomly(rc);
            }
        }

        supplyPaint(rc);
        record(rc);
        endTurn(rc);
    }

    private static void record(RobotController rc) {

    }


    private static void acquireSRP(RobotController rc) throws GameActionException {
        for (MapInfo mi : rc.senseNearbyMapInfos()) {
            if (canSRP(rc, mi.getMapLocation())) {
                curSRP = mi.getMapLocation();
                break;
            }
        }
    }

    private static void tryBuildSRP(RobotController rc) throws GameActionException {
        if (rc.canCompleteResourcePattern(curSRP)) {
            rc.completeResourcePattern(curSRP);
            badSRPs.add(curSRP);
            curSRP = null;
            return;
        }

        if(!rc.getLocation().equals(curSRP))
            Pathfinding.moveToward(rc, curSRP);
        MapLocation myLoc = rc.getLocation();
        if (canPaintReal(rc, myLoc) && myLoc.isWithinDistanceSquared(curSRP, 8)){
            boolean ideal = trySRP(myLoc);
            PaintType paintType = rc.senseMapInfo(myLoc).getPaint();
            if (paintType.isEnemy()) {

            }
            else if (paintType == PaintType.EMPTY || paintType.isSecondary() != ideal) {
                rc.attack(myLoc, ideal);
                return;
            }
        }
        // else find 5x5
        paintLoop:
        for (int i = -2; i <= 2; i++) {
            for (int j = -2; j <= 2; j++) {
                if (i == 0 && j == 0) continue;
                MapLocation loc = FastMath.addVec(curSRP, new MapLocation(i, j));
                if (canPaintReal(rc, loc)) {
                    boolean ideal = trySRP(loc);
                    PaintType paintType = rc.senseMapInfo(loc).getPaint();
                    if (paintType.isEnemy()) {

                    }
                    else if (paintType == PaintType.EMPTY || paintType.isSecondary() != ideal) {
                        rc.attack(loc, ideal);
                        return;
                    }
                }
            }
        }
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
        int soldierCount = 0;

        // TODO: unroll this loop, uses so much bytecode rn
        MapLocation[] surroundingLocations = rc.getAllLocationsWithinRadiusSquared(ruinLoc, 2);
        for (MapLocation loc : surroundingLocations) {
            if (rc.canSenseLocation(loc) && rc.isLocationOccupied(loc)) {
                RobotInfo robot = rc.senseRobotAtLocation(loc);
                if (robot.getType() == UnitType.SOLDIER && robot.getTeam() == rc.getTeam()) {
                    soldierCount++;
                }
                if (rc.senseMapInfo(loc).getMark() == PaintType.ALLY_SECONDARY) {
                    curRuinType = UnitType.LEVEL_ONE_PAINT_TOWER;
                }
            }

            if (soldierCount >= 2 && !rc.getLocation().isWithinDistanceSquared(ruinLoc, 2)) {
                return false;
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
                    if (!mi.getPaint().equals(ideal) && rc.isActionReady()) {
                        rc.attack(loc, ideal.isSecondary());
                        break paintLoop;
                    }
                }
            }
        }
    }

    // true is secondary
    static boolean trySRP(MapLocation loc) {
        if (centerSRP(loc)) return false;
        return (loc.x + loc.y) % 2 == 0;
    }
    static boolean centerSRP(MapLocation loc) {
        /*if ( (loc.x + loc.y) % 2 == 0 ) {
            if (loc.y % 3 == 2) {
                return (loc.x - 2 - (loc.y - 2) / 3) % 4 == 0;
            }
        }*/
        return possibleSRPs.contains(loc);
        //return false;
    }
    static FastLocSet badSRPs = new FastLocSet();
    static boolean canSRP(RobotController rc, MapLocation srpLoc) throws GameActionException {
        if (badSRPs.contains(srpLoc)) return false;
        if (!centerSRP(srpLoc)) return false;
        if (!rc.getLocation().isWithinDistanceSquared(srpLoc, 2)) {
            int soldierCount = 0;
            // TODO: unroll this loop #2, uses so much bytecode rn
            MapLocation[] surroundingLocations = rc.getAllLocationsWithinRadiusSquared(srpLoc, 2);
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
        /*for (MapLocation ml : rc.senseNearbyRuins(-1)) {
            RobotInfo ri = rc.senseRobotAtLocation(ml);
            if (ri == null || !ri.getType().isTowerType()) {
                if (ml.distanceSquaredTo(srpLoc) <= 32) return false;
            }
        }*/
        for (int i = -2; i <= 2; i++) {
            for (int j = -2; j <= 2; j++) {
                MapLocation loc = FastMath.addVec(srpLoc, new MapLocation(i, j));
                if (!rc.canSenseLocation(loc)) continue;
                MapInfo mi = rc.senseMapInfo(loc);
                if (mi.hasRuin() || mi.isWall()) {
                    badSRPs.add(srpLoc);
                    return false;
                }
                if (mi.getPaint().isEnemy()) return false;
            }
        }
        return true;
    }

    static void paintRandomly(RobotController rc) throws  GameActionException {
        MapLocation myLoc = rc.getLocation();
        MapInfo[] nearby = rc.senseNearbyMapInfos();

        if(canPaintReal(rc, myLoc) && !rc.senseMapInfo(myLoc).getPaint().isAlly()) {
            rc.attack(rc.getLocation(), trySRP(myLoc));
            return;
        }

        for (MapInfo mi : nearby) {
            MapLocation loc = mi.getMapLocation();
            if (!canPaintReal(rc, loc)) continue;
            if (mi.getPaint() == PaintType.EMPTY) {
                rc.attack(loc, trySRP(loc));
                return;
            }
        }
        /*for(MapLocation loc : nearby) {
            boolean canOverwrite = true;
            if (!canPaintReal(rc, loc)) continue;
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

        }*/
    }

    static void endTurn(RobotController rc) throws GameActionException {
        if (curRuin != null) {
            rc.setIndicatorLine(rc.getLocation(), curRuin.getMapLocation(), 255, 0, 0);
        } else if (curSRP != null) {
            rc.setIndicatorLine(rc.getLocation(), curSRP, 255, 0, 255);

        }
        if (rc.getID() % 3 == 1) {
            rc.setIndicatorDot(rc.getLocation(), 0, 255, 0);
            }

        rc.setIndicatorString(indicator);
    }
    static boolean canPaintReal(RobotController rc, MapLocation loc) throws GameActionException { // canPaint that checks for cost
        int paintCap = rc.getPaint();
        return rc.isActionReady() && paintCap > rc.getType().attackCost && rc.canPaint(loc);
    }

    static void supplyPaint(RobotController rc) throws GameActionException {
        if (rc.getPaint() > 140) return;
        for (Direction d : Direction.allDirections()) {
            if (!rc.onTheMap(rc.getLocation().add(d))) continue;
            RobotInfo r = rc.senseRobotAtLocation(rc.getLocation().add(d));
            if (r != null && r.getType().isTowerType()) {
                if (Util.isPaintTower(r.getType())) {
                    if (r.getPaintAmount() > 350) {
                        if (rc.canTransferPaint(r.getLocation(), -(200 - rc.getPaint()))) {
                            rc.transferPaint(r.getLocation(), -(200 - rc.getPaint()));
                            return;
                        }
                    }
                }
                if (Util.isMoneyTower(r.getType())) {
                    if (r.getPaintAmount() > 350) {
                        if (rc.canTransferPaint(r.getLocation(), -(200 - rc.getPaint()))) {
                            rc.transferPaint(r.getLocation(), -(200 - rc.getPaint()));
                            return;
                        }
                    }
                }
            }
        }
    }
}
