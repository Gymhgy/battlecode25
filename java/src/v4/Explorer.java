package v4;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import v4.fast.FastMath;

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

    public static void init() throws GameActionException {
        exploreDirection = ORDINAL_DIRECTIONS[FastMath.rand256() % 8];
    }

    public static boolean smartExplore(RobotController rc) throws GameActionException {

        if (!Pathfinding.canPass(rc, exploreDirection) || rc.isLocationOccupied(rc.getLocation().add(exploreDirection))) {
            // find new direction
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

    private static final Double[] onTheMapProbeLengths = { 2.0, 2.0, 2.0 };
    private static final Double[] onTheMapProbeAngles = { -Math.PI/6, 0.0, Math.PI/6 };
    public static boolean reachedBorder(RobotController rc, double direction) {
        // On the map probing
        for (int i = onTheMapProbeLengths.length; --i >= 0; ) {
            double x = Math.cos(direction + onTheMapProbeAngles[i]) * onTheMapProbeLengths[i];
            double y = Math.sin(direction + onTheMapProbeAngles[i]) * onTheMapProbeLengths[i];
            if (!rc.onTheMap(rc.getLocation().translate((int)Math.round(x), (int)Math.round(y)))) return true;
        }
        return false;
    }

    public static double angleBetween(double a, double b) {
        double angle = Math.abs(b - a);
        return Math.min(angle, 2 * Math.PI - angle);
    }
}