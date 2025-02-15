package v7;

import battlecode.common.*;
import v6.fast.FastIntSet;
import v6.fast.FastLocSet;
import v6.fast.FastMath;

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
    static MapInfo curRuin = null;
    static MapInfo[] nearbyTiles;
    static RobotInfo[] nearbyAllies;
    static MapLocation curSRP = null;
    static MapLocation closestEnemyTower;

    static FastLocSet paintTowers = new FastLocSet();
    static SoldierMicro soldierMicro;
    static void init(RobotController rc) throws GameActionException {
        Refill.init(80);
        soldierMicro = new SoldierMicro(rc);
    }

    static void run(RobotController rc) throws GameActionException {
        nearbyTiles = rc.senseNearbyMapInfos();
        nearbyAllies = rc.senseNearbyRobots(-1, rc.getTeam());
        Communicator.update(rc);
        Communicator.relayEnemyTower(rc);
        blacklistCleanup(rc);

        if (closestEnemyTower != null && !Communicator.enemyTowers.contains(closestEnemyTower)) closestEnemyTower = null;
        if (closestEnemyTower == null) closestEnemyTower = Communicator.enemyTowers.closest(rc.getLocation());
        attackTower(rc);
        if (closestEnemyTower!=null && closestEnemyTower.isWithinDistanceSquared(rc.getLocation(), 16)) {
            if (canBeat(rc))
                soldierMicro.doMicro(closestEnemyTower);
            else {
                closestEnemyTower = null;
            }
        }
        attackTower(rc);
        if (closestEnemyTower != null) {
            rc.setIndicatorLine(rc.getLocation(), closestEnemyTower, 0, 0, 0);
        }
        if (curRuin == null && rc.getNumberTowers() < 25) {
            for (MapInfo tile : nearbyTiles) {
                if (tile.getMapLocation().isWithinDistanceSquared(rc.getLocation(), 2)) {
                    continue;
                }
                if (ruinCheck(rc, tile)) {
                    curRuin = tile;
                    if (rc.getRoundNum() < 20) {
                        curRuinType = UnitType.LEVEL_ONE_MONEY_TOWER;
                    }
                    else if (tile.getMark().isSecondary()) {
                        curRuinType = UnitType.LEVEL_ONE_PAINT_TOWER;
                    } else {
                        curRuinType = (FastMath.rand256() % 7 < 2) ?
                                UnitType.LEVEL_ONE_PAINT_TOWER :
                                UnitType.LEVEL_ONE_MONEY_TOWER;
                    }
                }
            }
        }
        if (rc.getNumberTowers() == 25) curRuin = null;

        if (curRuin == null) {
            boolean refilling = Refill.refill(rc);
            if (refilling){
                endTurn(rc);
                return;
            }
        }
        else {
            int initial = Refill.minPaint;
            Refill.minPaint = 10;
            boolean refilling = Refill.refill(rc);
            if (refilling) {
                endTurn(rc);
                return;
            }
            Refill.minPaint = initial;
        }
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
            boolean isSrpBuilder = Util.isSrpBuilder(rc, rc.getID());
            if (isSrpBuilder) {
                if (curSRP != null && !canSRP(rc, curSRP)) curSRP = null;
                if (curSRP == null)
                    acquireSRP(rc);
                if (curSRP != null) {
                    tryBuildSRP(rc);
                }
                Explorer.smartExplore(rc);
            }
            if(!isSrpBuilder || curSRP == null) {
                if (rc.getID() % 4 < 2) {
                    Explorer.smartExplore(rc);
                    if(rc.isActionReady())
                        paintRandomly(rc);
                }
                else if (rc.getRoundNum() > (rc.getMapHeight() * rc.getMapWidth()) * 7 / 64 + 6 && rc.getID() % 4 == 2) {
                    if (fill(rc)) {
                        if (closestEnemyTower != null) {
                            Pathfinding.moveToward(rc, closestEnemyTower);
                        } else {
                            Explorer.smartExplore(rc);
                        }
                        if(rc.isActionReady())
                            paintRandomly(rc);
                    }
                }
                else {
                    if (closestEnemyTower != null) {
                        Pathfinding.moveToward(rc, closestEnemyTower);
                    } else {
                        /*
                        if (rc.getRoundNum < 50) {
                            Direction d = Explorer.findDirection
                            Explorer.ExploreDirection(d);
                         */
                        Explorer.smartExplore(rc);
                    }
                    if(rc.isActionReady())
                        paintRandomly(rc);
                }
            }
        }

        supplyPaint(rc);
        if (Util.shouldKMS(rc)) rc.disintegrate();;
        endTurn(rc);
    }

    private static boolean canBeat(RobotController rc) throws GameActionException {
        if (!rc.canSenseLocation(closestEnemyTower) || !rc.canSenseRobotAtLocation(closestEnemyTower)) return false;

        int mult = 1;
        RobotInfo[] allies = rc.senseNearbyRobots(-1, rc.getTeam());
        for (var ri : allies) {
            if (ri.getType() == UnitType.SOLDIER) {
                if (ri.getLocation().isWithinDistanceSquared(closestEnemyTower, 29) && ri.getPaintAmount() > 100) {
                    mult++;
                }
            }
        }

        int hp = rc.getHealth();
        int paint = rc.getPaint();
        int enemyHp = rc.senseRobotAtLocation(closestEnemyTower).health;
        if (enemyHp / 50 <= (paint / 6) * mult) return true;
        return false;
    }

    public static MapLocation deadTower;
    static void attackTower(RobotController rc) throws GameActionException {
        if (closestEnemyTower == null) return;
        if (rc.canAttack(closestEnemyTower)) rc.attack(closestEnemyTower);
        if (rc.canSenseLocation(closestEnemyTower) && !rc.isLocationOccupied(closestEnemyTower)) {
            deadTower = closestEnemyTower;
            Communicator.addTarget(deadTower, rc);
            closestEnemyTower = null;
        }
    }


    private static boolean fill(RobotController rc) throws GameActionException {
        MapInfo[] nearby = rc.senseNearbyMapInfos(UnitType.SOLDIER.actionRadiusSquared);
        int cnt = nearby.length;
        for (MapInfo mi : nearby) {
            if (mi.isWall() || mi.hasRuin()) cnt--;
            if (mi.getPaint() != PaintType.EMPTY) cnt--;
        }

        if (rc.isActionReady()) {
            paintRandomly(rc);
        }
        return cnt == 0;
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
        if (rc.canCompleteResourcePattern(curSRP) && rc.getChips() > 1200) {
            rc.completeResourcePattern(curSRP);
            //badSRPs.add(curSRP);
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

    static FastIntSet popTime = new FastIntSet();
    static FastLocSet blacklist = new FastLocSet();
    static void blacklistCleanup(RobotController rc) {
        while (popTime.size > 0) {
            if (rc.getRoundNum() - popTime.peek() > 10) {
                popTime.pop();
                blacklist.pop();
            }
            else break;
        }
    }
    static boolean ruinCheck(RobotController rc, MapInfo ruin) throws GameActionException {
        if (blacklist.contains(ruin.getMapLocation())) return false;
        if (!ruin.hasRuin()) return false;
        boolean ret = ruinCheckOld(rc, ruin);
        if (!ret) {
            blacklist.add(ruin.getMapLocation());
            popTime.add(rc.getRoundNum());
        }
        return ret;
    }
    static boolean ruinCheckOld(RobotController rc, MapInfo ruin) throws GameActionException {
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
            }
            if (rc.senseMapInfo(loc).getMark() == PaintType.ALLY_SECONDARY) {
                curRuinType = UnitType.LEVEL_ONE_PAINT_TOWER;
            }

            if (soldierCount >= 2 && !rc.getLocation().isWithinDistanceSquared(ruinLoc, 2)) {
                return false;
            }
        }
        int en = 0;
        int empty = 0;
        for (int i = -2; i <= 2; i++) {
            for (int j = -2; j <= 2; j++) {
                if (i == 0 && j == 0) continue;
                MapLocation loc = FastMath.addVec(ruin.getMapLocation(), new MapLocation(i, j));
                if (rc.canSenseLocation(loc)) {
                    PaintType pt = rc.senseMapInfo(loc).getPaint();
                    if (pt.isEnemy()) {
                        en++;

                        boolean mopperNearby = false;

                        for (RobotInfo nearbyRobot : nearbyAllies) {
                            if (nearbyRobot.getLocation().isWithinDistanceSquared(loc, 9) &&
                                    nearbyRobot.getType() == UnitType.MOPPER &&
                                    nearbyRobot.getTeam() == rc.getTeam()) {
                                mopperNearby = true;
                                break;
                            }
                        }
                        if (!mopperNearby) {
                            return false;
                        }

                    }
                    else if (pt == PaintType.EMPTY) {
                        empty++;
                    }
                }
            }
        }
        if (en >= 10) return false;
        if (empty > (rc.getPaint() * 5) / 1.5) return false;
        return true;
    }

    static void tryBuild(RobotController rc) throws GameActionException {
        if (rc.getChips() > 1000 && rc.canCompleteTowerPattern(curRuinType, curRuin.getMapLocation())) {
            rc.completeTowerPattern(curRuinType, curRuin.getMapLocation());
            if(rc.canTransferPaint(curRuin.getMapLocation(), -Refill.getEmptyPaintAmount(rc))) {
                rc.transferPaint(curRuin.getMapLocation(), -Refill.getEmptyPaintAmount(rc));
            }
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
        int xp = loc.x % 4;
        int yp = loc.y % 4;
        return SRP.charAt(xp + yp*4) == 'X';
    }
    static String SRP = "XXOXXOOOOOXOXOOO";
    static boolean centerSRP(MapLocation loc) {
        int xp = loc.x % 4;
        int yp = loc.y % 4;
        return xp == 2 && yp == 2;
    }

    static FastLocSet badSRPs = new FastLocSet();
    static boolean canSRP(RobotController rc, MapLocation srpLoc) throws GameActionException {
        if (badSRPs.contains(srpLoc)) return false;
        if (!centerSRP(srpLoc)) return false;
        if (rc.canSenseLocation(srpLoc) && rc.senseMapInfo(srpLoc).isResourcePatternCenter()) return false;
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
                if (!rc.onTheMap(loc)) {
                    badSRPs.add(srpLoc);
                    return false;
                }
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

        rc.setIndicatorString(RobotPlayer.indicator);
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
