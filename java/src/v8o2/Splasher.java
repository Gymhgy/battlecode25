package v8o2;

import battlecode.common.*;
import v8o2.fast.*;

public class Splasher {
    private static FastLocSet allyPaintTowers = new FastLocSet();
    private static FastLocSet allyMoneyTowers = new FastLocSet();

    static MapLocation target = null;
    private static int range = UnitType.SPLASHER.actionRadiusSquared;
    static MapLocation prevNext = null;
    int turnsSinceLast = 0;
    static SplasherMicro splasherMicro;

    static void init(RobotController rc) {
        Refill.init(80);
        splasherMicro = new SplasherMicro(rc);
    }

    static void updateTarget(RobotController rc, MapLocation[] ruins) throws GameActionException {
        int best = 10000000;
        MapLocation bl = null;
        for (int i = ruins.length; i-->0; ) {
            RobotInfo r = rc.senseRobotAtLocation(ruins[i]);
            if (!(r != null && r.getTeam() != rc.getTeam() && r.getType().isTowerType())) {
                continue;
            }
            int d = rc.getLocation().distanceSquaredTo(ruins[i]);
            if (d > 16) continue;
            if (d < best) {
                best = d;
                bl = ruins[i];
            }
        }
        if (best <= 16) {
            target = bl;
        }
    }

    static void run(RobotController rc) throws GameActionException {
        cache.clear();
        Communicator.update(rc);
        Communicator.relayEnemyTower(rc);
        boolean refilling = Refill.refill(rc);
        if (refilling) {
            rc.setIndicatorDot(rc.getLocation(), 255, 0, 125);
            rc.setIndicatorString("refilling");
            return;
        }

        RobotPlayer.indicator += "\nworth: " + Clock.getBytecodeNum() + "|";
        MapLocation[] ruins = rc.senseNearbyRuins(-1);
        updateRuinWorths(rc, ruins);
        RobotPlayer.indicator += Clock.getBytecodeNum() + "\n";
        /*RobotPlayer.indicator += "\ncache: " + Clock.getBytecodeNum() + "|";
        computeCache(rc);
        RobotPlayer.indicator += Clock.getBytecodeNum() + "\n";*/

        if (target != null && !Communicator.enemyTowers.contains(target)) target = null;
        if (target == null) target = Communicator.enemyTowers.closest(rc.getLocation());
        updateTarget(rc, ruins);
        if (target != null) {
            if (rc.canSenseLocation(target)) {
                RobotInfo r = rc.senseRobotAtLocation(target);
                if(r != null && Util.isDefenseTower(r.getType())) {
                    if (rc.isActionReady()) performAttack(rc);
                    splasherMicro.doMicro(target, true);
                } else {
                    attackTower(rc);
                    splasherMicro.doMicro(target, true);
                    attackTower(rc);
                    if (rc.isActionReady()) performAttack(rc);
                }
            } else {
                //if (rc.isActionReady()) performAttack(rc);
                MapLocation next = survey(rc);
                if (next != null) {
                    splasherMicro.doMicro(next, false);
                    rc.setIndicatorLine(rc.getLocation(), next, 255, 10, 10);
                } else {
                    Pathfinding.moveToward(rc, target);
                }
                if (rc.isActionReady()) performAttack(rc);
            }
        } else {
            if (rc.isActionReady()) performAttack(rc);

            MapLocation next = survey(rc);
            if (next == null)
                Explorer.smartExplore(rc);
                //else if (prevNext != null && rc.getActionCooldownTurns() < 30)
                //    Pathfinding.moveToward(rc, prevNext);
            else {
                splasherMicro.doMicro(next, false);
            }

            if (next != null)
                rc.setIndicatorLine(rc.getLocation(), next, 255, 10, 10);

            prevNext = next;
        }

        RobotPlayer.indicator += "\nworth: " + Clock.getBytecodeNum() + "|";
        updateRuinWorths(rc, rc.senseNearbyRuins(-1));
        RobotPlayer.indicator += Clock.getBytecodeNum() + "\n";

        if (target != null)
            rc.setIndicatorLine(rc.getLocation(), target, 255, 255, 255);
        endTurn(rc);
    }

