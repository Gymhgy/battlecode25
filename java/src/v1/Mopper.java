package v1;

import battlecode.common.*;
import v1.fast.FastLocSet;

public class Mopper {

    private static FastLocSet allyPaintTowers = new FastLocSet();
    private static FastLocSet allyMoneyTowers = new FastLocSet();
    private static MapLocation paintTower = null;
    private static MapLocation moneyTower = null;
    private static MapLocation target = null;


    static void init(RobotController rc) {
        var messages = rc.readMessages(-1);
    }

    static void run(RobotController rc) throws GameActionException {
        if (rc.isActionReady()) {
            performAttack(rc);
        }

        if (paintTower != null && moneyTower != null) {
            runRoute(rc);
        } else {
            wander(rc);
        }



        if (rc.isActionReady()) {
            performAttack(rc);
        }
    }

    static void wander(RobotController rc) throws GameActionException {
        RobotInfo[] allies = rc.senseNearbyRobots(-1, rc.getTeam());
        for (RobotInfo ally : allies) {
            if (target == null) {
                if (Util.isMoneyTower(ally.getType())) {
                    if (ally.getPaintAmount() < 300) {
                        target = ally.getLocation();
                        break;
                    }
                }
                 else if (ally.getType() == UnitType.SOLDIER) {
                    if (ally.getPaintAmount() < 100) {
                        target = ally.getLocation();
                        break;
                    }
                }
            }
            if (paintTower == null && Util.isPaintTower(ally.getType())) {
                paintTower = ally.getLocation();
            }
        }

        // If towers not found, wander randomly
        if (paintTower == null || moneyTower == null) {
            Pathfinding.navigateRandomly(rc);
        }
        else {
            runRoute(rc);
        }
    }

    static void runRoute(RobotController rc) throws GameActionException {
        if (rc.canSenseLocation(paintTower)) {
            if (rc.senseRobotAtLocation(paintTower) == null) {
                wander(rc);
                return;
            }
        }
        /*if (rc.canSenseLocation(moneyTower)) {
            if (rc.senseRobotAtLocation(moneyTower) == null) {
                wander(rc);
                return;
            }
        }*/

        if (target == null) {
            target = rc.getPaint() > 60 ? target : paintTower;
        }

        if (rc.getLocation().isAdjacentTo(target)) {
            RobotInfo tower = rc.senseRobotAtLocation(target);
            if (target.equals(paintTower)) {
                int amount = UnitType.MOPPER.paintCapacity - rc.getPaint();
                if (tower.getPaintAmount() >= (1 - (double)rc.getRoundNum() / 2000) * 300) {
                    if (rc.canTransferPaint(target, -amount)) {
                        rc.transferPaint(target, -amount);
                        target = null;
                    }
                }
            } else {
               if (rc.getPaint() >= 60) {
                   if (rc.canTransferPaint(target, rc.getPaint() - 60)) {
                       rc.transferPaint(target, rc.getPaint() - 60);
                       target = paintTower;
                   }
               }
            }
        }
        if (target == null) {
            Pathfinding.navigateRandomly(rc);
        }
        else {
            Pathfinding.moveToward(rc, target);
        }
    }

    static void performAttack(RobotController rc) throws GameActionException {
        RobotInfo[] enemies = rc.senseNearbyRobots(2, rc.getTeam().opponent());
        RobotInfo[] allies = rc.senseNearbyRobots(2, rc.getTeam());

        if (rc.getPaint() > 60) {
            for (RobotInfo ally : allies) {
                if (ally.getType() == UnitType.SOLDIER) {
                    if (ally.getPaintAmount() < 150) {
                        if (rc.canTransferPaint(ally.getLocation(), 100 - rc.getPaint())) {
                            rc.transferPaint(ally.getLocation(), 100 - rc.getPaint());
                            return;
                        }
                    }
                }
            }
        }

        for (RobotInfo enemy : enemies) {
            if (rc.senseMapInfo(enemy.getLocation()).getPaint().isEnemy()) {
                rc.attack(enemy.getLocation());
                return;
            }
        }

        // TODO: Mop in the direction that hits the most enemy bots
        int maxHits = 0;
        Direction bestDirection = null;
        for (RobotInfo enemy : enemies) {
            //...
        }

        // Attack any tile with enemy paint on it
        for (MapInfo loc : rc.senseNearbyMapInfos(2)) {
            if (loc.getPaint().isEnemy()) {
                rc.attack(loc.getMapLocation());
                return;
            }
        }
    }
}
