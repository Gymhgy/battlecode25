package v4;

import battlecode.common.*;
import v4.fast.FastMath;

/**
 * This class contains logic / variable that is shared between all units
 * pathfinding logics will be here
 */
public class Pathfinding {

    static int turnsSinceRandomTargetChange = 0;
    static MapLocation target;
    public static boolean navigateRandomly(RobotController rc) throws GameActionException {
        turnsSinceRandomTargetChange++;
        if(target == null || rc.getLocation().distanceSquaredTo(target) < 5 ||
                turnsSinceRandomTargetChange > rc.getMapWidth() + rc.getMapHeight()) {
            int targetX = FastMath.rand256() % rc.getMapWidth();
            int targetY = FastMath.rand256() % rc.getMapHeight();
            target = new MapLocation(targetX, targetY);
            turnsSinceRandomTargetChange = 0;
        }
        moveToward(rc, target);
        if(rc.isMovementReady()) {
            int targetX = FastMath.rand256() % rc.getMapWidth();
            int targetY = FastMath.rand256() % rc.getMapHeight();
            target = new MapLocation(targetX, targetY);
            return false;
        }
        return true;
    }
    static void randomMove(RobotController rc) throws GameActionException {
        int starting_i = FastMath.rand256() % 8;
        for (int i = starting_i; i < starting_i + 8; i++) {
            Direction dir = Direction.allDirections()[i % 8];
            if (rc.canMove(dir)) rc.move(dir);
        }
    }

    static void tryMoveDir(RobotController rc, Direction dir) throws GameActionException {
        if (rc.isMovementReady() && dir != Direction.CENTER) {
            if (rc.canMove(dir) && canPass(rc, dir)) {
                rc.move(dir);
            } else if (rc.canMove(dir.rotateRight()) && canPass(rc, dir.rotateRight(), dir)) {
                rc.move(dir.rotateRight());
            } else if (rc.canMove(dir.rotateLeft()) && canPass(rc, dir.rotateLeft(), dir)) {
                rc.move(dir.rotateLeft());
            } else {
                randomMove(rc);
            }
        }
    }
    static void follow(RobotController rc, MapLocation location) throws GameActionException {
        tryMoveDir(rc, rc.getLocation().directionTo(location));
    }

    static int getClosestID(MapLocation fromLocation, MapLocation[] locations) {
        int dis = Integer.MAX_VALUE;
        int rv = -1;
        for (int i = locations.length; --i >= 0;) {
            MapLocation location = locations[i];
            if (location != null) {
                int newDis = fromLocation.distanceSquaredTo(location);
                if (newDis < dis) {
                    rv = i;
                    dis = newDis;
                }
            }
        }
        assert dis != Integer.MAX_VALUE;
        return rv;
    }
    static int getClosestID(RobotController rc, MapLocation[] locations) {
        return getClosestID(rc.getLocation(), locations);
    }

    static int getClosestDis(MapLocation fromLocation, MapLocation[] locations) {
        int id = getClosestID(fromLocation, locations);
        return fromLocation.distanceSquaredTo(locations[id]);
    }
    static int getClosestDis(RobotController rc, MapLocation[] locations) {
        return getClosestDis(rc.getLocation(), locations);
    }

    static MapLocation getClosestLoc(MapLocation fromLocation, MapLocation[] locations) {
        return locations[getClosestID(fromLocation, locations)];
    }

    static MapLocation getClosestLoc(RobotController rc, MapLocation[] locations) {
        return getClosestLoc(rc.getLocation(), locations);
    }

    // new path finding code from Ray
    private static final int PRV_LENGTH = 60;
    private static Direction[] prv = new Direction[PRV_LENGTH];
    private static int pathingCnt = 0;
    private static MapLocation lastPathingTarget = null;
    private static MapLocation lastLocation = null;
    private static int stuckCnt = 0;
    private static int lastPathingTurn = 0;
    private static int currentTurnDir = FastMath.rand256() % 2;
    public static int disableTurnDirRound = 0;

    private static Direction[] prv_ = new Direction[PRV_LENGTH];
    private static int pathingCnt_ = 0;
    static int MAX_DEPTH = 15;

