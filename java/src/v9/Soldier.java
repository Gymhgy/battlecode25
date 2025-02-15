package v9;

import battlecode.common.*;
import v9.fast.FastIntSet;
import v9.fast.FastLocSet;
import v9.fast.FastMath;

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
    static int[][] defenseTowerPattern = {
            {1, 1, 2, 1, 1},
            {1, 2, 2, 2, 1},
            {2, 2, 0, 2, 2},
            {1, 2, 2, 2, 1},
            {1, 1, 2, 1, 1},
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
    static MapLocation curRuin = null;
    static MapLocation[] nearbyRuins;
    static RobotInfo[] nearbyAllies;
    static MapLocation curSRP = null;
    static MapLocation closestEnemyTower;

    static FastLocSet ruins = new FastLocSet();
    static SoldierMicro soldierMicro;
    static void init(RobotController rc) throws GameActionException {
        Refill.init(80);
        soldierMicro = new SoldierMicro(rc);
    }
    static void updateTarget(RobotController rc) {
        MapLocation closest = Communicator.enemyTowers.closest(rc.getLocation());
        if (closest != null && closest.isWithinDistanceSquared(rc.getLocation(), 16)) {
            closestEnemyTower = closest;
        }
    }

    static FastLocSet checked = new FastLocSet();
    static void checkNearbySRPs(RobotController rc) throws GameActionException{
        //RobotPlayer.indicator += "check: " + Clock.getBytecodeNum() + " | ";
        for(int i = nearbys.length; i-->0;) {
            MapInfo mi = nearbys[i];
            MapLocation srp = mi.getMapLocation();
            if (checked.contains(srp)) continue;
            if(mi.getMark().isSecondary() || mi.isResourcePatternCenter()) {
                checked.add(srp);
                badSRPs.add(srp.translate(-3, -3));
                badSRPs.add(srp.translate(-3, -2));
                badSRPs.add(srp.translate(-3, -1));
                badSRPs.add(srp.translate(-3, 0));
                badSRPs.add(srp.translate(-3, 1));
                badSRPs.add(srp.translate(-3, 2));
                badSRPs.add(srp.translate(-3, 3));
                badSRPs.add(srp.translate(-2, -3));
                badSRPs.add(srp.translate(-2, -2));
                badSRPs.add(srp.translate(-2, -1));
                badSRPs.add(srp.translate(-2, 0));
                badSRPs.add(srp.translate(-2, 1));
                badSRPs.add(srp.translate(-2, 2));
                badSRPs.add(srp.translate(-2, 3));
                badSRPs.add(srp.translate(-1, -3));
                badSRPs.add(srp.translate(-1, -2));
                badSRPs.add(srp.translate(-1, -1));
                badSRPs.add(srp.translate(-1, 0));
                badSRPs.add(srp.translate(-1, 1));
                badSRPs.add(srp.translate(-1, 2));
                badSRPs.add(srp.translate(-1, 3));
                badSRPs.add(srp.translate(0, -3));
                badSRPs.add(srp.translate(0, -2));
                badSRPs.add(srp.translate(0, -1));
                badSRPs.add(srp.translate(0, 1));
                badSRPs.add(srp.translate(0, 2));
                badSRPs.add(srp.translate(0, 3));
                badSRPs.add(srp.translate(1, -3));
                badSRPs.add(srp.translate(1, -2));
                badSRPs.add(srp.translate(1, -1));
                badSRPs.add(srp.translate(1, 0));
                badSRPs.add(srp.translate(1, 1));
                badSRPs.add(srp.translate(1, 2));
                badSRPs.add(srp.translate(1, 3));
                badSRPs.add(srp.translate(2, -3));
                badSRPs.add(srp.translate(2, -2));
                badSRPs.add(srp.translate(2, -1));
                badSRPs.add(srp.translate(2, 0));
                badSRPs.add(srp.translate(2, 1));
                badSRPs.add(srp.translate(2, 2));
                badSRPs.add(srp.translate(2, 3));
                badSRPs.add(srp.translate(3, -3));
                badSRPs.add(srp.translate(3, -2));
                badSRPs.add(srp.translate(3, -1));
                badSRPs.add(srp.translate(3, 0));
                badSRPs.add(srp.translate(3, 1));
                badSRPs.add(srp.translate(3, 2));
                badSRPs.add(srp.translate(3, 3));
                badSRPs.add(srp.translate(4, 1));
                badSRPs.add(srp.translate(4, -1));
                badSRPs.add(srp.translate(-4, 1));
                badSRPs.add(srp.translate(-4, -1));
                badSRPs.add(srp.translate(1, 4));
                badSRPs.add(srp.translate(1, -4));
                badSRPs.add(srp.translate(-1, 4));
                badSRPs.add(srp.translate(-1, -4));
                badSRPs.add(srp.translate(4, 2));
                badSRPs.add(srp.translate(4, -2));
                badSRPs.add(srp.translate(-4, 2));
                badSRPs.add(srp.translate(-4, -2));
                badSRPs.add(srp.translate(2, 4));
                badSRPs.add(srp.translate(2, -4));
                badSRPs.add(srp.translate(-2, 4));
                badSRPs.add(srp.translate(-2, -4));
            }
       }
        //RobotPlayer.indicator += Clock.getBytecodeNum() + "\n";
    }
    static MapInfo[] nearbys;
    static void run(RobotController rc) throws GameActionException {
        nearbyAllies = rc.senseNearbyRobots(-1, rc.getTeam());
        nearbys = rc.senseNearbyMapInfos();
        Communicator.update(rc);
        Communicator.relayEnemyTower(rc);
        blacklistCleanup(rc);
        nearbyRuins = Communicator.ruins;

        if (closestEnemyTower != null && !Communicator.enemyTowers.contains(closestEnemyTower)) closestEnemyTower = null;
        if (closestEnemyTower == null) closestEnemyTower = Communicator.enemyTowers.closest(rc.getLocation());
        //updateTarget(rc);
        attackTower(rc);
        if (closestEnemyTower!=null && closestEnemyTower.isWithinDistanceSquared(rc.getLocation(), 20)) {
            if (canBeat(rc))
                soldierMicro.doMicro(closestEnemyTower);
            else {
                closestEnemyTower = null;
            }
        }
        attackTower(rc);
        if (closestEnemyTower != null) {
            //rc.setIndicatorLine(rc.getLocation(), closestEnemyTower, 0, 0, 0);
        }


        boolean notWorking = curRuin == null || FastMath.chebyshev(curRuin, rc.getLocation()) > 2;
        if (notWorking && rc.getNumberTowers() < 25) {
            for (MapLocation tile : nearbyRuins) {
                if (tile.isWithinDistanceSquared(rc.getLocation(), 2)) {
                    continue;
                }
                ruins.add(tile);

                if (ruinCheck(rc, tile)) {
                    curRuin = tile;
                    if (curRuinType == null) {
                        if (rc.getRoundNum() < 20) {
                            curRuinType = UnitType.LEVEL_ONE_MONEY_TOWER;
                        } else {
                            curRuinType = (FastMath.rand256() % 7 < 2) ?
                                    UnitType.LEVEL_ONE_PAINT_TOWER :
                                    UnitType.LEVEL_ONE_MONEY_TOWER;
                        }
                    }
                }
            }
        }

        //RobotPlayer.indicator += "annoy: " + Clock.getBytecodeNum() + " | ";
        if (rc.isActionReady()) {
            for (MapLocation tile : nearbyRuins) {
                 if (annoy(rc, tile)) break;
            }
        }
        //RobotPlayer.indicator += Clock.getBytecodeNum() + "\n";


        if (rc.getNumberTowers() == 25) curRuin = null;

        //RobotPlayer.indicator += "refill: " + Clock.getBytecodeNum() + " | ";
        if (curRuin == null && curSRP == null) {
            boolean refilling = Refill.refill(rc);
            if (refilling){
                endTurn(rc);
                return;
            }
            else {
                //curRuin = ruins.closest(rc.getLocation());
                //if (curRuin != null && blacklist.contains(curRuin)) curRuin = null;
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
        //RobotPlayer.indicator += Clock.getBytecodeNum() + "\n";

        if (curRuin != null) {
            MapLocation toMark = null;
            boolean alrMarked = false;

            for (MapLocation loc : rc.getAllLocationsWithinRadiusSquared(curRuin, 2)) {
                if (rc.canSenseLocation(loc) && rc.senseMapInfo(loc).getMark().isAlly()) {
                    alrMarked = true;
                    break;
                }
                if (rc.canMark(loc)) {
                    if (curRuinType == UnitType.LEVEL_ONE_PAINT_TOWER && (loc.x+loc.y)%2 == 1)
                        toMark = loc;
                    else if (curRuinType == UnitType.LEVEL_ONE_MONEY_TOWER && (loc.x+loc.y)%2 == 0)
                        toMark = loc;
                }
            }

            if (!alrMarked && toMark != null) {
                rc.mark(toMark, false);
            }

            if (ruinCheck(rc, curRuin)) {
                tryBuild(rc);
            } else {
                curRuin = null;
            }
        }

        if (curRuin == null) {
            boolean isSrpBuilder = Util.isSrpBuilder(rc, rc.getID());
            isSrpBuilder &= closestEnemyTower == null ||
                    !closestEnemyTower.isWithinDistanceSquared(rc.getLocation(), 30);
            if (isSrpBuilder) {
                if (curSRP != null && !canSRP(rc, curSRP)) curSRP = null;
                if (curSRP == null) {
                    checkNearbySRPs(rc);
                    //RobotPlayer.indicator += "wtf: " + Clock.getBytecodeNum() + " | ";
                    acquireSRP(rc);
                    //RobotPlayer.indicator +=  Clock.getBytecodeNum() + "\n";

                }
                if (curSRP != null) {
                    tryBuildSRP(rc);
                }
                Explorer.smartExplore(rc);
            }
            if(!isSrpBuilder || curSRP == null) {
                /*if (rc.getID() % 4 < 2) {
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
                        Explorer.smartExplore(rc);
                    }
                    if(rc.isActionReady())
                        paintRandomly(rc);
                }*/
                if (rc.getID() % 5 == 1) {
                    Explorer.smartExplore(rc);
                    if(rc.isActionReady())
                        paintRandomly(rc);

                }
                else {
                    if (closestEnemyTower != null) {
                        Pathfinding.moveToward(rc, closestEnemyTower);
                    } else {
                        Explorer.smartExplore(rc);
                    }
                    if(rc.isActionReady())
                        paintRandomly(rc);
                }
            }
        }

        //RobotPlayer.indicator += "supply: " + Clock.getBytecodeNum() + " | ";
        supplyPaint(rc);
        //RobotPlayer.indicator += Clock.getBytecodeNum() + "\n";
        if (Util.shouldKMS(rc)) rc.disintegrate();;
        endTurn(rc);
    }

    private static boolean canBeat(RobotController rc) throws GameActionException {
        if (!rc.canSenseLocation(closestEnemyTower) || !rc.canSenseRobotAtLocation(closestEnemyTower)) return false;

        int mult = 1;
        int paintAmt = rc.getPaint();
        for (var ri : nearbyAllies) {
            if (ri.getType() == UnitType.SOLDIER) {
                if (ri.getLocation().isWithinDistanceSquared(closestEnemyTower, 29) && ri.getPaintAmount() > 50) {
                    mult++;
                    paintAmt += ri.getPaintAmount();
                }
            }
        }

        int hp = rc.getHealth();
        int paint = rc.getPaint();
        int enemyHp = rc.senseRobotAtLocation(closestEnemyTower).health;
        /*if (hp <= 40) {
            if (enemyHp / 50 <= mult) return true;
            return false;
        }*/
        if (enemyHp / 50 <= paintAmt / 5.5) return true;
        return false;
    }

    static void attackTower(RobotController rc) throws GameActionException {
        if (closestEnemyTower == null) return;
        if (rc.canAttack(closestEnemyTower)) rc.attack(closestEnemyTower);
        if (rc.canSenseLocation(closestEnemyTower) && !rc.isLocationOccupied(closestEnemyTower)) {
            closestEnemyTower = null;
        }
    }



    private static boolean fill(RobotController rc) throws GameActionException {
        /*MapInfo[] nearby = rc.senseNearbyMapInfos(UnitType.SOLDIER.actionRadiusSquared);
        int cnt = nearby.length;
        for (MapInfo mi : nearby) {
            if (mi.isWall() || mi.hasRuin()) cnt--;
            if (mi.getPaint() != PaintType.EMPTY) cnt--;
        }

        if (rc.isActionReady()) {
            paintRandomly(rc);
        }
        return cnt == 0;*/
        return true;
    }

    private static void acquireSRP(RobotController rc) throws GameActionException {
        for (MapInfo mi : nearbys) {
            if (Clock.getBytecodesLeft() < 2500) break;
            if (centerSRP(mi.getMapLocation()) && canSRP(rc, mi.getMapLocation())) {
                curSRP = mi.getMapLocation();
                return;
            }
        }
        for (MapInfo mi : nearbys) {
            if (Clock.getBytecodesLeft() < 2500) break;
            if (canSRP(rc, mi.getMapLocation())) {
                curSRP = mi.getMapLocation();
                break;
            }
        }
    }

    private static void tryBuildSRP(RobotController rc) throws GameActionException {
        RobotInfo[] enemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
        boolean safe = true;
        for (int i = 0; i < enemies.length; i++) {
            if (enemies[i].getType() == UnitType.SPLASHER || enemies[i].getType() == UnitType.MOPPER) {
                safe = false;
                break;
            }
        }
        if (!safe) {
            blacklist.add(curSRP);
            popTime.add(rc.getRoundNum());
            curSRP = null;
            return;
        }
        if (safe) {
            if (rc.canCompleteResourcePattern(curSRP)) {
                rc.completeResourcePattern(curSRP);
                //badSRPs.add(curSRP);
                curSRP = null;
                return;
            }
        }

        if(!rc.getLocation().equals(curSRP)) {
            Pathfinding.moveToward(rc, curSRP);
            nearbys = rc.senseNearbyMapInfos();
            checkNearbySRPs(rc);
            if (!canSRP(rc, curSRP)) {
                curSRP = null;
                return;
            }
        }
        if (rc.getLocation().equals(curSRP) && rc.canMark(curSRP) && !rc.senseMapInfo(curSRP).getMark().isSecondary()) {
            rc.mark(curSRP, true);
        }
        MapLocation myLoc = rc.getLocation();
        boolean adj = rc.getLocation().isWithinDistanceSquared(curSRP, 2);
        if (canPaintReal(rc, myLoc) && myLoc.isWithinDistanceSquared(curSRP, 8)){
            boolean ideal = trySRP(curSRP.x - myLoc.x, curSRP.y - myLoc.y);
            PaintType paintType = rc.senseMapInfo(myLoc).getPaint();
            if (paintType.isEnemy()) {

            }
            else if (paintType == PaintType.EMPTY) {
                rc.attack(myLoc, ideal);
                return;
            }
            else if (adj && paintType.isSecondary() != ideal) {
                rc.attack(myLoc, ideal);
                return;
            }
        }
        // else find 5x5
        paintLoop:
        for (int i = -2; i <= 2; i++) {
            for (int j = -2; j <= 2; j++) {
                MapLocation loc = curSRP.translate(i, j);
                if (canPaintReal(rc, loc)) {
                    boolean ideal = trySRP(i, j);
                    PaintType paintType = rc.senseMapInfo(loc).getPaint();
                    if (paintType.isEnemy()) {

                    }
                    else if (paintType == PaintType.EMPTY) {
                        rc.attack(loc, ideal);
                        return;
                    }
                    else if (adj && paintType.isSecondary() != ideal) {
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
    static boolean ruinCheck(RobotController rc, MapLocation ruin) throws GameActionException {
        //RobotPlayer.indicator += "r-check: " + Clock.getBytecodeNum() + " | ";
        boolean ret = ruinCheckOld(rc, ruin);
        if (blacklist.contains(ruin)) {
            //RobotPlayer.indicator += Clock.getBytecodeNum() + "!\n";
            return false;
        }
        if (!ret) {
            blacklist.add(ruin);
            popTime.add(rc.getRoundNum());
        }
        //RobotPlayer.indicator += Clock.getBytecodeNum() + "\n";
        return ret;
    }
    static boolean ruinCheckOld(RobotController rc, MapLocation ruinLoc) throws GameActionException {
        //RobotPlayer.indicator += "\nr-check: " + ruinLoc + " ";
        if (!rc.canSenseLocation(ruinLoc)) {
            // uh we can't see the damn ruin... return true to be safe
            return true;
        }
        RobotInfo ri = rc.senseRobotAtLocation(ruinLoc);
        if (ri != null && ri.getType().isTowerType()) {
            ruins.remove(ruinLoc);
            if(rc.getPaint() < 150 && ri.getLocation().isAdjacentTo(rc.getLocation())) {
                Refill.refilling = true;
            }
            return false;
        }
        int soldierCount = 0;

        // TODO: unroll this loop, uses so much bytecode rn
        MapLocation[] surroundingLocations = rc.getAllLocationsWithinRadiusSquared(ruinLoc, 2);
        boolean seePaint = false, seeMoney = false;
        for (MapLocation loc : surroundingLocations) {
            if (rc.canSenseLocation(loc) && rc.isLocationOccupied(loc)) {
                RobotInfo robot = rc.senseRobotAtLocation(loc);
                if (robot.getType() == UnitType.SOLDIER && robot.getTeam() == rc.getTeam()) {
                    soldierCount++;
                }
            }
            if (rc.senseMapInfo(loc).getMark().isAlly() && (loc.x+loc.y)%2 == 1) {
                curRuinType = UnitType.LEVEL_ONE_PAINT_TOWER;
                seePaint = true;
            }
            else if (rc.senseMapInfo(loc).getMark().isAlly() && (loc.x+loc.y)%2 == 0) {
                curRuinType = UnitType.LEVEL_ONE_MONEY_TOWER;
                seeMoney = true;
            }

            if (soldierCount >= 3 && !rc.getLocation().isWithinDistanceSquared(ruinLoc, 2)) {
                return false;
            }
        }
        if (seeMoney && seePaint) curRuinType = UnitType.LEVEL_ONE_DEFENSE_TOWER;
        int en = 0;
        int empty = 0;
        for (int i = -2; i <= 2; i++) {
            for (int j = -2; j <= 2; j++) {
                if (i == 0 && j == 0) continue;
                MapLocation loc = FastMath.addVec(ruinLoc, new MapLocation(i, j));
                if (rc.canSenseLocation(loc)) {
                    RobotInfo r = rc.senseRobotAtLocation(loc);
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
        if (empty / Math.max(1, soldierCount) > (rc.getPaint() / 5) ) {
            Refill.refilling = true;
            return true;
        }
        return true;
    }

    static boolean annoy(RobotController rc, MapLocation ruinLoc) throws GameActionException {
        if (!rc.canSenseLocation(ruinLoc)) return false;
        RobotInfo r = rc.senseRobotAtLocation(ruinLoc);
        if (r != null && r.getType().isTowerType()) return false;
        int en = 0;
        int ally = 0;
        boolean enemySoldier = false;
        MapLocation annoy = null;
        for (int i = -2; i <= 2; i++) {
            for (int j = -2; j <= 2; j++) {
                if (i == 0 && j == 0) continue;
                MapLocation loc = FastMath.addVec(ruinLoc, new MapLocation(i, j));
                if (rc.canSenseLocation(loc)) {
                    r = rc.senseRobotAtLocation(loc);
                    if(!enemySoldier && r!=null && r.getType()==UnitType.SOLDIER && r.getTeam() == rc.getTeam().opponent()) enemySoldier = true;
                    PaintType pt = rc.senseMapInfo(loc).getPaint();
                    if (pt.isAlly()) ally++;
                    if (pt.isEnemy()) {
                        en++;
                    }
                    else if (pt == PaintType.EMPTY) {
                        if (canPaintReal(rc, loc))
                            if (annoy == null || FastMath.rand256() % 4 == 0)
                                annoy = loc;
                        if (enemySoldier || en >= 5) break;
                    }
                }
            }
        }
        if (enemySoldier || en >= 5) {
            if (ally == 0 || FastMath.rand256() % 3 == 0)
                if (annoy != null) {
                    rc.attack(annoy);
                    return true;
                }
        }
        return false;
    }

    static void tryBuild(RobotController rc) throws GameActionException {
        if (rc.getChips() > 1000 && rc.canCompleteTowerPattern(curRuinType, curRuin)) {
            rc.completeTowerPattern(curRuinType, curRuin);
            ruins.remove(curRuin);

            /*if (curRuinType != UnitType.LEVEL_ONE_DEFENSE_TOWER) {
                MapLocation markLoc = null;
                boolean seeOne = false;
                boolean seeTwo = false;
                for (int i = RobotPlayer.directions.length; i-- > 0; ) {
                    MapLocation loc = curRuin.getMapLocation().add(RobotPlayer.directions[i]);
                    if (rc.canSenseLocation(loc)) {
                        PaintType m = rc.senseMapInfo(loc).getMark();
                        if (m == PaintType.ALLY_PRIMARY) seeOne = true;
                        if (m == PaintType.ENEMY_SECONDARY) seeTwo = true;
                        if (rc.canMark(loc) && m == PaintType.EMPTY) {
                            markLoc = loc;
                        }
                    }
                }
                if (!seeOne || !seeTwo) if(markLoc != null) rc.mark(markLoc, curRuinType == UnitType.LEVEL_ONE_MONEY_TOWER);
            }*/

            if(rc.canTransferPaint(curRuin, -Refill.getEmptyPaintAmount(rc))) {
                rc.transferPaint(curRuin, -Refill.getEmptyPaintAmount(rc));
            } else if(rc.getPaint() < 150) {
                Refill.refilling = true;
            }
            curRuinType = switch(curRuinType) {
                case LEVEL_ONE_PAINT_TOWER -> UnitType.LEVEL_ONE_MONEY_TOWER;
                case LEVEL_ONE_MONEY_TOWER -> UnitType.LEVEL_ONE_PAINT_TOWER;
                default -> FastMath.rand256() % 2 == 0 ? UnitType.LEVEL_ONE_PAINT_TOWER : UnitType.LEVEL_ONE_MONEY_TOWER;
            };
            curRuin = null;
            return;
        }
        Pathfinding.moveToward(rc, curRuin);

        if (!rc.isActionReady()) return;
        int[][] tower = switch(curRuinType) {
            case LEVEL_ONE_PAINT_TOWER -> paintTowerPattern;
            case LEVEL_ONE_MONEY_TOWER -> moneyTowerPattern;
            default -> defenseTowerPattern;
        };
        // Paint under self first... then 5x5
        // Paint under self
        MapLocation myLoc = rc.getLocation();
        if (canPaintReal(rc, myLoc) && myLoc.isWithinDistanceSquared(curRuin, 8)){
            MapLocation delta = FastMath.minusVec(curRuin, myLoc);
            PaintType ideal = numToPaint(tower[delta.x + 2][delta.y + 2]);
            if (!rc.senseMapInfo(myLoc).getPaint().equals(ideal)) {
                //System.out.println(rc.getLocation().toString() + " Painting at my position: " + myLoc);
                rc.attack(myLoc, ideal.isSecondary());
            }
        }
        // else find 5x5

        paintLoop:
        {
            MapLocation loc;
            loc = curRuin.translate(-2, -2);
            if (canPaintReal(rc, loc)) {
                MapInfo mi = rc.senseMapInfo(loc);
                PaintType ideal = numToPaint(tower[0][0]);
                if (!mi.getPaint().equals(ideal) && rc.isActionReady()) {
                    rc.attack(loc, ideal.isSecondary());
                    break paintLoop;
                }
            }
            loc = curRuin.translate(-2, 2);
            if (canPaintReal(rc, loc)) {
                MapInfo mi = rc.senseMapInfo(loc);
                PaintType ideal = numToPaint(tower[0][4]);
                if (!mi.getPaint().equals(ideal) && rc.isActionReady()) {
                    rc.attack(loc, ideal.isSecondary());
                    break paintLoop;
                }
            }
            loc = curRuin.translate(2, -2);
            if (canPaintReal(rc, loc)) {
                MapInfo mi = rc.senseMapInfo(loc);
                PaintType ideal = numToPaint(tower[4][0]);
                if (!mi.getPaint().equals(ideal) && rc.isActionReady()) {
                    rc.attack(loc, ideal.isSecondary());
                    break paintLoop;
                }
            }
            loc = curRuin.translate(2, 2);
            if (canPaintReal(rc, loc)) {
                MapInfo mi = rc.senseMapInfo(loc);
                PaintType ideal = numToPaint(tower[4][4]);
                if (!mi.getPaint().equals(ideal) && rc.isActionReady()) {
                    rc.attack(loc, ideal.isSecondary());
                    break paintLoop;
                }
            }
            loc = curRuin.translate(-2, -1);
            if (canPaintReal(rc, loc)) {
                MapInfo mi = rc.senseMapInfo(loc);
                PaintType ideal = numToPaint(tower[0][1]);
                if (!mi.getPaint().equals(ideal) && rc.isActionReady()) {
                    rc.attack(loc, ideal.isSecondary());
                    break paintLoop;
                }
            }
            loc = curRuin.translate(-2, 1);
            if (canPaintReal(rc, loc)) {
                MapInfo mi = rc.senseMapInfo(loc);
                PaintType ideal = numToPaint(tower[0][3]);
                if (!mi.getPaint().equals(ideal) && rc.isActionReady()) {
                    rc.attack(loc, ideal.isSecondary());
                    break paintLoop;
                }
            }
            loc = curRuin.translate(-1, -2);
            if (canPaintReal(rc, loc)) {
                MapInfo mi = rc.senseMapInfo(loc);
                PaintType ideal = numToPaint(tower[1][0]);
                if (!mi.getPaint().equals(ideal) && rc.isActionReady()) {
                    rc.attack(loc, ideal.isSecondary());
                    break paintLoop;
                }
            }
            loc = curRuin.translate(-1, 2);
            if (canPaintReal(rc, loc)) {
                MapInfo mi = rc.senseMapInfo(loc);
                PaintType ideal = numToPaint(tower[1][4]);
                if (!mi.getPaint().equals(ideal) && rc.isActionReady()) {
                    rc.attack(loc, ideal.isSecondary());
                    break paintLoop;
                }
            }
            loc = curRuin.translate(1, -2);
            if (canPaintReal(rc, loc)) {
                MapInfo mi = rc.senseMapInfo(loc);
                PaintType ideal = numToPaint(tower[3][0]);
                if (!mi.getPaint().equals(ideal) && rc.isActionReady()) {
                    rc.attack(loc, ideal.isSecondary());
                    break paintLoop;
                }
            }
            loc = curRuin.translate(1, 2);
            if (canPaintReal(rc, loc)) {
                MapInfo mi = rc.senseMapInfo(loc);
                PaintType ideal = numToPaint(tower[3][4]);
                if (!mi.getPaint().equals(ideal) && rc.isActionReady()) {
                    rc.attack(loc, ideal.isSecondary());
                    break paintLoop;
                }
            }
            loc = curRuin.translate(2, -1);
            if (canPaintReal(rc, loc)) {
                MapInfo mi = rc.senseMapInfo(loc);
                PaintType ideal = numToPaint(tower[4][1]);
                if (!mi.getPaint().equals(ideal) && rc.isActionReady()) {
                    rc.attack(loc, ideal.isSecondary());
                    break paintLoop;
                }
            }
            loc = curRuin.translate(2, 1);
            if (canPaintReal(rc, loc)) {
                MapInfo mi = rc.senseMapInfo(loc);
                PaintType ideal = numToPaint(tower[4][3]);
                if (!mi.getPaint().equals(ideal) && rc.isActionReady()) {
                    rc.attack(loc, ideal.isSecondary());
                    break paintLoop;
                }
            }
            loc = curRuin.translate(-2, 0);
            if (canPaintReal(rc, loc)) {
                MapInfo mi = rc.senseMapInfo(loc);
                PaintType ideal = numToPaint(tower[0][2]);
                if (!mi.getPaint().equals(ideal) && rc.isActionReady()) {
                    rc.attack(loc, ideal.isSecondary());
                    break paintLoop;
                }
            }
            loc = curRuin.translate(-1, -1);
            if (canPaintReal(rc, loc)) {
                MapInfo mi = rc.senseMapInfo(loc);
                PaintType ideal = numToPaint(tower[1][1]);
                if (!mi.getPaint().equals(ideal) && rc.isActionReady()) {
                    rc.attack(loc, ideal.isSecondary());
                    break paintLoop;
                }
            }
            loc = curRuin.translate(-1, 1);
            if (canPaintReal(rc, loc)) {
                MapInfo mi = rc.senseMapInfo(loc);
                PaintType ideal = numToPaint(tower[1][3]);
                if (!mi.getPaint().equals(ideal) && rc.isActionReady()) {
                    rc.attack(loc, ideal.isSecondary());
                    break paintLoop;
                }
            }
            loc = curRuin.translate(0, -2);
            if (canPaintReal(rc, loc)) {
                MapInfo mi = rc.senseMapInfo(loc);
                PaintType ideal = numToPaint(tower[2][0]);
                if (!mi.getPaint().equals(ideal) && rc.isActionReady()) {
                    rc.attack(loc, ideal.isSecondary());
                    break paintLoop;
                }
            }
            loc = curRuin.translate(0, 2);
            if (canPaintReal(rc, loc)) {
                MapInfo mi = rc.senseMapInfo(loc);
                PaintType ideal = numToPaint(tower[2][4]);
                if (!mi.getPaint().equals(ideal) && rc.isActionReady()) {
                    rc.attack(loc, ideal.isSecondary());
                    break paintLoop;
                }
            }
            loc = curRuin.translate(1, -1);
            if (canPaintReal(rc, loc)) {
                MapInfo mi = rc.senseMapInfo(loc);
                PaintType ideal = numToPaint(tower[3][1]);
                if (!mi.getPaint().equals(ideal) && rc.isActionReady()) {
                    rc.attack(loc, ideal.isSecondary());
                    break paintLoop;
                }
            }
            loc = curRuin.translate(1, 1);
            if (canPaintReal(rc, loc)) {
                MapInfo mi = rc.senseMapInfo(loc);
                PaintType ideal = numToPaint(tower[3][3]);
                if (!mi.getPaint().equals(ideal) && rc.isActionReady()) {
                    rc.attack(loc, ideal.isSecondary());
                    break paintLoop;
                }
            }
            loc = curRuin.translate(2, 0);
            if (canPaintReal(rc, loc)) {
                MapInfo mi = rc.senseMapInfo(loc);
                PaintType ideal = numToPaint(tower[4][2]);
                if (!mi.getPaint().equals(ideal) && rc.isActionReady()) {
                    rc.attack(loc, ideal.isSecondary());
                    break paintLoop;
                }
            }
            loc = curRuin.translate(-1, 0);
            if (canPaintReal(rc, loc)) {
                MapInfo mi = rc.senseMapInfo(loc);
                PaintType ideal = numToPaint(tower[1][2]);
                if (!mi.getPaint().equals(ideal) && rc.isActionReady()) {
                    rc.attack(loc, ideal.isSecondary());
                    break paintLoop;
                }
            }
            loc = curRuin.translate(0, -1);
            if (canPaintReal(rc, loc)) {
                MapInfo mi = rc.senseMapInfo(loc);
                PaintType ideal = numToPaint(tower[2][1]);
                if (!mi.getPaint().equals(ideal) && rc.isActionReady()) {
                    rc.attack(loc, ideal.isSecondary());
                    break paintLoop;
                }
            }
            loc = curRuin.translate(0, 1);
            if (canPaintReal(rc, loc)) {
                MapInfo mi = rc.senseMapInfo(loc);
                PaintType ideal = numToPaint(tower[2][3]);
                if (!mi.getPaint().equals(ideal) && rc.isActionReady()) {
                    rc.attack(loc, ideal.isSecondary());
                    break paintLoop;
                }
            }
            loc = curRuin.translate(1, 0);
            if (canPaintReal(rc, loc)) {
                MapInfo mi = rc.senseMapInfo(loc);
                PaintType ideal = numToPaint(tower[3][2]);
                if (!mi.getPaint().equals(ideal) && rc.isActionReady()) {
                    rc.attack(loc, ideal.isSecondary());
                    break paintLoop;
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
    static boolean trySRP(int i, int j) {
        int xp = (i + 2) % 4;
        int yp = (j + 2) % 4;
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
        if (blacklist.contains(srpLoc)) return false;
        if (rc.canSenseLocation(srpLoc) && rc.senseMapInfo(srpLoc).isResourcePatternCenter()) return false;
        if (!rc.getLocation().isWithinDistanceSquared(srpLoc, 2)) {
            int soldierCount = 0;
            MapLocation loc = srpLoc.translate(0, 0);
            if (rc.canSenseLocation(loc) && rc.isLocationOccupied(loc)) {
                RobotInfo robot = rc.senseRobotAtLocation(loc);
                if (robot.getType() == UnitType.SOLDIER && robot.getTeam() == rc.getTeam()) {
                    if (Util.isSrpBuilder(rc, robot.getID()))
                        soldierCount++;
                }
            }

            loc = srpLoc.translate(1, 0);
            if (rc.canSenseLocation(loc) && rc.isLocationOccupied(loc)) {
                RobotInfo robot = rc.senseRobotAtLocation(loc);
                if (robot.getType() == UnitType.SOLDIER && robot.getTeam() == rc.getTeam()) {
                    if (Util.isSrpBuilder(rc, robot.getID()))
                        soldierCount++;
                }
            }

            loc = srpLoc.translate(0, 1);
            if (rc.canSenseLocation(loc) && rc.isLocationOccupied(loc)) {
                RobotInfo robot = rc.senseRobotAtLocation(loc);
                if (robot.getType() == UnitType.SOLDIER && robot.getTeam() == rc.getTeam()) {
                    if (Util.isSrpBuilder(rc, robot.getID()))
                        soldierCount++;
                }
            }
            if (soldierCount >= 3) return false;

            loc = srpLoc.translate(1, 1);
            if (rc.canSenseLocation(loc) && rc.isLocationOccupied(loc)) {
                RobotInfo robot = rc.senseRobotAtLocation(loc);
                if (robot.getType() == UnitType.SOLDIER && robot.getTeam() == rc.getTeam()) {
                    if (Util.isSrpBuilder(rc, robot.getID()))
                        soldierCount++;
                }
            }
            if (soldierCount >= 3) return false;

            loc = srpLoc.translate(-1, -1);
            if (rc.canSenseLocation(loc) && rc.isLocationOccupied(loc)) {
                RobotInfo robot = rc.senseRobotAtLocation(loc);
                if (robot.getType() == UnitType.SOLDIER && robot.getTeam() == rc.getTeam()) {
                    if (Util.isSrpBuilder(rc, robot.getID()))
                        soldierCount++;
                }
            }
            if (soldierCount >= 3) return false;

            loc = srpLoc.translate(-1, 0);
            if (rc.canSenseLocation(loc) && rc.isLocationOccupied(loc)) {
                RobotInfo robot = rc.senseRobotAtLocation(loc);
                if (robot.getType() == UnitType.SOLDIER && robot.getTeam() == rc.getTeam()) {
                    if (Util.isSrpBuilder(rc, robot.getID()))
                        soldierCount++;
                }
            }
            if (soldierCount >= 3) return false;

            loc = srpLoc.translate(0, -1);
            if (rc.canSenseLocation(loc) && rc.isLocationOccupied(loc)) {
                RobotInfo robot = rc.senseRobotAtLocation(loc);
                if (robot.getType() == UnitType.SOLDIER && robot.getTeam() == rc.getTeam()) {
                    if (Util.isSrpBuilder(rc, robot.getID()))
                        soldierCount++;
                }
            }
            if (soldierCount >= 3) return false;

            loc = srpLoc.translate(-1, 1);
            if (rc.canSenseLocation(loc) && rc.isLocationOccupied(loc)) {
                RobotInfo robot = rc.senseRobotAtLocation(loc);
                if (robot.getType() == UnitType.SOLDIER && robot.getTeam() == rc.getTeam()) {
                    if (Util.isSrpBuilder(rc, robot.getID()))
                        soldierCount++;
                }
            }
            if (soldierCount >= 3) return false;

            loc = srpLoc.translate(1, -1);
            if (rc.canSenseLocation(loc) && rc.isLocationOccupied(loc)) {
                RobotInfo robot = rc.senseRobotAtLocation(loc);
                if (robot.getType() == UnitType.SOLDIER && robot.getTeam() == rc.getTeam()) {
                    if (Util.isSrpBuilder(rc, robot.getID()))
                        soldierCount++;
                }
            }
            if (soldierCount >= 3) return false;
        }
        /*for (MapLocation ml : rc.senseNearbyRuins(-1)) {
            RobotInfo ri = rc.senseRobotAtLocation(ml);
            if (ri == null || !ri.getType().isTowerType()) {
                if (ml.distanceSquaredTo(srpLoc) <= 32) return false;
            }
        }*/
        for (MapLocation ruin : nearbyRuins) {
            RobotInfo r;
            if (FastMath.chebyshev(ruin, srpLoc) <= 3 && rc.canSenseLocation(ruin) && ((r=rc.senseRobotAtLocation(ruin))==null || !r.getType().isTowerType())) {
                //blacklist.add(srpLoc);
                //popTime.add(rc.getRoundNum());
                return false;
            }
        }

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
                if (mi.getPaint().isEnemy()) {
                    blacklist.add(srpLoc);
                    popTime.add(rc.getRoundNum());
                    return false;
                }
            }
        }
        return true;
    }

    static void paintRandomly(RobotController rc) throws  GameActionException {
        MapLocation myLoc = rc.getLocation();

        if(canPaintReal(rc, myLoc) && !rc.senseMapInfo(myLoc).getPaint().isAlly()) {
            rc.attack(rc.getLocation(), trySRP(myLoc));
            return;
        }
        /*
        for (MapInfo mi : nearbys) {
            MapLocation loc = mi.getMapLocation();
            if (!canPaintReal(rc, loc)) continue;
            if (mi.getPaint() == PaintType.EMPTY) {
                rc.attack(loc, trySRP(loc));
                return;
            }
        }
*/
        //if (rc.getPaint() <= 120) return;
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
            //rc.setIndicatorLine(rc.getLocation(), curRuin, 255, 0, 0);
        } else if (curSRP != null) {
            //rc.setIndicatorLine(rc.getLocation(), curSRP, 255, 0, 255);

        }
        if (rc.getID() % 3 == 1) {
            //rc.setIndicatorDot(rc.getLocation(), 0, 255, 0);
            }

        //rc.setIndicatorString(RobotPlayer.indicator);
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
