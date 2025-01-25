package v8o3;

import battlecode.common.*;

public class Pathfinding {
    private static MapLocation target = null;
    private static MapLocation stayawayFrom = null;
    public static int stuckCnt;
    private static RobotInfo[] allies;
    private static RobotInfo[] nearbyFriends;
    private static RobotInfo[] enemies;
    static Direction[] MOVEABLE_DIRECTIONS = {
            Direction.NORTH,
            Direction.NORTHEAST,
            Direction.EAST,
            Direction.SOUTHEAST,
            Direction.SOUTH,
            Direction.SOUTHWEST,
            Direction.WEST,
            Direction.NORTHWEST
    };
    static RobotController rc;
    static void init(RobotController rc) {
        Pathfinding.rc = rc;
    }

    static void randomMove() throws GameActionException {
        int starting_i = FastMath.rand256() % 8;
        for (int i = starting_i; i < starting_i + 8; i++) {
            Direction dir = MOVEABLE_DIRECTIONS[i % 8];
            if (rc.canMove(dir)) rc.move(dir);
        }
    }

    static void tryMoveDir(Direction dir) throws GameActionException {
        if (rc.isMovementReady() && dir != Direction.CENTER) {
            if (rc.canMove(dir)) {
                rc.move(dir);
            } else if (rc.canMove(dir.rotateRight())) {
                rc.move(dir.rotateRight());
            } else if (rc.canMove(dir.rotateLeft())) {
                rc.move(dir.rotateLeft());
            } else {
                randomMove();
            }
        }
    }
    public static void tryMove(Direction dir) throws GameActionException {
        if (dir == Direction.CENTER)
            return;
        if (rc.canMove(dir)) {
            rc.move(dir);
        }
    }
    static public void moveToward(RobotController rc, MapLocation loc) throws GameActionException {
        if (!rc.isMovementReady() || loc == null)
            return;
        target = loc;
        stayawayFrom = null;
        allies = rc.senseNearbyRobots(-1, rc.getTeam());
        nearbyFriends = rc.senseNearbyRobots(2, rc.getTeam());
        enemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
        Direction dir = BugNav.getMoveDir();
        if (dir == null)
            return;
        tryMove(dir);
    }

    static class BugNav {
        static DirectionStack dirStack = new DirectionStack();
        static MapLocation prevTarget = null; // previous target
        private static FastLocSet visistedLocs = new FastLocSet();
        static int currentTurnDir = 0;
        private static int stackDepthCutoff = 8;
        static final int MAX_DEPTH = 20;
        static final int BYTECODE_CUTOFF = 6000;
        private static int lastMoveRound = -1;

        static Direction turn(Direction dir) {
            return currentTurnDir == 0 ? dir.rotateLeft() : dir.rotateRight();
        }

        static Direction turn(Direction dir, int turnDir) {
            return turnDir == 0 ? dir.rotateLeft() : dir.rotateRight();
        }

        static Direction getMoveDir() throws GameActionException {
            if (rc.getRoundNum() == lastMoveRound) {
                return null;
            } else {
                lastMoveRound = rc.getRoundNum();
            }

            // different target? ==> previous data does not help!
            if (prevTarget == null || target.distanceSquaredTo(prevTarget) > 2) {
                resetPathfinding();
            }

            //Debug.printString(Debug.INFO, String.format("move%sst%dcnt%d", target, stuckCnt, dirStack.size));

            prevTarget = target;
            if (visistedLocs.contains(rc.getLocation())) {
                stuckCnt++;
            } else {
                stuckCnt = 0;
                visistedLocs.add(rc.getLocation());
            }
            if (dirStack.size == 0) {
                stackDepthCutoff = 8;
                Direction dir = rc.getLocation().directionTo(target);
                if (canMoveOrFill(dir)) {
                    return dir;
                }
                currentTurnDir = getTurnDir(dir);
                // obstacle encountered, rotate and add new dirs to stack
                while (!canMoveOrFill(dir) && dirStack.size < 8) {
                    if (!rc.onTheMap(rc.getLocation().add(dir))) {
                        currentTurnDir ^= 1;
                        dirStack.clear();
                        return null; // do not move
                    }
                    dirStack.push(dir);
                    dir = turn(dir);
                }
                if (dirStack.size != 8) {
                    return dir;
                }
            }
            else {
                // dxx
                // xo
                // x
                // suppose you are at o, x is wall, and d is another duck, you are pathing left and bugging up rn
                // and the duck moves away, you wanna take its spot
                if (dirStack.size > 1 && canMoveOrFill(dirStack.top(2))) {
                    dirStack.pop(2);
                } else if (dirStack.size == 1 && canMoveOrFill(turn(dirStack.top(), 1 - currentTurnDir))) {
                    /*
                    consider bugging down around x and turning left to the location above y,
                    the stack will contain a single direction that is down,
                    which is blocked by y, without this special case it will turn left above y and go up
                    w00w
                    wx0w
                    w0yw
                    w00w
                     */
                    Direction d = turn(dirStack.top(), 1 - currentTurnDir);
                    dirStack.pop();
                    return d;
                }
                while (dirStack.size > 0 && canMoveOrFill(dirStack.top())) {
                    dirStack.pop();
                }
                if (dirStack.size == 0) {
                    Direction dir = rc.getLocation().directionTo(target);
                    if (canMoveOrFill(dir)) {
                        return dir;
                    }

                    dirStack.push(dir);
                }
                // keep rotating and adding things to the stack
                Direction curDir;
                int stackSizeLimit = Math.min(DirectionStack.STACK_SIZE, dirStack.size + 8);
                while (dirStack.size > 0 && !canMoveOrFill(curDir = turn(dirStack.top()))) {
                    if (!rc.onTheMap(rc.getLocation().add(curDir))) {
                        currentTurnDir ^= 1;
                        dirStack.clear();
                        return null; // do not move
                    }
                    dirStack.push(curDir);
                    if (dirStack.size == stackSizeLimit) {
                        dirStack.clear();
                        return null;
                    }
                }
                if (dirStack.size >= stackDepthCutoff) {
                    int cutoff = stackDepthCutoff + 8;
                    //Debug.printString(Debug.PATHFINDING, "reset");
                    dirStack.clear();
                    stackDepthCutoff = cutoff;
                }
                Direction moveDir = dirStack.size == 0 ? dirStack.dirs[0] : turn(dirStack.top());
                if (canMoveOrFill(moveDir)) {
                    return moveDir;
                }
            }
            return null;
        }

