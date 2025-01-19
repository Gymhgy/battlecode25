package v5pathfind;

import battlecode.common.*;
import v5pathfind.fast.FastLocSet;

public class Communicator {

    static int loc2int(MapLocation loc) {
        return loc.x * 60 + loc.y;
    }

    static MapLocation int2loc(int n) {
        return new MapLocation(n / 60, n % 60);
    }


    // Bottom 12 bits are used for location
    // 0000 0000 0000 0000 0000 0000 0000 0000
    // _                        ______________
    // Enemy/Ally bit             MapLocation

    static FastLocSet paintTowers = new FastLocSet();
    static FastLocSet enemyTowers = new FastLocSet();
    static FastLocSet allies = new FastLocSet();
    static void update(RobotController rc) throws GameActionException {
        MapLocation[] ruins = rc.senseNearbyRuins(-1);
        for (Message m : rc.readMessages(rc.getRoundNum()-1)) {
            MapLocation loc = int2loc(m.getBytes());
            donotsend.add(m.getSenderID(), m.getBytes());
            if (allies.contains(loc)) continue;
            if (!enemyTowers.contains(loc)) {
                enemyTowers.add(loc);
                queue.add(loc);
            }
        }
        for (int i = ruins.length; i-->0; ) {
            RobotInfo r = rc.senseRobotAtLocation(ruins[i]);
            if (r == null) {
                enemyTowers.remove(ruins[i]);
                queue.remove(ruins[i]);
                allies.remove(ruins[i]);
                paintTowers.remove(ruins[i]);
            }
            else if (rc.getTeam().equals(r.getTeam())) {
                if (Util.isPaintTower(r.getType())){
                    paintTowers.add(ruins[i]);
                }
                enemyTowers.remove(ruins[i]);
                queue.remove(ruins[i]);
                allies.add(ruins[i]);
                /*
                if (rc.getType() == UnitType.SPLASHER) {
                    rc.setIndicatorLine(rc.getLocation(), ruins[i], 0, 0, 255);
                    if (Splasher.target != null && enemyTowers.contains((Splasher.target))) {
                        rc.setIndicatorDot(new MapLocation(0,0), 255, 255, 255);
                    }
                }
                */
            }
            else {
                if (!enemyTowers.contains(ruins[i]))
                    queue.add(ruins[i]);
                enemyTowers.add(ruins[i]);
            }
        }

    }

    // Bottom 12 bits are used for location
    // 0000 0000 0000 0000 0000 0000 0000 0000
    // _                        ______________
    // Enemy/Ally bit             MapLocation


    static FastLocSet queue = new FastLocSet();
    static FastLocSet donotsend = new FastLocSet();
    static void relayEnemyTower(RobotController rc) throws GameActionException {
        MapLocation enemy = queue.pop();
        if (enemy == null) return;
        RobotInfo[] nearby = rc.senseNearbyRobots(-1, rc.getTeam());
        RobotInfo toSend = null;
        int canSends = 0;
        for (int i = nearby.length; i-->0; ) {
            RobotInfo r = nearby[i];
            if (r.getType().isTowerType()) {
                if (rc.canSendMessage(r.getLocation())) {
                    canSends++;
                    if(donotsend.contains(r.ID, loc2int(enemy))) continue;
                    if (Util.isPaintTower(r.getType())) {
                        rc.sendMessage(r.getLocation(), loc2int(enemy));
                        donotsend.add(r.ID, loc2int(enemy));
                        return;
                    }
                    else toSend = r;
                }
            }
        }
        if (toSend != null) {
            donotsend.add(toSend.ID, loc2int(enemy));
            rc.sendMessage(toSend.getLocation(), loc2int(enemy));
        }
        //else if (canSends > 0) relayEnemyTower(rc);
    }
}
