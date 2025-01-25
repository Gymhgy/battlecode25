package v8o3;

import battlecode.common.*;

public class Refill {

    static int minPaint;
    static void init(int minPaint) {
        Refill.minPaint = minPaint;
    }

    static boolean refilling = false;
    static MapLocation closestTower = null;
    static int lastRefill = 0;
    static boolean refill(RobotController rc) throws GameActionException {
        if ((rc.getPaint() < minPaint && rc.getRoundNum() - lastRefill > 25) || refilling) {
            closestTower = Refill.closestRefillTower(rc, Communicator.paintTowers);
            refilling = Refill.refillPaint(rc, closestTower);
        }
        return refilling;
    }

    static int getEmptyPaintAmount(RobotController rc) throws GameActionException {
        return (rc.getType().paintCapacity - rc.getPaint());
    }

    static MapLocation closestRefillTower(RobotController rc, FastLocSet towers) throws GameActionException {
        RobotInfo[] nearby = rc.senseNearbyRobots(-1, rc.getTeam());
        for (int i = nearby.length; i-->0; ) {
            if (Util.isMoneyTower(nearby[i].getType())) {
                if (closestTower == nearby[i].getLocation()) closestTower = null;
                if (nearby[i].getPaintAmount() > 0) {
                    return nearby[i].getLocation();
                }
            }
        }
        if (closestTower != null) return closestTower;
        // TODO: if you see it and its super crowded, try something else.
        //if (FastMath.rand256() % 3 == 0)
        //    return towers.secondClosest(rc.getLocation());
        return towers.closest(rc.getLocation());
    }

    static boolean refillPaint(RobotController rc, MapLocation loc) throws GameActionException {
        int amount = -getEmptyPaintAmount(rc);
        if (loc == null) return false;
        if (rc.canSenseRobotAtLocation(loc)) {
            RobotInfo ri = rc.senseRobotAtLocation(loc);
            UnitType a = ri.getType();
            int amt = -Math.min(25, ri.getPaintAmount());
            if (rc.getPaint() < 3 && rc.canTransferPaint(loc, amt)) {
                rc.transferPaint(loc, amt);
                lastRefill = rc.getRoundNum();
                return false;
            }
            else if (Util.isPaintTower(a)) {

                /*if (rc.getChips() > 1300 && ri.getPaintAmount() + amount < 100) {
                    return true;
                }*/
                if (!rc.getLocation().isAdjacentTo(loc)) {
                    int crowd = 0;
                    for (Direction d : RobotPlayer.directions) {
                        MapLocation wait = loc.add(d);
                        if (rc.canSenseLocation(wait) && rc.isLocationOccupied(wait)) {
                            crowd++;
                        }
                    }
                    if (crowd >= 4) return true;
                }
                amount = Math.max(-ri.getPaintAmount(), amount);
                if (rc.canTransferPaint(loc, amount)) {
                    rc.transferPaint(loc, amount);
                    lastRefill = rc.getRoundNum();
                    return false;
                } else {
                    /*if (rc.getLocation().isAdjacentTo(loc)) {
                        for (Direction d : RobotPlayer.directions) {
                            MapLocation wait = rc.getLocation().add(d);
                            if (rc.canSenseLocation(wait) && rc.senseMapInfo(wait).getPaint().isAlly() && rc.canMove(d)) {
                                rc.move(d);
                                return true;
                            }
                        }
                    }*/
                    //if (rc.getPaint() >= 5 && rc.getLocation().)

                }
            }
            else if (Util.isMoneyTower(a)){
                amount = Math.max(-ri.getPaintAmount(), amount);
                if (rc.canTransferPaint(loc, amount)) {
                    rc.transferPaint(loc, amount);
                    lastRefill = rc.getRoundNum();
                    return false;
                }
                else {

                }
            }
        }
        /*if (rc.getPaint() > minPaint) {
            //return false;
        }*/
         {
            Pathfinding.moveToward(rc, loc);
        }
        return true;
    }


}