        static int simulate(int turnDir, Direction dir) throws GameActionException {
            int originalTurnDir = turnDir;
            MapLocation now = rc.getLocation();
            DirectionStack dirStack = new DirectionStack();
            while (!canPass(now, dir) && dirStack.size < 8) {
                dirStack.push(dir);
                dir = turn(dir, turnDir);
            }
            now = now.add(dir);
            int ans = 1;

            while (!now.isAdjacentTo(target)) {
                if (ans > MAX_DEPTH || Clock.getBytecodesLeft() < BYTECODE_CUTOFF) {
                    break;
                }
                //Debug.setIndicatorDot(Debug.PATHFINDING, now, originalTurnDir == 0? 255 : 0, 0, originalTurnDir == 0? 0 : 255);
                Direction moveDir = now.directionTo(target);
                if (dirStack.size == 0) {
                    if (!canPass(now, moveDir)) {
                        Direction dirL = moveDir.rotateLeft();
                        MapLocation locL = now.add(dirL);
                        Direction dirR = moveDir.rotateRight();
                        MapLocation locR = now.add(dirR);
                        if (target.distanceSquaredTo(locL) <= target.distanceSquaredTo(locR)) {
                            if (canPass(now, dirL)) {
                                moveDir = dirL;
                            }  else {
                                while (!canPass(now, moveDir) && dirStack.size < 8) {
                                    dirStack.push(moveDir);
                                    moveDir = turn(moveDir, 0);
                                }
                                turnDir = 0;
                            }
                        } else {
                            if (canPass(now, dirR)) {
                                moveDir = dirR;
                            }  else {
                                while (!canPass(now, moveDir) && dirStack.size < 8) {
                                    dirStack.push(moveDir);
                                    moveDir = turn(moveDir, 1);
                                }
                                turnDir = 1;
                            }
                        }
                    }
                } else {
                    if (dirStack.size > 1 && canPass(now, dirStack.top(2))) {
                        dirStack.pop(2);
                    } else if (dirStack.size == 1 && canPass(now, turn(dirStack.top(), 1 - turnDir))) {
                        moveDir = turn(dirStack.top(), 1 - turnDir);
                        dirStack.pop();
                    }
                    while (dirStack.size > 0 && canPass(now, dirStack.top())) {
                        dirStack.pop();
                    }

                    if (dirStack.size == 0) {
                        if (!canPass(now, moveDir)) {
                            Direction dirL = moveDir.rotateLeft();
                            MapLocation locL = now.add(dirL);
                            Direction dirR = moveDir.rotateRight();
                            MapLocation locR = now.add(dirR);
                            if (target.distanceSquaredTo(locL) <= target.distanceSquaredTo(locR)) {
                                if (canPass(now, dirL)) {
                                    moveDir = dirL;
                                } else if (canPass(now, dirR)) {
                                    moveDir = dirR;
                                }
                            } else {
                                if (canPass(now, dirR)) {
                                    moveDir = dirR;
                                }  else if (canPass(now, dirL)) {
                                    moveDir = dirL;
                                }
                            }
                        }
                        if (!canPass(now, moveDir)) {
                            dirStack.push(moveDir);
                        }
                    }
                    while (dirStack.size > 0 && !canPass(now, turn(dirStack.top(), turnDir))) {
                        dirStack.push(turn(dirStack.top(), turnDir));
                        if (dirStack.size > 8) {
                            return -1;
                        }
                    }
                    moveDir = dirStack.size == 0 ? dirStack.dirs[0] : turn(dirStack.top(), turnDir);
                }
                now = now.add(moveDir);
                ans++;
            }

            return ans + Util.distance(now, target);
        }