    static String indicator;
    static String moveToward(RobotController rc, MapLocation location) throws GameActionException {
        // reset queue when target location changes or there's gap in between calls
        if (!location.equals(lastPathingTarget) || lastPathingTurn < rc.getRoundNum() - 4) {
            pathingCnt = 0;
            stuckCnt = 0;
        }
        indicator = "";
        indicator += String.format("2%sc%dt%s,", location, pathingCnt, currentTurnDir == 0? "L":"R");
        if (rc.isMovementReady()) {
            // we increase stuck count only if it's a new turn (optim for empty carriers)
            if (rc.getLocation().equals(lastLocation)) {
                if (rc.getRoundNum() != lastPathingTurn) {
                    stuckCnt++;
                }
            } else {
                lastLocation = rc.getLocation();
                stuckCnt = 0;
            }
            lastPathingTarget = location;
            lastPathingTurn = rc.getRoundNum();
            if (stuckCnt >= 3) {
                indicator += "stuck reset";
                randomMove(rc);
                pathingCnt = 0;
            }

            if (pathingCnt == 0) {
                //if free of obstacle: try go directly to target
                Direction dir = rc.getLocation().directionTo(location);
                boolean dirCanPass = canPass(rc, dir);
                boolean dirRightCanPass = canPass(rc, dir.rotateRight(), dir);
                boolean dirLeftCanPass = canPass(rc, dir.rotateLeft(), dir);
                if (dirCanPass || dirRightCanPass || dirLeftCanPass) {
                    if (dirCanPass) {
                        move(rc, dir);
                    } else if (dirRightCanPass) {
                        move(rc, dir.rotateRight());
                    } else if (dirLeftCanPass) {
                        move(rc, dir.rotateLeft());
                    }
                } else {
                    //encounters obstacle; run simulation to determine best way to go
                    if (rc.getRoundNum() > disableTurnDirRound) {
                        currentTurnDir = getTurnDir(rc, dir, location);
                    }
                    while (!canPass(rc, dir) && pathingCnt != 8) {
//                        rc.setIndicatorLine(rc.getLocation(), rc.getLocation().add(dir), 0, 0, 255);
                        if (!rc.onTheMap(rc.getLocation().add(dir))) {
                            currentTurnDir ^= 1;
                            pathingCnt = 0;
                            indicator += "edge switch";
                            disableTurnDirRound = rc.getRoundNum() + 100;
                            return indicator;
                        }
                        prv[pathingCnt] = dir;
                        pathingCnt++;
                        if (currentTurnDir == 0) dir = dir.rotateLeft();
                        else dir = dir.rotateRight();
                    }
                    if (pathingCnt == 8) {
                        indicator += "permblocked";
                    }
                }
            } else {
                //update stack of past directions, move to next available direction
                if (pathingCnt > 1 && canPass(rc, prv[pathingCnt - 2])) {
                    pathingCnt -= 2;
                }
                while (pathingCnt > 0 && canPass(rc, prv[pathingCnt - 1])) {
//                    rc.setIndicatorLine(rc.getLocation(), rc.getLocation().add(prv[pathingCnt - 1]), 0, 255, 0);
                    pathingCnt--;
                }
                if (pathingCnt == 0) {
                    Direction dir = rc.getLocation().directionTo(location);
                    if (!canPass(rc, dir)) {
                        prv[pathingCnt++] = dir;
                    }
                }
                int pathingCntCutOff = Math.min(PRV_LENGTH, pathingCnt + 8); // if 8 then all dirs blocked
                while (pathingCnt > 0 && !canPass(rc, currentTurnDir == 0?prv[pathingCnt - 1].rotateLeft():prv[pathingCnt - 1].rotateRight())) {
                    prv[pathingCnt] = currentTurnDir == 0?prv[pathingCnt - 1].rotateLeft():prv[pathingCnt - 1].rotateRight();
//                    rc.setIndicatorLine(rc.getLocation(), rc.getLocation().add(prv[pathingCnt]), 255, 0, 0);
                    if (!rc.onTheMap(rc.getLocation().add(prv[pathingCnt]))) {
                        currentTurnDir ^= 1;
                        pathingCnt = 0;
                        indicator += "edge switch";
                        disableTurnDirRound = rc.getRoundNum() + 100;
                        return indicator;
                    }
                    pathingCnt++;
                    if (pathingCnt == pathingCntCutOff) {
                        pathingCnt = 0;
                        indicator += "cutoff";
                        return indicator;
                    }
                }
                Direction moveDir = pathingCnt == 0? prv[pathingCnt] :
                        (currentTurnDir == 0?prv[pathingCnt - 1].rotateLeft():prv[pathingCnt - 1].rotateRight());

            }
        }
        lastPathingTarget = location;
        lastPathingTurn = rc.getRoundNum();

        return indicator;
    }

