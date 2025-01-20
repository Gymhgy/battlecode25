package v7rf;

import battlecode.common.*;
import v7rf.fast.FastIntSet;
import v7rf.fast.FastLocSet;

public class Communicator {

    static Info parse(int n) {
        Info info = new Info();
        if ((n >> 31) != 0) info.addition = false;
        info.loc = int2loc(n & 0b1111_1111_1111);
        return info;
    }
    // 0000 0000 0000 0000 0000 0000 0000 0000
    // _
    // Addition/Subtraction?
    static int serialize(Info info) { // Q: What does serialize do
        int ret = 0;
        if (!info.addition) ret |= (1 << 31);
        ret |= loc2int(info.loc);
        return ret;
    }
    static int serialize(boolean addition, MapLocation loc) {
        int ret = 0;
        if (!addition) ret |= (1 << 31);
        ret |= loc2int(loc);
        return ret;
    }

    static int loc2int(MapLocation loc) {
        if (loc == null) return 0;
        return loc.x * 60 + loc.y;
    }

    static MapLocation int2loc(int n) {
        return new MapLocation(n / 60, n % 60);
    }


    // Bottom 12 bits are used for location
    // 0000 0000 0000 0000 0000 0000 0000 0000
    // _                        ______________
    // Enemy/Ally bit             MapLocation

    static MapLocation[] ruins;
    static FastLocSet paintTowers = new FastLocSet();
    static FastLocSet enemyTowers = new FastLocSet();
    static FastLocSet allies = new FastLocSet();
    static void update(RobotController rc) throws GameActionException {
        ruins = rc.senseNearbyRuins(-1);

        for (Message m : rc.readMessages(rc.getRoundNum()-1)) {
            Info info = parse(m.getBytes());
            RobotPlayer.indicator += "<" + info +">";
            if (info.addition) {
                if (allies.contains(info.loc)) continue;
                if (!enemyTowers.contains(info.loc)) {
                    enemyTowers.add(info.loc);
                    queue.remove(m.getBytes());
                }
            } else {
                enemyTowers.remove(info.loc);
            }
            // TODO: maybe add something here allowing bots to relay... but keep track of sender tower loc
            // and then when relaying..., don't relay if the other tower is within 80 sq dist
            donotsend.add(m.getSenderID(), m.getBytes());
        }

        for (int i = ruins.length; i-->0; ) {
            RobotInfo r = rc.senseRobotAtLocation(ruins[i]);
            if (r == null) { // i.e, tower has died
                if (enemyTowers.contains(ruins[i])) {
                    queue.add(serialize(false, ruins[i])); // What does queue and popTime kepp track of?
                    popTime.add(rc.getRoundNum()); // Round Number?
                }
                enemyTowers.remove(ruins[i]);  // If found/ else do nothing
                allies.remove(ruins[i]); // So you can fuck around with allies and PaintTowerstoo
                paintTowers.remove(ruins[i]);
            }
            else if (rc.getTeam().equals(r.getTeam())) {
                if (Util.isPaintTower(r.getType())){
                    paintTowers.add(ruins[i]); // this
                }
                if (enemyTowers.contains(ruins[i])) {
                    queue.add(serialize(false, ruins[i])); // Again, what do we need queuefo
                    popTime.add(rc.getRoundNum());
                }
                enemyTowers.remove(ruins[i]);
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
                if (!enemyTowers.contains(ruins[i])) {
                    queue.add(serialize(true, ruins[i]));
                    popTime.add(rc.getRoundNum());
                }
                paintTowers.remove(ruins[i]);
                enemyTowers.add(ruins[i]);
            }
        }
        for(int i = 0; i < queue.size; i++) {
            RobotPlayer.indicator += parse(queue.at(i)) + "|";
        }
    }

    // Bottom 12 bits are used for location
    // 0000 0000 0000 0000 0000 0000 0000 0000
    // _                        ______________
    // Enemy/Ally bit             MapLocation


    static FastIntSet queue = new FastIntSet();
    static FastIntSet popTime = new FastIntSet();
    static FastLocSet donotsend = new FastLocSet();
    static void relayEnemyTower(RobotController rc) throws GameActionException {

        // do a little bit of cleanup
        while (popTime.size > 0) {
            if (rc.getRoundNum() - popTime.peek() > 50) {
                popTime.pop();
                queue.pop();
            }
            else break;
        }

        RobotInfo[] towers = new RobotInfo[ruins.length];
        int j = 0;
        for (int i = ruins.length; i-->0;) {
            RobotInfo ri = rc.senseRobotAtLocation(ruins[i]);
            if (ri != null && ri.getTeam() == rc.getTeam()) towers[j++] = ri;
        }
        for (int i = 0; i < queue.size; i++) {
            Info info = parse(queue.at(i));
            for (int k = 0; k < j; k++) {
                RobotInfo ri = towers[k];
                if (!ri.getType().isTowerType())  {
                    System.out.println("yo wtf"); // yo wtf
                    continue;
                }
                int msg = serialize(info);
                if(donotsend.contains(ri.ID, msg)) continue;
                if (!rc.canSendMessage(ri.getLocation())) continue;
                rc.sendMessage(ri.getLocation(), msg);
                donotsend.add(ri.ID, msg);
                return;
            }
        }

    }
}

class Info {
    boolean addition = true;
    MapLocation loc;

    public String toString() {
        return (addition ? "add " : "del ") + loc;
    }
}
