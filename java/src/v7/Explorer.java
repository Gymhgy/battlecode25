package v7;

import battlecode.common.*;
import v7.fast.*;

public class Explorer {
    private static Direction[] buffer = new Direction[8];
    private static final Direction[] ORDINAL_DIRECTIONS = {
            Direction.NORTH,
            Direction.NORTHEAST,
            Direction.EAST,
            Direction.SOUTHEAST,
            Direction.SOUTH,
            Direction.SOUTHWEST,
            Direction.WEST,
            Direction.NORTHWEST,
    };
    private static int bufferLength = 0;
    private static Direction exploreDirection = ORDINAL_DIRECTIONS[3];
    static MapLocation[] firstFour = new MapLocation[4];
    static MapLocation center;
    static int fourCount = 0;
    // Q: Is it useful to keep track of generally where we're sending bots?

    public static void makeFirstFour() {
        firstFour[0] = new MapLocation(center.x  - center.x / 4, center.y);
        firstFour[1] = new MapLocation(center.x  + center.x/ 4, center.y);
        firstFour[2] = new MapLocation(center.x, center.y + center.y / 4);
        firstFour[3] = new MapLocation(center.x, center.y - center.y/ 4);
    }
    public static Direction fromDelta(int dx, int dy) {
        double angle = Math.atan2(dy, dx);
        double degrees = Math.toDegrees(angle);
        degrees = (degrees + 360) % 360;
        if (degrees >= 337.5 || degrees < 22.5) {
            return Direction.EAST;
        } else if (degrees >= 22.5 && degrees < 67.5) {
            return Direction.NORTHEAST;
        } else if (degrees >= 67.5 && degrees < 112.5) {
            return Direction.NORTH;
        } else if (degrees >= 112.5 && degrees < 157.5) {
            return Direction.NORTHWEST;
        } else if (degrees >= 157.5 && degrees < 202.5) {
            return Direction.WEST;
        } else if (degrees >= 202.5 && degrees < 247.5) {
            return Direction.SOUTHWEST;
        } else if (degrees >= 247.5 && degrees < 292.5) {
            return Direction.SOUTH;
        } else { // degrees >= 292.5 && degrees < 337.5
            return Direction.SOUTHEAST;
        }
    }

    public static void init() throws GameActionException {
        exploreDirection = ORDINAL_DIRECTIONS[FastMath.rand256() % 8];
    }
    public static boolean smartExplore(RobotController rc) throws GameActionException {
        if (!Pathfinding.canPass(rc, exploreDirection) || rc.isLocationOccupied(rc.getLocation().add(exploreDirection))) {
            bufferLength = 0;
            for (int i = ORDINAL_DIRECTIONS.length; --i >= 0; ) {
                Direction direction = ORDINAL_DIRECTIONS[i];
                // new direction cannot be directly opposite of previous direction
                if (direction.equals(exploreDirection) || direction.opposite().equals(exploreDirection)) {
                    continue;
                }

                // possible valid direction
                buffer[bufferLength++] = direction;
            }
            if (bufferLength == 0) {
                // can't explore :(
                exploreDirection = ORDINAL_DIRECTIONS[FastMath.rand256() % 8];
            } else {
                exploreDirection = buffer[(int) (Math.random() * bufferLength)];
            }
        }
        if (rc.isMovementReady() && Pathfinding.canPass(rc, exploreDirection)) {
            rc.move(exploreDirection);
            return true;
        }
        return false;
    }
    public static Direction exploreInDirection(RobotController rc, Direction d) throws GameActionException { // Smart Explore in a direction
        if (!Explorer.senseBorderCheck(rc, directionToAngle(d)) || rc.isLocationOccupied(rc.getLocation().add(d))) {
            Direction newDirection = bounce(rc, d);
            if (rc.isMovementReady() && Pathfinding.canPass(rc, newDirection)) {
                rc.move(exploreDirection);
            }
            return newDirection;
        }
        if (rc.isMovementReady() && Pathfinding.canPass(rc, d)) {
            rc.move(d);
            return d;
        }
        return d;
    }
    public static Direction findDirection(RobotController rc) throws GameActionException {
        center = new MapLocation(rc.getMapWidth(), rc.getMapHeight());
        makeFirstFour();
        MapLocation targetLoc = firstFour[fourCount];
        MapLocation robotLoc = rc.getLocation();
        fourCount = fourCount + 1 % 4;
        return fromDelta(robotLoc.x - targetLoc.x, robotLoc.y - targetLoc.y);
    }



    private static final Double[] onTheMapProbeLengths = { 4.0, 4.0, 4.0 };
    private static final Double[] onTheMapProbeAngles = { -Math.PI/6, 0.0, Math.PI/6 };
    public static boolean senseBorderCheck(RobotController rc, double direction) {
        // On the map probing
        for (int i = onTheMapProbeLengths.length; --i >= 0; ) {
            double x = Math.cos(direction + onTheMapProbeAngles[i]) * onTheMapProbeLengths[i];
            double y = Math.sin(direction + onTheMapProbeAngles[i]) * onTheMapProbeLengths[i];
            if (!rc.onTheMap(rc.getLocation().translate((int)Math.round(x), (int)Math.round(y)))) return true;
        }
        return false;
    }
    public static double directionToAngle (Direction d) {
        return Math.toDegrees(Math.atan2(d.dy, d.dx));
    }
    public static Direction bounce(RobotController rc, Direction d) {

        return Direction.CENTER;
    }
    public static double angleBetween(double a, double b) {
        double angle = Math.abs(b - a);
        return Math.min(angle, 2 * Math.PI - angle);
    }
}