    static int getSteps(MapLocation a, MapLocation b) {
        int xdif = a.x - b.x;
        int ydif = a.y - b.y;
        if (xdif < 0) xdif = -xdif;
        if (ydif < 0) ydif = -ydif;
        if (xdif > ydif) return xdif;
        else return ydif;
    }

    static int getCenterDir(RobotController rc, Direction dir) throws GameActionException {
        double a = rc.getLocation().x - rc.getMapWidth()/2.0;
        double b = rc.getLocation().y - rc.getMapHeight()/2.0;
        double c = dir.dx;
        double d = dir.dy;
        if (a * d - b * c > 0) return 1;
        return 0;
    }

    private static final int BYTECODE_CUTOFF = 10000;
    static int getTurnDir(RobotController rc, Direction direction, MapLocation target) throws GameActionException{
        //int ret = getCenterDir(direction);
        MapLocation now = rc.getLocation();
        int moveLeft = 0;
        int moveRight = 0;

        pathingCnt_ = 0;
        Direction dir = direction;
        while (!canPass(rc, now.add(dir), dir) && pathingCnt_ != 8) {
            prv_[pathingCnt_] = dir;
            pathingCnt_++;
            dir = dir.rotateLeft();
            if (pathingCnt_ > 8) {
                break;
            }
        }
        now = now.add(dir);

        int byteCodeRem = Clock.getBytecodesLeft();
        if (byteCodeRem < BYTECODE_CUTOFF)
            return FastMath.rand256() % 2;
        //simulate turning left
        while (pathingCnt_ > 0) {
            moveLeft++;
            if (moveLeft > MAX_DEPTH) {
                break;
            }
            if (Clock.getBytecodesLeft() < BYTECODE_CUTOFF) {
                moveLeft = -1;
                break;
            }
            while (pathingCnt_ > 0 && canPass(rc, now.add(prv_[pathingCnt_ - 1]), prv_[pathingCnt_ - 1])) {
                pathingCnt_--;
            }
            if (pathingCnt_ > 1 && canPass(rc, now.add(prv_[pathingCnt_ - 1]), prv_[pathingCnt_ - 2])) {
                pathingCnt_-=2;
            }
            while (pathingCnt_ > 0 && !canPass(rc, now.add(prv_[pathingCnt_ - 1].rotateLeft()), prv_[pathingCnt_ - 1].rotateLeft())) {
                prv_[pathingCnt_] = prv_[pathingCnt_ - 1].rotateLeft();
                pathingCnt_++;
                if (pathingCnt_ > 8) {
                    moveLeft = -1;
                    break;
                }
            }
            if (pathingCnt_ > 8 || pathingCnt_ == 0) {
                break;
            }
            Direction moveDir = pathingCnt_ == 0? prv_[pathingCnt_] : prv_[pathingCnt_ - 1].rotateLeft();
            now = now.add(moveDir);
        }
        MapLocation leftend = now;
        pathingCnt_ = 0;
        now = rc.getLocation();
        dir = direction;
        //simulate turning right
        while (!canPass(rc, dir) && pathingCnt_ != 8) {
            prv_[pathingCnt_] = dir;
            pathingCnt_++;
            dir = dir.rotateRight();
            if (pathingCnt_ > 8) {
                break;
            }
        }
        now = now.add(dir);

        while (pathingCnt_ > 0) {
            moveRight++;
            if (moveRight > MAX_DEPTH) {
                break;
            }
            if (Clock.getBytecodesLeft() < BYTECODE_CUTOFF) {
                moveRight = -1;
                break;
            }
            while (pathingCnt_ > 0 && canPass(rc, now.add(prv_[pathingCnt_ - 1]), prv_[pathingCnt_ - 1])) {
                pathingCnt_--;
            }
            if (pathingCnt_ > 1 && canPass(rc, now.add(prv_[pathingCnt_ - 1]), prv_[pathingCnt_ - 2])) {
                pathingCnt_-=2;
            }
            while (pathingCnt_ > 0 && !canPass(rc, now.add(prv_[pathingCnt_ - 1].rotateRight()), prv_[pathingCnt_ - 1].rotateRight())) {
                prv_[pathingCnt_] = prv_[pathingCnt_ - 1].rotateRight();
                pathingCnt_++;
                if (pathingCnt_ > 8) {
                    moveRight = -1;
                    break;
                }
            }
            if (pathingCnt_ > 8 || pathingCnt_ == 0) {
                break;
            }
            Direction moveDir = pathingCnt_ == 0? prv_[pathingCnt_] : prv_[pathingCnt_ - 1].rotateRight();
            now = now.add(moveDir);
        }
        MapLocation rightend = now;
        //find best direction
        if (moveLeft == -1 || moveRight == -1) return FastMath.rand256() % 2;
        if (moveLeft + getSteps(leftend, target) <= moveRight + getSteps(rightend, target)) return 0;
        else return 1;

    }