    private static void attackTower(RobotController rc) throws GameActionException {
        // System.out.println(target.toString());
        if (FastMath.manhattan(rc.getLocation(), target) <= 4 && rc.isActionReady()) {
            MapLocation bestAttackTile = null;
            int bestValue = Integer.MIN_VALUE;

            for (MapLocation loc : rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 4)) {
                if (FastMath.manhattan(loc, target) <= 2 && rc.canAttack(loc)) {
                    rc.attack(loc);
                    break;
                }
            }
            //performAttack(rc);
        }
    }

    static void computeCache(RobotController rc) throws GameActionException {
        for (MapInfo mi : rc.senseNearbyMapInfos()) {
            MapLocation loc = mi.getMapLocation();
            if (cache.contains(loc)) {
            } else {
                int val = getWorth(mi);
                cache.add(loc, val);
            }
        }
    }

    static FastLocSet nearRuins = new FastLocSet();
    static FastLocSet seens = new FastLocSet();
    private static void updateRuinWorths(RobotController rc, MapLocation[] ruins) {
        for (MapLocation ruin : ruins) {
            if (seens.contains(ruin)) continue;

            MapLocation loc;
            loc = ruin.translate(-2, -2);
            if (loc.x >= 0 && loc.y >= 0) nearRuins.add(loc);
            loc = ruin.translate(-2, -1);
            if (loc.x >= 0 && loc.y >= 0) nearRuins.add(loc);
            loc = ruin.translate(-2, 0);
            if (loc.x >= 0 && loc.y >= 0) nearRuins.add(loc);
            loc = ruin.translate(-2, 1);
            if (loc.x >= 0 && loc.y >= 0) nearRuins.add(loc);
            loc = ruin.translate(-2, 2);
            if (loc.x >= 0 && loc.y >= 0) nearRuins.add(loc);

            loc = ruin.translate(-1, -2);
            if (loc.x >= 0 && loc.y >= 0) nearRuins.add(loc);
            loc = ruin.translate(-1, -1);
            if (loc.x >= 0 && loc.y >= 0) nearRuins.add(loc);
            loc = ruin.translate(-1, 0);
            if (loc.x >= 0 && loc.y >= 0) nearRuins.add(loc);
            loc = ruin.translate(-1, 1);
            if (loc.x >= 0 && loc.y >= 0) nearRuins.add(loc);
            loc = ruin.translate(-1, 2);
            if (loc.x >= 0 && loc.y >= 0) nearRuins.add(loc);

            loc = ruin.translate(0, -2);
            if (loc.x >= 0 && loc.y >= 0) nearRuins.add(loc);
            loc = ruin.translate(0, -1);
            if (loc.x >= 0 && loc.y >= 0) nearRuins.add(loc);
            loc = ruin.translate(0, 1);
            if (loc.x >= 0 && loc.y >= 0) nearRuins.add(loc);
            loc = ruin.translate(0, 2);
            if (loc.x >= 0 && loc.y >= 0) nearRuins.add(loc);

            loc = ruin.translate(1, -2);
            if (loc.x >= 0 && loc.y >= 0) nearRuins.add(loc);
            loc = ruin.translate(1, -1);
            if (loc.x >= 0 && loc.y >= 0) nearRuins.add(loc);
            loc = ruin.translate(1, 0);
            if (loc.x >= 0 && loc.y >= 0) nearRuins.add(loc);
            loc = ruin.translate(1, 1);
            if (loc.x >= 0 && loc.y >= 0) nearRuins.add(loc);
            loc = ruin.translate(1, 2);
            if (loc.x >= 0 && loc.y >= 0) nearRuins.add(loc);

            loc = ruin.translate(2, -2);
            if (loc.x >= 0 && loc.y >= 0) nearRuins.add(loc);
            loc = ruin.translate(2, -1);
            if (loc.x >= 0 && loc.y >= 0) nearRuins.add(loc);
            loc = ruin.translate(2, 0);
            if (loc.x >= 0 && loc.y >= 0) nearRuins.add(loc);
            loc = ruin.translate(2, 1);
            if (loc.x >= 0 && loc.y >= 0) nearRuins.add(loc);
            loc = ruin.translate(2, 2);
            if (loc.x >= 0 && loc.y >= 0) nearRuins.add(loc);

            seens.add(ruin);
        }
    }
    private static int getWorth(MapInfo mi) {
        PaintType pt = mi.getPaint();
        //TODO: tweak this...
        if (mi.hasRuin()) return 0;
        if (pt.isAlly()) {
            if (nearRuins.contains(mi.getMapLocation())) return -2;
            return 0;
        }
        if (pt.isEnemy()) {
            if (nearRuins.contains(mi.getMapLocation())) return 7;
            return 4;
        }
        return 1;
    }

    private static int worthThreshold = 12; // leaving this here: easier to see and tweak

    static int[][] attackTiles2 = {
            {-2, 0}, {2, 0}, {0, -2}, {0, 2}, {0, 0}
    };
    static int[][] coordinates = {
            {0, 0}, {-1, 0}, {1, 0}, {0, -1}, {0, 1},
            {-1, -1}, {-1, 1}, {1, -1}, {1, 1}, /*{-2, 0},
            {2, 0}, {0, -2}, {0, 2}*/
    };
    static FastLocIntMap cache = new FastLocIntMap();

    static void performAttack(RobotController rc) throws GameActionException {
            /*  RobotInfo[] enemies = rc.senseNearbyRobots(range, rc.getTeam().opponent());
            RobotInfo[] allies = rc.senseNearbyRobots(range, rc.getTeam());
            I'm leaving enemies/allies here... even though it isn't used right now. */
        //MapInfo[] attackTiles = rc.senseNearbyMapInfos(range);

        int bestWorth = 0;
        MapLocation best = null;
        String ind = Clock.getBytecodeNum() + "|";
        MapLocation[] ruins = rc.senseNearbyRuins(-1);
        MapLocation squareMapLoc;
        squareMapLoc = rc.getLocation().translate(0, 0);
        if (rc.canSenseLocation(squareMapLoc)) {
            int worth = totalWorth(rc, squareMapLoc);
            if (worth > worthThreshold) {
                best = squareMapLoc;
                bestWorth = worth;
            }
        }

        squareMapLoc = rc.getLocation().translate(-1, 0);
        if (rc.canSenseLocation(squareMapLoc)) {
            int worth = totalWorth(rc, squareMapLoc);
            if (worth > worthThreshold) {
                best = squareMapLoc;
                bestWorth = worth;
            }
        }

        squareMapLoc = rc.getLocation().translate(1, 0);
        if (rc.canSenseLocation(squareMapLoc)) {
            int worth = totalWorth(rc, squareMapLoc);
            if (worth > worthThreshold) {
                best = squareMapLoc;
                bestWorth = worth;
            }
        }

        squareMapLoc = rc.getLocation().translate(0, -1);
        if (rc.canSenseLocation(squareMapLoc)) {
            int worth = totalWorth(rc, squareMapLoc);
            if (worth > worthThreshold) {
                best = squareMapLoc;
                bestWorth = worth;
            }
        }

        squareMapLoc = rc.getLocation().translate(0, 1);
        if (rc.canSenseLocation(squareMapLoc)) {
            int worth = totalWorth(rc, squareMapLoc);
            if (worth > worthThreshold) {
                best = squareMapLoc;
                bestWorth = worth;
            }
        }

        squareMapLoc = rc.getLocation().translate(-1, -1);
        if (rc.canSenseLocation(squareMapLoc)) {
            int worth = totalWorth(rc, squareMapLoc);
            if (worth > worthThreshold) {
                best = squareMapLoc;
                bestWorth = worth;
            }
        }

        squareMapLoc = rc.getLocation().translate(-1, 1);
        if (rc.canSenseLocation(squareMapLoc)) {
            int worth = totalWorth(rc, squareMapLoc);
            if (worth > worthThreshold) {
                best = squareMapLoc;
                bestWorth = worth;
            }
        }

        squareMapLoc = rc.getLocation().translate(1, -1);
        if (rc.canSenseLocation(squareMapLoc)) {
            int worth = totalWorth(rc, squareMapLoc);
            if (worth > worthThreshold) {
                best = squareMapLoc;
                bestWorth = worth;
            }
        }

        squareMapLoc = rc.getLocation().translate(1, 1);
        if (rc.canSenseLocation(squareMapLoc)) {
            int worth = totalWorth(rc, squareMapLoc);
            if (worth > worthThreshold) {
                best = squareMapLoc;
                bestWorth = worth;
            }
        }

        squareMapLoc = rc.getLocation().translate(2, 0);
        if (rc.canSenseLocation(squareMapLoc)) {
            int worth = totalWorth(rc, squareMapLoc);
            if (worth > worthThreshold) {
                best = squareMapLoc;
                bestWorth = worth;
            }
        }

        squareMapLoc = rc.getLocation().translate(-2, 0);
        if (rc.canSenseLocation(squareMapLoc)) {
            int worth = totalWorth(rc, squareMapLoc);
            if (worth > worthThreshold) {
                best = squareMapLoc;
                bestWorth = worth;
            }
        }

        squareMapLoc = rc.getLocation().translate(0, 2);
        if (rc.canSenseLocation(squareMapLoc)) {
            int worth = totalWorth(rc, squareMapLoc);
            if (worth > worthThreshold) {
                best = squareMapLoc;
                bestWorth = worth;
            }
        }

        squareMapLoc = rc.getLocation().translate(0, -2);
        if (rc.canSenseLocation(squareMapLoc)) {
            int worth = totalWorth(rc, squareMapLoc);
            if (worth > worthThreshold) {
                best = squareMapLoc;
                bestWorth = worth;
            }
        }
        ind += Clock.getBytecodeNum();
        RobotPlayer.indicator += ind;
        //rc.setIndicatorString(ind);
        int thresh = rc.getRoundNum() < 150 ? 24 : worthThreshold;
        if (best != null && canPaintReal(rc, best) && bestWorth > worthThreshold) {
            if (rc.canAttack(best)) {
                rc.attack(best);
            }
        }
    }

    static boolean canPaintReal(RobotController rc, MapLocation loc) throws GameActionException { // canPaint that checks for cost
        int paintCap = rc.getPaint();
        return rc.isActionReady() && paintCap > rc.getType().attackCost && rc.canPaint(loc);
    }


    static void endTurn(RobotController rc) throws GameActionException {
        rc.setIndicatorString(RobotPlayer.indicator);
    }


    private static MapLocation survey(RobotController rc) throws GameActionException {
        int w1, w2, w3, w4, w5, w6, w7, w8;
        w1 = w2 = w3 = w4 = w5 = w6 = w7 = w8 = 0;
        MapLocation loc = rc.getLocation();
        if (rc.canSenseLocation(loc.translate(0, 0)) &&
                rc.senseMapInfo(loc.translate(0, 0)).getPaint().isEnemy()) w1++;
        if (rc.canSenseLocation(loc.translate(0, 1)) &&
                rc.senseMapInfo(loc.translate(0, 1)).getPaint().isEnemy()) w1++;
        if (rc.canSenseLocation(loc.translate(0, 2)) &&
                rc.senseMapInfo(loc.translate(0, 2)).getPaint().isEnemy()) w1++;
        if (rc.canSenseLocation(loc.translate(1, 0)) &&
                rc.senseMapInfo(loc.translate(1, 0)).getPaint().isEnemy()) w1++;
        if (rc.canSenseLocation(loc.translate(1, 1)) &&
                rc.senseMapInfo(loc.translate(1, 1)).getPaint().isEnemy()) w1++;
        if (rc.canSenseLocation(loc.translate(1, 2)) &&
                rc.senseMapInfo(loc.translate(1, 2)).getPaint().isEnemy()) w1++;
        if (rc.canSenseLocation(loc.translate(2, 0)) &&
                rc.senseMapInfo(loc.translate(2, 0)).getPaint().isEnemy()) w1++;
        if (rc.canSenseLocation(loc.translate(2, 1)) &&
                rc.senseMapInfo(loc.translate(2, 1)).getPaint().isEnemy()) w1++;
        if (rc.canSenseLocation(loc.translate(2, 2)) &&
                rc.senseMapInfo(loc.translate(2, 2)).getPaint().isEnemy()) w1++;
        if (rc.canSenseLocation(loc.translate(-2, 0)) &&
                rc.senseMapInfo(loc.translate(-2, 0)).getPaint().isEnemy()) w2++;
        if (rc.canSenseLocation(loc.translate(-2, 1)) &&
                rc.senseMapInfo(loc.translate(-2, 1)).getPaint().isEnemy()) w2++;
        if (rc.canSenseLocation(loc.translate(-2, 2)) &&
                rc.senseMapInfo(loc.translate(-2, 2)).getPaint().isEnemy()) w2++;
        if (rc.canSenseLocation(loc.translate(-1, 0)) &&
                rc.senseMapInfo(loc.translate(-1, 0)).getPaint().isEnemy()) w2++;
        if (rc.canSenseLocation(loc.translate(-1, 1)) &&
                rc.senseMapInfo(loc.translate(-1, 1)).getPaint().isEnemy()) w2++;
        if (rc.canSenseLocation(loc.translate(-1, 2)) &&
                rc.senseMapInfo(loc.translate(-1, 2)).getPaint().isEnemy()) w2++;
        if (rc.canSenseLocation(loc.translate(0, 0)) &&
                rc.senseMapInfo(loc.translate(0, 0)).getPaint().isEnemy()) w2++;
        if (rc.canSenseLocation(loc.translate(0, 1)) &&
                rc.senseMapInfo(loc.translate(0, 1)).getPaint().isEnemy()) w2++;
        if (rc.canSenseLocation(loc.translate(0, 2)) &&
                rc.senseMapInfo(loc.translate(0, 2)).getPaint().isEnemy()) w2++;
        if (rc.canSenseLocation(loc.translate(0, -2)) &&
                rc.senseMapInfo(loc.translate(0, -2)).getPaint().isEnemy()) w3++;
        if (rc.canSenseLocation(loc.translate(0, -1)) &&
                rc.senseMapInfo(loc.translate(0, -1)).getPaint().isEnemy()) w3++;
        if (rc.canSenseLocation(loc.translate(0, 0)) &&
                rc.senseMapInfo(loc.translate(0, 0)).getPaint().isEnemy()) w3++;
        if (rc.canSenseLocation(loc.translate(1, -2)) &&
                rc.senseMapInfo(loc.translate(1, -2)).getPaint().isEnemy()) w3++;
        if (rc.canSenseLocation(loc.translate(1, -1)) &&
                rc.senseMapInfo(loc.translate(1, -1)).getPaint().isEnemy()) w3++;
        if (rc.canSenseLocation(loc.translate(1, 0)) &&
                rc.senseMapInfo(loc.translate(1, 0)).getPaint().isEnemy()) w3++;
        if (rc.canSenseLocation(loc.translate(2, -2)) &&
                rc.senseMapInfo(loc.translate(2, -2)).getPaint().isEnemy()) w3++;
        if (rc.canSenseLocation(loc.translate(2, -1)) &&
                rc.senseMapInfo(loc.translate(2, -1)).getPaint().isEnemy()) w3++;
        if (rc.canSenseLocation(loc.translate(2, 0)) &&
                rc.senseMapInfo(loc.translate(2, 0)).getPaint().isEnemy()) w3++;
        if (rc.canSenseLocation(loc.translate(-2, -2)) &&
                rc.senseMapInfo(loc.translate(-2, -2)).getPaint().isEnemy()) w4++;
        if (rc.canSenseLocation(loc.translate(-2, -1)) &&
                rc.senseMapInfo(loc.translate(-2, -1)).getPaint().isEnemy()) w4++;
        if (rc.canSenseLocation(loc.translate(-2, 0)) &&
                rc.senseMapInfo(loc.translate(-2, 0)).getPaint().isEnemy()) w4++;
        if (rc.canSenseLocation(loc.translate(-1, -2)) &&
                rc.senseMapInfo(loc.translate(-1, -2)).getPaint().isEnemy()) w4++;
        if (rc.canSenseLocation(loc.translate(-1, -1)) &&
                rc.senseMapInfo(loc.translate(-1, -1)).getPaint().isEnemy()) w4++;
        if (rc.canSenseLocation(loc.translate(-1, 0)) &&
                rc.senseMapInfo(loc.translate(-1, 0)).getPaint().isEnemy()) w4++;
        if (rc.canSenseLocation(loc.translate(0, -2)) &&
                rc.senseMapInfo(loc.translate(0, -2)).getPaint().isEnemy()) w4++;
        if (rc.canSenseLocation(loc.translate(0, -1)) &&
                rc.senseMapInfo(loc.translate(0, -1)).getPaint().isEnemy()) w4++;
        if (rc.canSenseLocation(loc.translate(0, 0)) &&
                rc.senseMapInfo(loc.translate(0, 0)).getPaint().isEnemy()) w4++;
        if (rc.canSenseLocation(loc.translate(1, -1)) &&
                rc.senseMapInfo(loc.translate(1, -1)).getPaint().isEnemy()) w5++;
        if (rc.canSenseLocation(loc.translate(1, 0)) &&
                rc.senseMapInfo(loc.translate(1, 0)).getPaint().isEnemy()) w5++;
        if (rc.canSenseLocation(loc.translate(1, 1)) &&
                rc.senseMapInfo(loc.translate(1, 1)).getPaint().isEnemy()) w5++;
        if (rc.canSenseLocation(loc.translate(2, -1)) &&
                rc.senseMapInfo(loc.translate(2, -1)).getPaint().isEnemy()) w5++;
        if (rc.canSenseLocation(loc.translate(2, 0)) &&
                rc.senseMapInfo(loc.translate(2, 0)).getPaint().isEnemy()) w5++;
        if (rc.canSenseLocation(loc.translate(2, 1)) &&
                rc.senseMapInfo(loc.translate(2, 1)).getPaint().isEnemy()) w5++;
        if (rc.canSenseLocation(loc.translate(3, -1)) &&
                rc.senseMapInfo(loc.translate(3, -1)).getPaint().isEnemy()) w5++;
        if (rc.canSenseLocation(loc.translate(3, 0)) &&
                rc.senseMapInfo(loc.translate(3, 0)).getPaint().isEnemy()) w5++;
        if (rc.canSenseLocation(loc.translate(3, 1)) &&
                rc.senseMapInfo(loc.translate(3, 1)).getPaint().isEnemy()) w5++;
        if (rc.canSenseLocation(loc.translate(-3, -1)) &&
                rc.senseMapInfo(loc.translate(-3, -1)).getPaint().isEnemy()) w6++;
        if (rc.canSenseLocation(loc.translate(-3, 0)) &&
                rc.senseMapInfo(loc.translate(-3, 0)).getPaint().isEnemy()) w6++;
        if (rc.canSenseLocation(loc.translate(-3, 1)) &&
                rc.senseMapInfo(loc.translate(-3, 1)).getPaint().isEnemy()) w6++;
        if (rc.canSenseLocation(loc.translate(-2, -1)) &&
                rc.senseMapInfo(loc.translate(-2, -1)).getPaint().isEnemy()) w6++;
        if (rc.canSenseLocation(loc.translate(-2, 0)) &&
                rc.senseMapInfo(loc.translate(-2, 0)).getPaint().isEnemy()) w6++;
        if (rc.canSenseLocation(loc.translate(-2, 1)) &&
                rc.senseMapInfo(loc.translate(-2, 1)).getPaint().isEnemy()) w6++;
        if (rc.canSenseLocation(loc.translate(-1, -1)) &&
                rc.senseMapInfo(loc.translate(-1, -1)).getPaint().isEnemy()) w6++;
        if (rc.canSenseLocation(loc.translate(-1, 0)) &&
                rc.senseMapInfo(loc.translate(-1, 0)).getPaint().isEnemy()) w6++;
        if (rc.canSenseLocation(loc.translate(-1, 1)) &&
                rc.senseMapInfo(loc.translate(-1, 1)).getPaint().isEnemy()) w6++;
        if (rc.canSenseLocation(loc.translate(-1, 1)) &&
                rc.senseMapInfo(loc.translate(-1, 1)).getPaint().isEnemy()) w7++;
        if (rc.canSenseLocation(loc.translate(-1, 2)) &&
                rc.senseMapInfo(loc.translate(-1, 2)).getPaint().isEnemy()) w7++;
        if (rc.canSenseLocation(loc.translate(-1, 3)) &&
                rc.senseMapInfo(loc.translate(-1, 3)).getPaint().isEnemy()) w7++;
        if (rc.canSenseLocation(loc.translate(0, 1)) &&
                rc.senseMapInfo(loc.translate(0, 1)).getPaint().isEnemy()) w7++;
        if (rc.canSenseLocation(loc.translate(0, 2)) &&
                rc.senseMapInfo(loc.translate(0, 2)).getPaint().isEnemy()) w7++;
        if (rc.canSenseLocation(loc.translate(0, 3)) &&
                rc.senseMapInfo(loc.translate(0, 3)).getPaint().isEnemy()) w7++;
        if (rc.canSenseLocation(loc.translate(1, 1)) &&
                rc.senseMapInfo(loc.translate(1, 1)).getPaint().isEnemy()) w7++;
        if (rc.canSenseLocation(loc.translate(1, 2)) &&
                rc.senseMapInfo(loc.translate(1, 2)).getPaint().isEnemy()) w7++;
        if (rc.canSenseLocation(loc.translate(1, 3)) &&
                rc.senseMapInfo(loc.translate(1, 3)).getPaint().isEnemy()) w7++;
        if (rc.canSenseLocation(loc.translate(-1, -3)) &&
                rc.senseMapInfo(loc.translate(-1, -3)).getPaint().isEnemy()) w8++;
        if (rc.canSenseLocation(loc.translate(-1, -2)) &&
                rc.senseMapInfo(loc.translate(-1, -2)).getPaint().isEnemy()) w8++;
        if (rc.canSenseLocation(loc.translate(-1, -1)) &&
                rc.senseMapInfo(loc.translate(-1, -1)).getPaint().isEnemy()) w8++;
        if (rc.canSenseLocation(loc.translate(0, -3)) &&
                rc.senseMapInfo(loc.translate(0, -3)).getPaint().isEnemy()) w8++;
        if (rc.canSenseLocation(loc.translate(0, -2)) &&
                rc.senseMapInfo(loc.translate(0, -2)).getPaint().isEnemy()) w8++;
        if (rc.canSenseLocation(loc.translate(0, -1)) &&
                rc.senseMapInfo(loc.translate(0, -1)).getPaint().isEnemy()) w8++;
        if (rc.canSenseLocation(loc.translate(1, -3)) &&
                rc.senseMapInfo(loc.translate(1, -3)).getPaint().isEnemy()) w8++;
        if (rc.canSenseLocation(loc.translate(1, -2)) &&
                rc.senseMapInfo(loc.translate(1, -2)).getPaint().isEnemy()) w8++;
        if (rc.canSenseLocation(loc.translate(1, -1)) &&
                rc.senseMapInfo(loc.translate(1, -1)).getPaint().isEnemy()) w8++;


        int[] arr = {w1, w2, w3, w4, w5, w6, w7, w8};
        int max = 0;
        for (int i = 8; i-- > 0; ) max = Math.max(max, arr[i]);
        if (max < 4) return null;
        if (w1 == max) return loc.translate(1, 1);
        if (w2 == max) return loc.translate(-1, 1);
        if (w3 == max) return loc.translate(1, -1);
        if (w4 == max) return loc.translate(-1, -1);
        if (w5 == max) return loc.translate(2, 0);
        if (w6 == max) return loc.translate(-2, 0);
        if (w7 == max) return loc.translate(0, 2);
        if (w8 == max) return loc.translate(0, -2);

        return null;
    }


    static int totalWorth(RobotController rc, MapLocation square) throws GameActionException {
        MapLocation loc;
        int worth = 0;
        MapInfo mi;

        loc = square.translate(0, 0);
        if (rc.canSenseLocation(loc)) {
            mi = rc.senseMapInfo(loc);
            if (cache.contains(loc)) {
                worth += cache.getVal(loc);
            } else {
                int val = getWorth(mi);
                /*RobotInfo r = rc.senseRobotAtLocation(loc);
                if (r != null) {
                    if (r.getType().isTowerType() && r.getTeam() != rc.getTeam())
                        worth += 12;
                }*/
                cache.add(loc, val);
                worth += val;
            }
        }
        loc = square.translate(-1, 0);
        if (rc.canSenseLocation(loc)) {
            mi = rc.senseMapInfo(loc);
            if (cache.contains(loc)) {
                worth += cache.getVal(loc);
            } else {
                int val = getWorth(mi);
                /*RobotInfo r = rc.senseRobotAtLocation(loc);
                if (r != null) {
                    if (r.getType().isTowerType() && r.getTeam() != rc.getTeam())
                        worth += 12;
                }*/
                cache.add(loc, val);
                worth += val;
            }

        }
        loc = square.translate(1, 0);
        if (rc.canSenseLocation(loc)) {
            mi = rc.senseMapInfo(loc);
            if (cache.contains(loc)) {
                worth += cache.getVal(loc);
            } else {
                int val = getWorth(mi);
                /*RobotInfo r = rc.senseRobotAtLocation(loc);
                if (r != null) {
                    if (r.getType().isTowerType() && r.getTeam() != rc.getTeam())
                        worth += 12;
                }*/
                cache.add(loc, val);
                worth += val;
            }

        }
        loc = square.translate(0, -1);
        if (rc.canSenseLocation(loc)) {
            mi = rc.senseMapInfo(loc);
            if (cache.contains(loc)) {
                worth += cache.getVal(loc);
            } else {
                int val = getWorth(mi);
                /*RobotInfo r = rc.senseRobotAtLocation(loc);
                if (r != null) {
                    if (r.getType().isTowerType() && r.getTeam() != rc.getTeam())
                        worth += 12;
                }*/
                cache.add(loc, val);
                worth += val;
            }

        }
        loc = square.translate(0, 1);
        if (rc.canSenseLocation(loc)) {
            mi = rc.senseMapInfo(loc);
            if (cache.contains(loc)) {
                worth += cache.getVal(loc);
            } else {
                int val = getWorth(mi);
                /*RobotInfo r = rc.senseRobotAtLocation(loc);
                if (r != null) {
                    if (r.getType().isTowerType() && r.getTeam() != rc.getTeam())
                        worth += 12;
                }*/
                cache.add(loc, val);
                worth += val;
            }

        }
        loc = square.translate(-1, -1);
        if (rc.canSenseLocation(loc)) {
            mi = rc.senseMapInfo(loc);
            if (cache.contains(loc)) {
                worth += cache.getVal(loc);
            } else {
                int val = getWorth(mi);
                /*RobotInfo r = rc.senseRobotAtLocation(loc);
                if (r != null) {
                    if (r.getType().isTowerType() && r.getTeam() != rc.getTeam())
                        worth += 12;
                }*/
                cache.add(loc, val);
                worth += val;
            }

        }
        loc = square.translate(-1, 1);
        if (rc.canSenseLocation(loc)) {
            mi = rc.senseMapInfo(loc);
            if (cache.contains(loc)) {
                worth += cache.getVal(loc);
            } else {
                int val = getWorth(mi);
                /*RobotInfo r = rc.senseRobotAtLocation(loc);
                if (r != null) {
                    if (r.getType().isTowerType() && r.getTeam() != rc.getTeam())
                        worth += 12;
                }*/
                cache.add(loc, val);
                worth += val;
            }

        }
        loc = square.translate(1, -1);
        if (rc.canSenseLocation(loc)) {
            mi = rc.senseMapInfo(loc);
            if (cache.contains(loc)) {
                worth += cache.getVal(loc);
            } else {
                int val = getWorth(mi);
                /*RobotInfo r = rc.senseRobotAtLocation(loc);
                if (r != null) {
                    if (r.getType().isTowerType() && r.getTeam() != rc.getTeam())
                        worth += 12;
                }*/
                cache.add(loc, val);
                worth += val;
            }

        }
        loc = square.translate(1, 1);
        if (rc.canSenseLocation(loc)) {
            mi = rc.senseMapInfo(loc);
            if (cache.contains(loc)) {
                worth += cache.getVal(loc);
            } else {
                int val = getWorth(mi);
                /*RobotInfo r = rc.senseRobotAtLocation(loc);
                if (r != null) {
                    if (r.getType().isTowerType() && r.getTeam() != rc.getTeam())
                        worth += 12;
                }*/
                cache.add(loc, val);
                worth += val;
            }

        }
        return worth;

    }

}