        static int getTurnDir(Direction dir) throws GameActionException {
            //Debug.bytecodeDebug += "  turnDir=" + Clock.getBytecodeNum();
            int ansL = simulate(0, dir);
            int ansR = simulate(1, dir);
            //Debug.bytecodeDebug += "  turnDir=" + Clock.getBytecodeNum();
            //Debug.printString(Debug.PATHFINDING, String.format("t%d|%d", ansL, ansR));
            if (ansL == ansR) return FastMath.rand256() % 2;
            if ((ansL <= ansR && ansL != -1) || ansR == -1) {
                return 0;
            } else {
                return 1;
            }
        }

        // clear some of the previous data
        static void resetPathfinding() {
            stackDepthCutoff = 8;
            dirStack.clear();
            stuckCnt = 0;
            visistedLocs.clear();
        }

        static boolean canMoveOrFill(Direction dir) throws GameActionException {
            MapLocation loc = rc.getLocation().add(dir);
            if (stayawayFrom != null && loc.isAdjacentTo(stayawayFrom))
                return false;
            if (canPass(rc.getLocation(), dir) /*rc.canMove(dir)*/) {
                // it is not ok to move onto a tile to block a teammate's movement
                for (int i = nearbyFriends.length; --i >= 0;) {
                    RobotInfo ally = nearbyFriends[i];
                    if (!ally.location.isAdjacentTo(loc))
                        continue;
                    boolean ok = false;
                    for (Direction d: MOVEABLE_DIRECTIONS) {
                        MapLocation a = ally.location.add(d);
                        if (rc.canSenseLocation(a) && rc.sensePassability(a) && (rc.senseRobotAtLocation(a) == null || a.equals(rc.getLocation())) && !loc.equals(a)) {
                            ok = true;
                            break;
                        }
                    }
                    if (!ok) {
                        //Debug.setIndicatorDot(Debug.MICRO, rc.getLocation().add(dir), 255, 0, 0);
                        return false;
                    }
                }
                return true;
            }
            if (!rc.canSenseLocation(loc))
                return false;

            MapInfo info = rc.senseMapInfo(loc);

            if (rc.senseRobotAtLocation(loc) != null) {
                return FastMath.rand256() % 10 == 0; // unstucking rng
            }
            return false;
        }

        static boolean canPass(MapLocation loc, Direction targetDir) throws GameActionException {
            MapLocation newLoc = loc.add(targetDir);
            if (!rc.onTheMap(newLoc))
                return false;
            if (rc.canSenseLocation(newLoc)) {
                if (newLoc.equals(rc.getLocation())) return true;
                if (rc.isLocationOccupied(newLoc)) return false;
                MapInfo mi = rc.senseMapInfo(newLoc);

                if (mi.isWall()) return false;
                if (mi.hasRuin()) return false;
                if (rc.getType() == UnitType.SOLDIER && rc.getPaint() < 25) return mi.getPaint().isAlly();
                if (rc.getType() == UnitType.SPLASHER && rc.getPaint() < 25) return mi.getPaint().isAlly();

                if (rc.getType() == UnitType.MOPPER && mi.getPaint().isEnemy()) return false;
                if (rc.getType() == UnitType.MOPPER || rc.getType() == UnitType.SOLDIER) {
                    for (RobotInfo r : enemies) {
                        if (r.getLocation().isWithinDistanceSquared(newLoc, 9) && r.getType().isTowerType()) {
                            return false;
                        }
                    }
                }
                /*int adj = 0;
                for (Direction d : Direction.allDirections()) {
                    if(!rc.canSenseLocation(loc.add(d))) continue;
                    RobotInfo r = rc.senseRobotAtLocation(loc.add(d));
                    if (r != null && r.getTeam() == rc.getTeam() && !r.getType().isTowerType()) adj++;
                }
                if (adj >= 4) return FastMath.rand256() % adj < 2;*/
                return rc.sensePassability(newLoc);
            }
            return true;
        }
    }
}

class DirectionStack {
    static int STACK_SIZE = 60;
    int size = 0;
    Direction[] dirs = new Direction[STACK_SIZE];

    final void clear() {
        size = 0;
    }

    final void push(Direction d) {
        dirs[size++] = d;
    }

    final Direction top() {
        return dirs[size - 1];
    }

    /**
     * Returns the top n element of the stack
     * @param n
     * @return
     */
    final Direction top(int n) {
        return dirs[size - n];
    }

    final void pop() {
        size--;
    }

    final void pop(int n) {
        size -= n;
    }
}