    static void move(RobotController rc, Direction d) throws GameActionException {
        MapLocation loc = rc.getLocation().add(d);
        if(!rc.canSenseLocation(loc)) return;
        if(rc.canMove(d)) {
            rc.move(d);
        }
    }

    //TODO: Set this function up
    static boolean canPass(RobotController rc, MapLocation loc, Direction targetDir) throws GameActionException {
        if (!rc.onTheMap(loc)) return false;
        if (loc.equals(rc.getLocation())) return true;
        if (!rc.canSenseLocation(loc)) return true;
        MapInfo mi = rc.senseMapInfo(loc);

        if (mi.isWall()) return false;
        if (mi.hasRuin()) return false;
        if (rc.getType() == UnitType.MOPPER && mi.getPaint().isEnemy()) return false;
        if (rc.getType() == UnitType.MOPPER || rc.getType() == UnitType.SOLDIER) {
            for (RobotInfo r : rc.senseNearbyRobots(loc, 9, rc.getTeam().opponent())) {
                if (r.getType().isTowerType()) {
                    return false;
                }
            }
        }
        //if (rc.getType() == UnitType.MOPPER && mi.getPaint() == PaintType.EMPTY) return false;

        int adj = 0;
        for (Direction d : Direction.allDirections()) {
            if(!rc.canSenseLocation(loc.add(d))) continue;
            RobotInfo r = rc.senseRobotAtLocation(loc.add(d));
            if (r != null && r.getTeam() == rc.getTeam() && !r.getType().isTowerType()) adj++;
        }
       // if (adj >= 6) return false;
        RobotInfo robot = rc.senseRobotAtLocation(loc);
        if (robot == null)
            return true;
        return false;

    }

    static boolean canPass(RobotController rc, Direction dir, Direction targetDir) throws GameActionException {
        MapLocation loc = rc.getLocation().add(dir);
        return canPass(rc, loc, targetDir);
    }

    static boolean canPass(RobotController rc, Direction dir) throws GameActionException {
        return canPass(rc, dir, dir);
    }

    static Direction Dxy2dir(int dx, int dy) {
        if (dx == 0 && dy == 0) return Direction.CENTER;
        if (dx == 0 && dy == 1) return Direction.NORTH;
        if (dx == 0 && dy == -1) return Direction.SOUTH;
        if (dx == 1 && dy == 0) return Direction.EAST;
        if (dx == 1 && dy == 1) return Direction.NORTHEAST;
        if (dx == 1 && dy == -1) return Direction.SOUTHEAST;
        if (dx == -1 && dy == 0) return Direction.WEST;
        if (dx == -1 && dy == 1) return Direction.NORTHWEST;
        if (dx == -1 && dy == -1) return Direction.SOUTHWEST;
        assert false; // shouldn't reach here
        return null;
    }

    static boolean isPassable(RobotController rc, Direction d) throws GameActionException {
        return rc.canSenseLocation(rc.getLocation().add(d)) && rc.senseMapInfo(rc.getLocation().add(d)).isPassable();
    }


