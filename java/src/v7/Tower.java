package v7;

import battlecode.common.*;
import v6.fast.FastIntSet;
import v6.fast.FastLocSet;
import v6.fast.FastMath;

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
        cleanup(rc);

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
    static FastIntSet queue = new FastIntSet();
    static void hearThePeopleSpeak(RobotController rc) throws GameActionException {
        Message[] messages = rc.readMessages(rc.getRoundNum()-1);
        for(Message m : messages) {
            Info info = Communicator.parse(m.getBytes());
            if (info.addition) {
                //if (!enemies.contains(loc)) queue.add(loc);
                if (info.loc.equals(rc.getLocation())) continue;
                enemies.add(info.loc);
            }
            else {
                enemies.remove(info.loc);
            }
            if (!blacklist.contains(m.getBytes())) {
                blacklist.add(m.getBytes());
                popTime.add(rc.getRoundNum());
                queue.add(m.getBytes());
            }

            donotsend.add(m.getSenderID(), m.getBytes());
        }
    }

    static FastLocSet donotsend = new FastLocSet();
    static FastIntSet blacklist = new FastIntSet();
    static FastIntSet popTime = new FastIntSet();
    static void cleanup(RobotController rc) {
        while (popTime.size > 0) {
            if (rc.getRoundNum() - popTime.peek() > 30) {
                popTime.pop();
                blacklist.pop();
            }
            else break;
        }
    }

    // TODO: only one potential problem... donotsend entries are never evicted... oh well
    static void letThePeopleKnow(RobotController rc) throws GameActionException {
        int i = 0;
        Info deletion = new Info();
        Info addition = new Info();
        addition.loc = enemies.closest(rc.getLocation());
        deletion.addition = false;
        if (queue.size() > 0) {
            Info info = Communicator.parse(queue.peek());
            if (!info.addition) {
                deletion.loc = info.loc;
            }
        }
        while (i++ < 20 && queue.size > 0) {
            rc.broadcastMessage(queue.pop());
        }

        if (addition.loc == null && deletion.loc == null) return;
        int d = Communicator.serialize(deletion);
        int a = Communicator.serialize(addition);
        RobotInfo[] allies = rc.senseNearbyRobots(-1, rc.getTeam());
        for (RobotInfo r : allies) {
            if (i >= 20) return;
            if (!rc.canSendMessage(r.getLocation())) continue;
            do {
                if (donotsend.contains(r.ID, d)) break;
                if (deletion.loc == null) break;
                rc.sendMessage(r.getLocation(), d);
                donotsend.add(rc.getID(), d);
                i++;
            } while(false);
            if (i>=20) return;
            if (donotsend.contains(r.ID, a)) continue;
            if (addition.loc == null) continue;
            rc.sendMessage(r.getLocation(), a);
            donotsend.add(rc.getID(), a);
            i++;
        }
    }
}
