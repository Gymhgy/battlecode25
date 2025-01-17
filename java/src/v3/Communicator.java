package v3;

import battlecode.common.*;
import v3.fast.*;

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
    static void updatePaintTowers(RobotController rc) throws GameActionException {
        RobotInfo[] nearby = rc.senseNearbyRobots(-1, rc.getTeam());
        for (int i = nearby.length; i-->0; ) {
            RobotInfo r = nearby[i];
            if (Util.isPaintTower(r.getType())){
                paintTowers.add(r.getLocation());
            }
        }
    }

    static FastLocSet enemyTowers = new FastLocSet();
    static void updateEnemyTowers(RobotController rc) throws GameActionException {
        RobotInfo[] nearby = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
        for (int i = nearby.length; i-->0; ) {
            RobotInfo r = nearby[i];
            if (!enemyTowers.contains(r.getLocation()))
                queue.add(r.getLocation());
            enemyTowers.add(r.getLocation());
        }
        for (Message m : rc.readMessages(rc.getRoundNum()-1)) {
            MapLocation loc = int2loc(m.getBytes());
            donotsend.add(m.getSenderID(), m.getBytes());
            if (!enemyTowers.contains(loc)) {
                enemyTowers.add(loc);
                queue.add(loc);
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