    static Direction mockMoveTowards(RobotController rc, MapLocation from, MapLocation location) throws GameActionException {
        // reset queue when target location changes or there's gap in between calls
        if (!location.equals(lastPathingTarget) || lastPathingTurn < rc.getRoundNum() - 4) {
            pathingCnt = 0;
            stuckCnt = 0;
        }
        RobotPlayer.indicator = "";
        RobotPlayer.indicator += String.format("2%sc%dt%s,", location, pathingCnt, currentTurnDir == 0? "L":"R");
        if (true) {
            // we increase stuck count only if it's a new turn (optim for empty carriers)
            if (from.equals(lastLocation)) {
                if (rc.getRoundNum() != lastPathingTurn) {
                    stuckCnt++;
                }
            } else {
                lastLocation = from;
                stuckCnt = 0;
            }
            lastPathingTarget = location;
            lastPathingTurn = rc.getRoundNum();
            /*if (stuckCnt >= 3) {
                RobotPlayer.indicator += "stuck reset";
                //randomMove(rc);
                pathingCnt = 0;
                return Direction.CENTER;
            }*/

            if (pathingCnt == 0) {
                //if free of obstacle: try go directly to target
                Direction dir = from.directionTo(location);
                boolean dirCanPass = rc.canSenseLocation(from.add(dir))
                        && rc.senseMapInfo(from.add(dir)).isPassable();
                boolean dirRightCanPass = rc.canSenseLocation(from.add(dir.rotateRight()))
                        && rc.senseMapInfo(from.add(dir.rotateRight())).isPassable();
                boolean dirLeftCanPass = rc.canSenseLocation(from.add(dir.rotateLeft()))
                        && rc.senseMapInfo(from.add(dir.rotateLeft())).isPassable();
                if (dirCanPass || dirRightCanPass || dirLeftCanPass) {
                    if (dirCanPass) {
                        return dir;
                    } else if (dirRightCanPass) {
                        return dir.rotateRight();
                    } else if (dirLeftCanPass) {
                        return dir.rotateLeft();
                    }
                } else {
                    //encounters obstacle; run simulation to determine best way to go
                    if (rc.getRoundNum() > disableTurnDirRound) {
                        currentTurnDir = getTurnDir(rc, dir, location);
                    }
                    while (!isPassable(rc, dir) && pathingCnt != 8) {
//                        rc.setRobotPlayer.indicatorLine(from, from.add(dir), 0, 0, 255);
                        if (!rc.onTheMap(from.add(dir))) {
                            currentTurnDir ^= 1;
                            pathingCnt = 0;
                            RobotPlayer.indicator += "edge switch";
                            disableTurnDirRound = rc.getRoundNum() + 100;
                            return Direction.CENTER;
                        }
                        prv[pathingCnt] = dir;
                        pathingCnt++;
                        if (currentTurnDir == 0) dir = dir.rotateLeft();
                        else dir = dir.rotateRight();
                    }
                    if (pathingCnt == 8) {
                        RobotPlayer.indicator += "permblocked";
                    } else if (isPassable(rc, dir)) {
                        return dir;
                    }
                }
            } else {
                //update stack of past directions, move to next available direction
                if (pathingCnt > 1 && isPassable(rc, prv[pathingCnt - 2])) {
                    pathingCnt -= 2;
                }
                while (pathingCnt > 0 && isPassable(rc, prv[pathingCnt - 1])) {
//                    rc.setRobotPlayer.indicatorLine(from, from.add(prv[pathingCnt - 1]), 0, 255, 0);
                    pathingCnt--;
                }
                if (pathingCnt == 0) {
                    Direction dir = from.directionTo(location);
                    if (!isPassable(rc, dir)) {
                        prv[pathingCnt++] = dir;
                    }
                }
                int pathingCntCutOff = Math.min(PRV_LENGTH, pathingCnt + 8); // if 8 then all dirs blocked
                while (pathingCnt > 0 && !isPassable(rc, currentTurnDir == 0?prv[pathingCnt - 1].rotateLeft():prv[pathingCnt - 1].rotateRight())) {
                    prv[pathingCnt] = currentTurnDir == 0?prv[pathingCnt - 1].rotateLeft():prv[pathingCnt - 1].rotateRight();
//                    rc.setRobotPlayer.indicatorLine(from, from.add(prv[pathingCnt]), 255, 0, 0);
                    if (!rc.onTheMap(from.add(prv[pathingCnt]))) {
                        currentTurnDir ^= 1;
                        pathingCnt = 0;
                        RobotPlayer.indicator += "edge switch";
                        disableTurnDirRound = rc.getRoundNum() + 100;
                        return Direction.CENTER;
                    }
                    pathingCnt++;
                    if (pathingCnt == pathingCntCutOff) {
                        pathingCnt = 0;
                        RobotPlayer.indicator += "cutoff";
                        return Direction.CENTER;
                    }
                }
                Direction moveDir = pathingCnt == 0? prv[pathingCnt] :
                        (currentTurnDir == 0?prv[pathingCnt - 1].rotateLeft():prv[pathingCnt - 1].rotateRight());
                if (isPassable(rc, moveDir)) {
                    return moveDir;
                } else {
                    // a robot blocking us while we are following wall, wait
                    RobotPlayer.indicator += "blocked";
                }
            }
        }
        lastPathingTarget = location;
        lastPathingTurn = rc.getRoundNum();

        return Direction.CENTER;
    }
    static void resetAfterMock() {
        pathingCnt = 0;
        stuckCnt = 0;
    }
}