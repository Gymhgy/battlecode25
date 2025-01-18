package v4;

import battlecode.common.*;
import v4.fast.FastLocSet;

public class Mopper {

    private static FastLocSet allyPaintTowers = new FastLocSet();
    private static FastLocSet allyMoneyTowers = new FastLocSet();

    static String indicator = "";
    static MapInfo[] nearbyTiles;
    static RobotInfo[] nearbyRobots;
    private static MapLocation enemyTower;
    static FastLocSet enemyTowers = new FastLocSet();

    static void init(RobotController rc) {
        Refill.init(40);
    }

    static void run(RobotController rc) throws GameActionException {

        nearbyTiles = rc.senseNearbyMapInfos();
        nearbyRobots = rc.senseNearbyRobots();
        indicator = "";
        Communicator.update(rc);
        Communicator.relayEnemyTower(rc);
        boolean refilling = Refill.refill(rc);
        if (!refilling) if(rc.isActionReady()) refillSplasher(rc);
        if (rc.isActionReady()) performAttack(rc);
        if (refilling) return;
        goTowardsEnemyPaint(rc);
        if (rc.isMovementReady())
            Explorer.smartExplore(rc);
        if(rc.isActionReady()) refillSplasher(rc);
        if (rc.isActionReady()) performAttack(rc);

    }
    static void goTowardsEnemyPaint(RobotController rc) throws GameActionException {
        nearbyTiles = rc.senseNearbyMapInfos();
        MapLocation[] nearbyRuins = rc.senseNearbyRuins(-1);
        RobotInfo[] nearbyAllies = rc.senseNearbyRobots(-1, rc.getTeam());

        // Precompute ruins with nearby soldiers
        FastLocSet ruinsWithSoldiers = new FastLocSet();

        for (MapLocation ruin : nearbyRuins) {
            for (RobotInfo enemy : nearbyAllies) {
                if (enemy.getType() == UnitType.SOLDIER && enemy.getLocation().distanceSquaredTo(ruin) <= 2) {
                    ruinsWithSoldiers.add(ruin);
                    break;
                }
            }
        }

        int highestPriority = Integer.MIN_VALUE;
        MapLocation target = null;

        for (MapInfo tile : nearbyTiles) {
            if (tile.getPaint().isEnemy()) {
                MapLocation loc = tile.getMapLocation();

                // Check priority
                int priority = 1; // Default: Closest paint

                for (MapLocation ruin : nearbyRuins) {
                    if (ruin.distanceSquaredTo(loc) <= 8) {
                        if (ruinsWithSoldiers.contains(ruin)) {
                            priority = 3; // Near ruin with soldier
                            break;
                        } else {
                            priority = 2; // Near ruin
                        }
                    }
                }

                int dist = rc.getLocation().distanceSquaredTo(loc);
                if (priority > highestPriority || (priority == highestPriority && dist < rc.getLocation().distanceSquaredTo(target))) {
                    target = loc;
                    highestPriority = priority;
                    if (priority == 3) break;
                }
            }
        }

        if (target != null) {
            Pathfinding.moveToward(rc, target);
            rc.setIndicatorLine(rc.getLocation(), target, 155, 255, 155);
        }
    }

    static boolean refillSplasher(RobotController rc) throws GameActionException {
        if (rc.getPaint() < 60) return false;
        RobotInfo[] allies = rc.senseNearbyRobots(2, rc.getTeam());
        MapLocation paint = Communicator.paintTowers.closest(rc.getLocation());
        for (RobotInfo r : allies) {
            if (r.getType() == UnitType.SPLASHER) {
                if (rc.canTransferPaint(r.getLocation(), 25)) {
                    rc.transferPaint(r.getLocation(), 25);
                    return true;
                }
            }
        }
        return false;
    }

    static void performAttack(RobotController rc) throws GameActionException {
        RobotInfo[] enemies = rc.senseNearbyRobots(2, rc.getTeam().opponent());

        MapLocation hitLoc = null;
        for (RobotInfo enemy : enemies) {
            if (rc.senseMapInfo(enemy.getLocation()).getPaint().isEnemy()) {
                hitLoc = enemy.getLocation();
                break;
            }
        }

        Direction bestDirection = null;
        int maxHits = 0;

        // Iterate through all cardinal directions
        for (Direction dir : Direction.cardinalDirections()) {
            int hits = 0;

            for (int step = 1; step <= 2; step++) {
                MapLocation orig = step == 1 ? rc.getLocation() : rc.getLocation().add(dir);
                MapLocation target = orig.add(dir);

                MapLocation adj1 = orig.add(dir.rotateRight());
                MapLocation adj2 = orig.add(dir.rotateLeft());
                if (rc.canSenseLocation(adj1)) {
                    RobotInfo robot = rc.senseRobotAtLocation(adj1);
                    if (robot != null && robot.getTeam() == rc.getTeam().opponent()) {
                        hits++;
                    }
                }
                if (rc.canSenseLocation(adj2)) {
                    RobotInfo robot = rc.senseRobotAtLocation(adj2);
                    if (robot != null && robot.getTeam() == rc.getTeam().opponent()) {
                        hits++;
                    }
                }

                if (rc.canSenseLocation(target)) {
                    RobotInfo robot = rc.senseRobotAtLocation(target);
                    if (robot != null && robot.getTeam() == rc.getTeam().opponent()) {
                        hits++;
                    }
                }
            }

            // Update best direction if this direction hits more enemies
            if (hits > maxHits) {
                maxHits = hits;
                bestDirection = dir;
            }
        }

        if ((hitLoc == null || maxHits >= 2) && maxHits > 0) {
            rc.mopSwing(bestDirection);
            return;
        }
        else if (hitLoc != null) {
            rc.attack(hitLoc);
            return;
        }

        // Attack any tile with enemy paint on it
        for (MapInfo loc : rc.senseNearbyMapInfos(2)) {
            if (loc.getPaint().isEnemy()) {
                rc.attack(loc.getMapLocation());
                return;
            }
        }

    }


    private static MapLocation reportBack (RobotController rc) throws GameActionException{

        return enemyTower;
    }
}
