package v7rf;

import battlecode.common.*;
import v7rf.fast.FastLocSet;

public class Refill {

    static int minPaint;
    static void init(int minPaint) {
        Refill.minPaint = minPaint;
    }

    static boolean refilling = false;
    static MapLocation closestTower = null;

    static boolean refill(RobotController rc) throws GameActionException {
        if (rc.getPaint() < minPaint || refilling) {
            if (!refilling || closestTower == null)
                closestTower = Refill.closestRefillTower(rc, Communicator.paintTowers);
            refilling = Refill.refillPaint(rc, closestTower);
        }
        return refilling;
    }

    static int getEmptyPaintAmount(RobotController rc) throws GameActionException {
        return (rc.getType().paintCapacity - rc.getPaint()) / 50 * 50;
    }

    static MapLocation closestRefillTower(RobotController rc, FastLocSet towers) throws GameActionException {
        RobotInfo[] nearby = rc.senseNearbyRobots(-1, rc.getTeam());
        for (int i = nearby.length; i-->0; ) {
            if (Util.isMoneyTower(nearby[i].getType())) {
                if (nearby[i].getPaintAmount() > getEmptyPaintAmount(rc)) {
                    return nearby[i].getLocation();
                }
            }
        }
        // TODO: if you see it and its super crowded, try something else.
        return towers.closest(rc.getLocation());
    }

    static boolean refillPaint(RobotController rc, MapLocation loc) throws GameActionException {
        int amount = -getEmptyPaintAmount(rc);
        if (loc == null) return false;
        if (rc.canSenseRobotAtLocation(loc)) { // So many checks:
            RobotInfo ri = rc.senseRobotAtLocation(loc);
            UnitType a = ri.getType();
            if (Util.isPaintTower(a)) {

                if (rc.getChips() > 1300 && ri.getPaintAmount() + amount < 100) {
                    return true;
                }
                if (rc.canTransferPaint(loc, amount)) {
                    rc.transferPaint(loc, amount);
                    return false;
                } else {
                    int amt = -Math.min(10, ri.getPaintAmount());
                    if (rc.getPaint() == 0 && rc.canTransferPaint(loc, amt)) {
                        rc.transferPaint(loc, amt);
                    }
                }
            }
            else if (Util.isMoneyTower(a)){
                if (rc.canTransferPaint(loc, amount)) {
                    rc.transferPaint(loc, amount);
                    return false;
                }
                else {
                    int amt = -Math.min(10, ri.getPaintAmount());
                    if (rc.getPaint() == 0 && rc.canTransferPaint(loc, amt)) {
                        rc.transferPaint(loc, amt);
                    }
                }
                if (ri.getPaintAmount() < -amount - 30) {
                    return false;
                }
            }
        }
        if (rc.getPaint() > minPaint) {
            return false;
        }
        else {
            Pathfinding.moveToward(rc, loc);
        }
        return true;
    }


}
