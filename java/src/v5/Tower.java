package v5;

import battlecode.common.*;
import v5.fast.FastLocSet;
import v5.fast.FastMath;

public class Tower {
    static final Direction[] directions = {
            Direction.NORTH,
            Direction.NORTHEAST,
            Direction.EAST,
            Direction.SOUTHEAST,
            Direction.SOUTH,
            Direction.SOUTHWEST,
            Direction.WEST,
            Direction.NORTHWEST,
    };
    static boolean og = false;
    static MapLocation[] spawners;
    static MapLocation center;
    static void init(RobotController rc) throws GameActionException {
        if (rc.getRoundNum() == 1) {
            og = true;
        }
        center = new MapLocation(rc.getMapWidth()/2, rc.getMapHeight()/2);
        spawners = rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 4);
    }


    static void run(RobotController rc) throws GameActionException {
        hearThePeopleSpeak(rc);
        MapLocation en = enemies.closest(rc.getLocation());

        rc.attack(null);
        RobotInfo[] allies = rc.senseNearbyRobots(-1, rc.getTeam());

        if (rc.getChips() > 2000 && rc.getRoundNum() > 50 && Util.isPaintTower(rc.getType())) {
            if(rc.canUpgradeTower(rc.getLocation())) {
                rc.upgradeTower(rc.getLocation());
            }
        }

        if (rc.getChips() > 1300) {


            UnitType type = UnitType.SOLDIER;
            if (og && rc.getRoundNum() < 20) {
                type = UnitType.SOLDIER;
            }
            else if (rc.getRoundNum() < 150 && FastMath.rand256() % 4 < 1) {
                type = UnitType.MOPPER;
            }
            else {
                var distr = new UnitType[] {UnitType.SOLDIER, UnitType.SOLDIER, UnitType.MOPPER, UnitType.SPLASHER, UnitType.SPLASHER};
                type = distr[FastMath.rand256() % distr.length];
            }

            MapLocation nextLoc = decideOnSpawn(rc, en);
            if (nextLoc != null && rc.canBuildRobot(type, nextLoc)) {
                rc.buildRobot(type, nextLoc);
            }
        }

        attackNearby(rc);
        letThePeopleKnow(rc);
        if (en != null) {
            rc.setIndicatorLine(rc.getLocation(), en, 255, 255, 255);
            rc.setIndicatorString(enemies.closest(rc.getLocation()).toString());
        }
        else rc.setIndicatorString(String.valueOf(enemies.size));
    }

    static MapLocation decideOnSpawn(RobotController rc, MapLocation enemy) throws GameActionException {
        if (!rc.isActionReady() || spawners.length == 0) return null;

        RobotInfo[] nearbyAllies = rc.senseNearbyRobots(-1, rc.getTeam());
        FastLocSet adjacentLocs = new FastLocSet();

        for (RobotInfo enemyRobot : nearbyAllies) {
            for (Direction dir : Direction.allDirections()) {
                adjacentLocs.add(enemyRobot.getLocation().add(dir));
            }
        }

        MapLocation bestLocation = null;
        int bestScore = Integer.MIN_VALUE;

        for (MapLocation spawner : spawners) {
            if (!rc.canBuildRobot(UnitType.SOLDIER, spawner)) continue;

            int score = 0;

            if (!adjacentLocs.contains(spawner)) {
                score += 1000;
            }

            if (enemy != null) {
                int dist = spawner.distanceSquaredTo(enemy);
                score -= dist;
                if (dist <= UnitType.LEVEL_THREE_PAINT_TOWER.actionRadiusSquared) score -= 10000;
            } else {
                score -= spawner.distanceSquaredTo(center);
            }

            score += FastMath.rand256() % 10;
            if (score > bestScore) {
                bestScore = score;
                bestLocation = spawner;
            }
        }
        return bestLocation;
    }

    static void attackNearby(RobotController rc) throws GameActionException {
        RobotInfo[] enemies = rc.senseNearbyRobots(rc.getType().actionRadiusSquared, rc.getTeam().opponent());

        for (RobotInfo enemy : enemies) {
            if (rc.isActionReady() && rc.canAttack(enemy.getLocation())) {
                rc.attack(enemy.getLocation());
                return;
            }
        }
    }



    static FastLocSet enemies = new FastLocSet();
    static FastLocSet queue = new FastLocSet();
    static void hearThePeopleSpeak(RobotController rc) throws GameActionException {
        Message[] messages = rc.readMessages(rc.getRoundNum()-1);
        for(Message m : messages) {
            MapLocation loc = Communicator.int2loc(m.getBytes());
            //if (!enemies.contains(loc)) queue.add(loc);
            if (loc.equals(rc.getLocation())) continue;
            enemies.add(loc);
            donotsend.add(m.getSenderID(), m.getBytes());
        }
    }

    static FastLocSet donotsend = new FastLocSet();
    static void letThePeopleKnow(RobotController rc) throws GameActionException {
        MapLocation enemy = enemies.closest(rc.getLocation());
        if (enemy == null) return;
        rc.broadcastMessage(Communicator.loc2int(enemy));
        RobotInfo[] allies = rc.senseNearbyRobots(-1, rc.getTeam());
        for (RobotInfo r : allies) {
            if (donotsend.contains(r.ID, Communicator.loc2int(enemy))) continue;
            if (rc.canSendMessage(r.getLocation())) {
                rc.sendMessage(r.getLocation(), Communicator.loc2int(enemy));
                donotsend.add(rc.getID(), Communicator.loc2int(enemy));
            }
        }
    }
}
