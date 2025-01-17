package v3;

import battlecode.common.*;
import v3.fast.FastLocSet;
import v3.fast.FastMath;

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
    static void init(RobotController rc) throws GameActionException {
        if (rc.getRoundNum() == 1) {
            og = true;
        }
    }


    static void run(RobotController rc) throws GameActionException {
        hearThePeopleSpeak(rc);
        RobotInfo[] allies = rc.senseNearbyRobots(-1, rc.getTeam());

        if (rc.getChips() > 2000 && rc.getRoundNum() > 50 && Util.isPaintTower(rc.getType())) {
            if(rc.canUpgradeTower(rc.getLocation())) {
                rc.upgradeTower(rc.getLocation());
            }
        }

        if (rc.getChips() > 1300 || (rc.getRoundNum() > 100 && rc.getRoundNum()%3 == 0)) {
            Direction dir = directions[FastMath.rand256() % 8];
            MapLocation nextLoc = rc.getLocation().add(dir);

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

            if (rc.canBuildRobot(type, nextLoc)) {
                rc.buildRobot(type, nextLoc);
            }
        }

        attackNearby(rc);
        letThePeopleKnow(rc);
    }



    static void attackNearby(RobotController rc) throws GameActionException {
        RobotInfo[] enemies = rc.senseNearbyRobots(rc.getType().actionRadiusSquared, rc.getTeam().opponent());
        // TODO: check if AOE can do more damage

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
            enemies.add(Communicator.int2loc(m.getBytes()));
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
