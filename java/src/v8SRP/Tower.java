package v8SRP;

import battlecode.common.*;
import v8SRP.fast.FastIntSet;
import v8SRP.fast.FastLocSet;
import v8SRP.fast.FastMath;

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
    static RobotInfo[] nearbyAllies;
    static MapLocation center;
    static void init(RobotController rc) throws GameActionException {
        if (rc.getRoundNum() == 1) {
            og = true;
        }
        center = new MapLocation(rc.getMapWidth()/2, rc.getMapHeight()/2);
        spawners = rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 4);
    }

    static RobotInfo[] nearbyEnemies;
    static void run(RobotController rc) throws GameActionException {
        hearThePeopleSpeak(rc);
        cleanup(rc);
        nearbyAllies = rc.senseNearbyRobots(-1, rc.getTeam());
        nearbyEnemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
        int moppers = 0;
        boolean soldier = false;
        for (RobotInfo r : nearbyAllies) {
            if (r.getType() == UnitType.MOPPER) moppers++;
        }
        for (RobotInfo r : nearbyEnemies) {
            if (r.getType() == UnitType.SOLDIER && moppers < 3) {
                soldier = true;
                break;
            }
        }
        MapLocation en = enemies.closest(rc.getLocation());

        rc.attack(null);
        MapInfo[] nearby = rc.senseNearbyMapInfos();
        if (rc.getChips() > 2000 && rc.getRoundNum() > 50 && Util.isPaintTower(rc.getType())) {
            if(rc.canUpgradeTower(rc.getLocation())) {
                rc.upgradeTower(rc.getLocation());
            }
        }

        if (rc.getChips() > 3000 && rc.getRoundNum() > 50 && Util.isMoneyTower(rc.getType())) {
            if(rc.canUpgradeTower(rc.getLocation())) {
                rc.upgradeTower(rc.getLocation());
            }
        }

        int enemyPaints = 0;
        for (MapInfo mi : nearby) {
            if (mi.getPaint().isEnemy()) enemyPaints++;
        }

        boolean canSpawn = true;
        if (Util.isPaintTower(rc.getType())) {
            if (rc.getRoundNum() < 20) canSpawn = true;
            else {
                int sum = 0;
                for (RobotInfo ally : nearbyAllies) {
                    boolean add = switch (ally.getType()) {
                        case MOPPER -> ally.getID() % 2 == 0 && ally.getPaintAmount() < 40;
                        case SOLDIER -> ally.getPaintAmount() < 80;
                        case SPLASHER -> ally.getPaintAmount() < 80;
                        default -> false;
                    };
                    if (add) {
                        sum += ally.getType().paintCapacity - ally.getPaintAmount();
                    }
                }
                if (rc.getPaint() < sum * 3.0 / 4) canSpawn = false;
            }
        }

        if ((soldier || canSpawn) && rc.getChips() > 1300) {


            UnitType type = UnitType.SOLDIER;
            /*if (rc.getRoundNum() == 1 && enemyPaints > 12) {
                type = UnitType.MOPPER;
            }*/
            /*if (og && rc.getRoundNum() == 2 && Util.isPaintTower(rc.getType())) {
                type = UnitType.SPLASHER;
            }
            else*/ if (og && rc.getRoundNum() < 15) {
                type = UnitType.SOLDIER;
            }
            else if (soldier) {
                rc.setIndicatorDot(rc.getLocation(), 255, 255, 0);
                type = UnitType.MOPPER;
            }
            else if (rc.getRoundNum() < 50) {
                var distr = new UnitType[] {UnitType.SOLDIER, UnitType.SOLDIER, UnitType.SOLDIER, UnitType.MOPPER, UnitType.MOPPER, UnitType.SPLASHER};
                type = distr[FastMath.rand256() % distr.length];
            }
            else {
                var distr = new UnitType[] {UnitType.SOLDIER, UnitType.SOLDIER, UnitType.MOPPER, UnitType.MOPPER, UnitType.SPLASHER, UnitType.SPLASHER,
                        UnitType.SOLDIER, UnitType.MOPPER, UnitType.SPLASHER, UnitType.SPLASHER};
                type = distr[FastMath.rand256() % distr.length];
            }

            MapLocation nextLoc = decideOnSpawn(rc, en, soldier, type);
            if (nextLoc != null)
                rc.setIndicatorDot(nextLoc,255, 0, 0);
            if (type == UnitType.SPLASHER && Util.isMoneyTower(rc.getType()))
                nextLoc = null;
            if (nextLoc != null && rc.canBuildRobot(type, nextLoc)) {
                rc.buildRobot(type, nextLoc);
                lastSpawn = nextLoc;
            }
        }

        attackNearby(rc);
        letThePeopleKnow(rc);
        RobotPlayer.indicator += "\n";
        if (en != null) {
            rc.setIndicatorLine(rc.getLocation(), en, 255, 255, 255);
            RobotPlayer.indicator += enemies.closest(rc.getLocation()).toString();
        }
        else RobotPlayer.indicator += enemies.size;

        rc.setIndicatorString(RobotPlayer.indicator);
    }

    static MapLocation lastSpawn = null;
    static MapLocation decideOnSpawn(RobotController rc, MapLocation enemy, boolean soldier, UnitType type) throws GameActionException {
        RobotPlayer.indicator+="\nspawning: ";
        RobotPlayer.indicator+=Clock.getBytecodeNum() + "|";

        if (!rc.isActionReady() || spawners.length == 0) return null;

        FastLocSet enemyLocs = new FastLocSet();
        MapLocation closest = null;
        if (soldier) {
            for (RobotInfo badGuy : nearbyEnemies) {
                if (badGuy.getType() == UnitType.SOLDIER) {
                    for (Direction dir : Direction.allDirections()) {
                        enemyLocs.add(badGuy.getLocation().add(dir));
                    }
                    if (closest == null || closest.distanceSquaredTo(rc.getLocation()) > badGuy.getLocation().distanceSquaredTo(rc.getLocation()))
                        closest = badGuy.getLocation();
                }
            }
        }

        FastLocSet adjacentLocs = new FastLocSet();

        for (RobotInfo ally : nearbyAllies) {
            for (Direction dir : Direction.allDirections()) {
                adjacentLocs.add(ally.getLocation().add(dir));
            }
        }

        MapLocation bestLocation = null;
        int bestScore = Integer.MIN_VALUE;

        for (MapLocation spawner : spawners) {
            RobotPlayer.indicator+=Clock.getBytecodeNum() + "|";
            if (!rc.canBuildRobot(type, spawner)) continue;

            int score = 0;

            for (Direction dir : directions) {
                if (rc.canSenseLocation(spawner.add(dir)) && rc.isLocationOccupied(spawner.add(dir))) {
                    score -= 1000;
                    break;
                }
            }

            if (soldier) {
                if (enemyLocs.contains(spawner)) score += 1000;
                if(closest != null)
                    score -= spawner.distanceSquaredTo(closest) * 2;
            }

            else if (enemy != null) {
                int dist = spawner.distanceSquaredTo(enemy);
                score -= dist;
                if (dist <= UnitType.LEVEL_THREE_PAINT_TOWER.actionRadiusSquared) score -= 10000;
            } else if (lastSpawn != null){
                score -= spawner.distanceSquaredTo(lastSpawn);
            }

            score += FastMath.rand256() % 10;
            if (score > bestScore) {
                bestScore = score;
                bestLocation = spawner;
            }
            if (soldier)
                System.out.println(spawner + " " + score);
        }
        RobotPlayer.indicator+=Clock.getBytecodeNum() + "|";
        return bestLocation;
    }

    static void attackNearby(RobotController rc) throws GameActionException {
        RobotInfo[] enemies = rc.senseNearbyRobots(rc.getType().actionRadiusSquared, rc.getTeam().opponent());

        RobotInfo target = null;

        // Prioritize by type and health: Soldiers first, then Splashers, then Moppers
        for (RobotInfo enemy : enemies) {
            if (enemy.getPaintAmount() == 0) continue;
            if (rc.canAttack(enemy.getLocation())) {
                if (target == null ||
                        (enemy.getType() == UnitType.SOLDIER && (target.getType() != UnitType.SOLDIER || enemy.getHealth() < target.getHealth())) ||
                        (enemy.getType() == UnitType.SPLASHER && target.getType() != UnitType.SOLDIER && (target.getType() != UnitType.SPLASHER || enemy.getHealth() < target.getHealth())) ||
                        (enemy.getType() == UnitType.MOPPER && target.getType() != UnitType.SOLDIER && target.getType() != UnitType.SPLASHER && enemy.getHealth() < target.getHealth())) {
                    target = enemy;
                }
            }
        }

        if (target != null) {
            if (rc.canAttack(target.getLocation()))
                rc.attack(target.getLocation());
        }
        else {
            for (RobotInfo enemy : enemies) {
                if (rc.canAttack(enemy.getLocation())) {
                    rc.attack(enemy.getLocation());
                    break;
                }
            }
        }
    }




    static FastLocSet enemies = new FastLocSet();
    static FastIntSet queue = new FastIntSet();
    static void hearThePeopleSpeak(RobotController rc) throws GameActionException {
        RobotPlayer.indicator+="\nhear the ppl: ";

        Message[] messages = rc.readMessages(rc.getRoundNum()-1);
        for(Message m : messages) {
            RobotPlayer.indicator += Clock.getBytecodeNum() +"|";
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
        RobotPlayer.indicator += "\ncleanup: ";
        RobotPlayer.indicator += Clock.getBytecodeNum() + "|";
        while (popTime.size > 0) {
            RobotPlayer.indicator += Clock.getBytecodeNum() + "|";
            if (rc.getRoundNum() - popTime.peek() > 30) {
                popTime.pop();
                blacklist.pop();
            }
            else break;
        }
    }

    // TODO: only one potential problem... donotsend entries are never evicted... oh well
    static void letThePeopleKnow(RobotController rc) throws GameActionException {
        RobotPlayer.indicator+="\nlet the ppl: ";
        RobotPlayer.indicator += Clock.getBytecodeNum() +"|";
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
        RobotPlayer.indicator += Clock.getBytecodeNum() +"|";

        if (addition.loc == null && deletion.loc == null) return;
        int d = Communicator.serialize(deletion);
        int a = Communicator.serialize(addition);
        RobotInfo[] allies = rc.senseNearbyRobots(-1, rc.getTeam());
        for (RobotInfo r : allies) {
            RobotPlayer.indicator += Clock.getBytecodeNum() +"|";
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
