package sim.space;

import app.properties.valid.Values.StartPosition;
import exceptions.InconsistencyException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import sim.ISimulationMember;
import utils.ISynopsisString;
import sim.space.cell.AbstractCell;
import sim.space.cell.CellRegistry;
import sim.space.cell.smallcell.SmallCell;
import sim.space.users.mobile.MobileUser;
import sim.space.util.DistanceComparator;
import utils.CommonFunctions;
import utilities.Couple;

/**
 *
 * The area containing the cells. The area is composed by points, which can
 * belong to either one or more cells.
 *
 * @author Xenofon Vasilakos xvas@aueb.gr
 */
public class Area implements ISimulationMember, ISynopsisString {

    public static final Object NONE = new Object();

    private final Logger _logger;
    /**
     * An array used to include the points that compose the area. The
     * coordinates start from upper left corner (0,0) and end up to lower right
     * corner of the are (maxX-1, MaxY-1).
     */
    private final Point[][] pointArray;
    private final int lengthY;
    private final int lengthX;
    private HashSet<AbstractCell> _scs;
    private HashSet<MobileUser> _mus;
    private final sim.run.SimulationBaseRunner simulation;

    /**
     * The real area dimensions that is simulated.
     */
    private RealArea REAL_AREA;

    /**
     * Simply creates an area of points, which is empty of cells and mobile
     * users.
     *
     * sim lengthY lengthX
     */
    public Area(sim.run.SimulationBaseRunner sim, int lengthY, int lengthX) {
        simulation = sim;
        _logger = CommonFunctions.getLoggerFor(this);

        // point objects in array //  
        this.lengthY = lengthY;
        this.lengthX = lengthX;
        pointArray = new Point[lengthY][lengthX];

        int count = 0;
        int total = lengthX * lengthY;
        double percentage = 0.2;//this is too small, yields too many prinouts: sim.getScenario().doubleProperty(Simulation.PROGRESS_UPDATE);

        int printPer = (int) (total * percentage);

        /*
         * y getCoordinates are mapped  to  rows 
         * x getCoordinates are mapped  to  columns 
         */
        for (int y = 0; y < pointArray.length; y++) {
            Point[] rowY = pointArray[y];
            for (int x = 0; x < rowY.length; x++) {
                rowY[x] = new Point(x, y);
                if (++count % printPer == 0) {
                    _logger.log(Level.INFO,
                            "\t {0}% of the area completed.", Math.round(10000.0 * count / total) / 100.0);
                }
            }
        }

        // Cells //
        _scs = new HashSet<>();

        // Mobile Users //
        _mus = new HashSet<>();

    }

    /**
     * @return the maxY
     */
    public int getLengthY() {
        return lengthY;
    }

    /**
     * @return the maxX
     */
    public int getLengthX() {
        return lengthX;
    }

    /**
     * Returns the point at coordinates (x, y).
     * <b>Caution:</b> the point returned is actually (y,x) in the underlying
     * array.
     *
     * @param x
     * @param y
     * @return the point at coordinates (x,y)
     */
    public Point getPointAt(int x, int y) {
        return getPointArray()[y][x];
    }

    public Point getRandPoint() {
        int y = simulation.getRandomGenerator().randIntInRange(0, lengthY - 1);
        int x = simulation.getRandomGenerator().randIntInRange(0, lengthX - 1);
        return getPointArray()[y][x];
    }

    public Point getRandPoint(int fromY, int toY, int fromX, int toX) {
        int y = simulation.getRandomGenerator().randIntInRange(fromY, toY - 1);
        int x = simulation.getRandomGenerator().randIntInRange(fromX, toX - 1);
        return getPointArray()[y][x];
    }

    public void addSC(SmallCell sc) {
        _scs.add(sc);
    }

    public final void updtCoverageByRadius(SmallCell sc) {
        /* for each point geometrically in range, add the cell coverage to this point. 
         */
        Point cellCenter = sc.getCoordinates();
        double cellRad = sc.getRadius();

        /*
         * To save computation simTime, find first the minimum x and y getCoordinates  in coverage based on the center and radius.
         * Minimum x and y form a square out of which no point is covered surely. On the contrary,  most of the points in the square are
         * covered, excluding the ones with euclidian distance from the center that is higher thatn the radius.
         */
        int min_y = Math.max(0, cellCenter.getY() - (int) cellRad);
        int min_x = Math.max(0, cellCenter.getX() - (int) cellRad);

        int max_y = Math.min(this.lengthY - 1, cellCenter.getY() + (int) cellRad);
        int max_x = Math.min(this.lengthX - 1, cellCenter.getX() + (int) cellRad);

        int y = min_y;//54
        int x = min_x;//28

        while (y <= max_y) {
            while (x <= max_x) {
                Point candidate = pointArray[y][x];
                double distance = DistanceComparator.euclidianDistance(candidate, cellCenter);

                if (distance <= cellRad) {
                    candidate.addCoverage(sc);
                    sc.addCoverage(candidate);
                }
                x++;
            }
            y++;
            x = min_x;
        }
    }

    public final void addSCUpdateCoverageByRadius(Set<SmallCell> cells) {
        for (Iterator<SmallCell> it = cells.iterator(); it.hasNext();) {
            SmallCell nxt_cell = it.next();
            updtCoverageByRadius(nxt_cell);
        }
    }

    public final void addMUs(Set<MobileUser> users) {
        for (Iterator<MobileUser> it = users.iterator(); it.hasNext();) {
            MobileUser nxt_mu = it.next();
            _mus.add(nxt_mu);
        }
    }

    public Set<AbstractCell> cells() {
        return Collections.unmodifiableSet(_scs);
    }

    /**
     * @return Unmodifiable setProperty of mobile users in the area.
     */
    public Set<MobileUser> MUs() {
        return Collections.unmodifiableSet(_mus);
    }

    /**
     * <b>Caution:</b> the underlying array maps coordinates (x, y) to position
     * (y,x) in the array, as y maps to the row of the array (starting from the
     * upper left corner of the area and ending at the lower right one) and x to
     * the column in the row y
     *
     * @return
     */
    public Point[][] getPointArray() {
        return pointArray;
    }

    public Set<Point> getPoints() {
        Set<Point> points = new HashSet<>(lengthY * lengthX);
        for (int y = 0; y < lengthY; y++) {
            for (int x = 0; x < lengthX; x++) {
                points.add(getPointAt(x, y));
            }
        }
        return points;
    }

    public void addSmallerCell(SmallCell aCell) {
        _scs.add(aCell);
    }

    public Couple<Point, Boolean> northWest(boolean loop, Point pointOfReference, double distance) {
        boolean looped = false;
        if (distance == 1) {
            return northWest(loop, pointOfReference);
        }
        int distanceInt = (int) Math.ceil(distance / Math.sqrt(2));// a bit of necessary rounding on pythagorean theorem for a diagonal move

        int newY = pointOfReference.getY() - distanceInt;
        if (newY < 0) {
            if (loop) {
                looped = true;
                newY += pointArray.length;//  recall newY is negative due to mving direction
                newY %= pointArray.length;//  recall newY is negative due to mving direction
            } else {
                return null;
            }
        }

        int newX = pointOfReference.getX() - distanceInt;
        if (newX < 0) {
            if (loop) {
                looped = true;
                newX += pointArray[0].length; // first add to avoid out of bounds exception if using mod and then addition
                newX %= pointArray[0].length;
            } else {
                return null;
            }
        }

        return new Couple(pointArray[newY][newX], looped);
    }

    public Couple<Point, Boolean> northWest(boolean loop, Point pointOfReference) {
        boolean looped = false;
        int newY = pointOfReference.getY() - 1;
        int newX = pointOfReference.getX() - 1;
        if (pointOfReference.getY() == 0) {
            if (newY < 0) {
                if (loop) {
                    looped = true;
                    newY = pointArray[0].length + newY;// recall newY is negative
                } else {
                    return null;
                }
            }
        }

        if (pointOfReference.getX() == 0) {
            if (newX < 0) {
                if (loop) {
                    looped = true;
                    newX = pointArray[0].length + newX;
                } else {
                    return null;
                }
            }
        }

        return new Couple(pointArray[newY][newX], looped);
    }

    public Couple<Point, Boolean> north(boolean loop, Point pointOfReference, double distance) {
        boolean looped = false;

//        if (distance == 1) {
//            return north(loop, pointOfReference);
//        }
        int distanceInt = (int) Math.ceil(distance);
        int newY = pointOfReference.getY() - distanceInt;
        int newX = pointOfReference.getX();

        if (newY < 0) {
            if (loop) {
                looped = true;
                newY += pointArray.length;// recall newY is negative due to mving direction
                newY %= pointArray.length;// if movement caueses to exceed the area multiple times
            } else {
                return null;
            }
        }

        return new Couple(pointArray[newY][newX], looped);
    }

    public Couple<Point, Boolean> north(boolean loop, Point pointOfReference) {
        boolean looped = false;
        int newY = pointOfReference.getY() - 1;
        if (newY < 0) {
            if (loop) {
                looped = true;
                newY += pointArray.length;// recall newY is negative due to mving direction when pointOfReference.getY()==0.
            } else {
                return null;
            }
        }
        return new Couple(pointArray[newY][pointOfReference.getX()], looped);
    }

    public Couple<Point, Boolean> northEast(boolean loop, Point pointOfReference, double distance) {
        boolean looped = false;
        if (distance == 1) {
            return northEast(loop, pointOfReference);
        }
        int distanceInt = (int) Math.ceil(distance / Math.sqrt(2));// a bit of necessary rounding on pythagorean theorem for a diagonal move
        int newY = pointOfReference.getY() - distanceInt;
        int newX = pointOfReference.getX() + distanceInt;

        if (newY < 0) {
            if (loop) {
                looped = true;
                newY += pointArray.length;// recall newY is negative
                newY %= pointArray.length;// if movement caueses to exceed the area multiple times
            } else {
                return null;
            }
        }

        if (newX >= pointArray[0].length) {
            if (loop) {
                looped = true;
                newX -= pointArray[0].length;
                newY %= pointArray[0].length;// if movement caueses to exceed the area multiple times
            } else {
                return null;
            }
        }

        return new Couple(pointArray[newY][newX], looped);
    }

    public Couple<Point, Boolean> northEast(boolean loop, Point pointOfReference) {
        boolean looped = false;
        int newY = pointOfReference.getY() - 1, newX = pointOfReference.getX() + 1;

        if (pointOfReference.getY() == 0) {
            if (loop) {
                looped = true;
                newY = pointArray.length - 1;
            } else {
                return null;
            }
        }
        if (newX == pointArray[0].length) {
            if (loop) {
                looped = true;
                newX = 0;
            } else {
                return null;
            }
        }

        return new Couple(pointArray[newY][newX], looped);
    }

    public Couple<Point, Boolean> west(boolean loop, Point pointOfReference, double distance) {
        boolean looped = false;
        if (distance == 1) {
            return west(loop, pointOfReference);
        }

        int distanceInt = (int) Math.ceil(distance);
        int newX = pointOfReference.getX() - distanceInt;

        if (pointOfReference.getX() - distanceInt < 0) {
            if (loop) {
                looped = true;
                newX += pointArray[0].length; // first add to avoid out of bounds exception if using mod and then addition
                newX %= pointArray[0].length;// if movement causes to exceed the area multiple times
            } else {
                return null;
            }
        }
//        try {
        return new Couple(pointArray[pointOfReference.getY()][newX], looped);
//        } catch (ArrayIndexOutOfBoundsException e) {
//            System.out.println("pointOfReference.getX()=" + pointOfReference.getX());
//            System.out.println("pointOfReference.getY()=" + pointOfReference.getY());
//            System.out.println("newX=" + newX);
//            System.out.println("pointArray[0].length=" + pointArray[0].length);
//            System.out.println("pointArray.length=" + pointArray.length);
//
////            System.exit(0);
//        }

    }

    public Couple<Point, Boolean> west(boolean loop, Point pointOfReference) {
        boolean looped = false;
        int newX = pointOfReference.getX() - 1;
        if (newX < 0) {
            if (loop) {
                looped = true;
                newX += pointArray[0].length;
            } else {
                return null;
            }
        }
        return new Couple(pointArray[pointOfReference.getY()][newX], looped);
    }

    public Couple<Point, Boolean> east(boolean loop, Point pointOfReference, double distance) {
        boolean looped = false;
//        if (distance == 1) {
//            return east(canLoop, pointOfReference);
//        }

        int distanceInt = (int) Math.ceil(distance);
        int newX = pointOfReference.getX() + distanceInt;

        if (newX >= pointArray[0].length) {
            if (loop) {
                looped = true;
                newX -= pointArray[0].length;
                newX %= pointArray[0].length;// if movement caueses to exceed the area multiple times
            } else {
                return null;
            }
        }

        return new Couple(pointArray[pointOfReference.getY()][newX], looped);
    }

    public Couple<Point, Boolean> east(boolean loop, Point pointOfReference) {
        boolean looped = false;

        int newX = pointOfReference.getX() + 1;

        if (newX == pointArray[0].length) {
            if (loop) {
                looped = true;
                newX -= pointArray[0].length;
            } else {
                return null;
            }
        }
        return new Couple(pointArray[pointOfReference.getY()][newX], looped);
    }

    public Couple<Point, Boolean> southWest(boolean loop, Point pointOfReference, double distance) {
        boolean looped = false;
        if (distance == 1) {
            return southWest(loop, pointOfReference);
        }
        int distanceInt = (int) Math.ceil(distance / Math.sqrt(2));// a bit of necessary rounding on pythagorean theorem for a diagonal move

        int newY = pointOfReference.getY() + distanceInt, newX = pointOfReference.getX() - distanceInt;

        if (newY >= pointArray.length) {
            if (loop) {
                looped = true;
                newY -= pointArray.length; // first subtract to avoid out of bounds exception due ot negative values if using mod and then subtractions
                newY %= pointArray.length;// if movement caueses to exceed the area multiple times
            } else {
                return null;
            }
        }
        if (newX < 0) {
            if (loop) {
                looped = true;
                newX += pointArray[0].length; // first add to avoid out of bounds exception if using mod and then addition
                newX %= pointArray.length;// if movement caueses to exceed the area multiple times
            } else {
                return null;
            }
        }

        return new Couple(pointArray[newY][newX], looped);

    }

    public Couple<Point, Boolean> southWest(boolean loop, Point pointOfReference) {
        boolean looped = false;
        int newY = pointOfReference.getY() + 1, newX = pointOfReference.getX() - 1;

        if (newY == pointArray.length) {
            if (loop) {
                looped = true;
                newY = 0;
            } else {
                return null;
            }
        }
        if (pointOfReference.getX() - 1 < 0) {
            if (loop) {
                looped = true;
                newX = pointArray[0].length - 1;
            } else {
                return null;
            }
        }
        return new Couple(pointArray[newY][newX], looped);
    }

    public Couple<Point, Boolean> south(boolean loop, Point pointOfReference, double distance) {
        boolean looped = false;
//        if (distance == 1) {
//            return south(loop, pointOfReference);
//        }

        int distanceInt = (int) Math.ceil(distance);

        int newY = pointOfReference.getY() + distanceInt;

        if (newY >= pointArray.length) {
            if (loop) {
                looped = true;
                newY -= pointArray.length; // first subtract to avoid out of bounds exception due ot negative values if using mod and then subtractions
                newY %= pointArray.length;// if movement caueses to exceed the area multiple times
            } else {
                return null;
            }
        }

        return new Couple(pointArray[newY][pointOfReference.getX()], looped);
    }

    public Couple<Point, Boolean> south(boolean loop, Point pointOfReference) {
        boolean looped = false;
        int newY = pointOfReference.getY() + 1;

        if (newY == pointArray.length) {
            if (loop) {
                looped = true;
                newY = 0;
            } else {
                return null;
            }
        }

        return new Couple(pointArray[newY][pointOfReference.getX()], looped);

    }

    public Couple<Point, Boolean> southEast(boolean loop, Point pointOfReference, double distance) {
        boolean looped = false;
        if (distance == 1) {
            return southEast(loop, pointOfReference);
        }
        int distanceInt = (int) Math.ceil(distance / Math.sqrt(2));// a bit of necessary rounding on pythagorean theorem for a diagonal move

        int newY = pointOfReference.getY() + distanceInt, newX = pointOfReference.getX() + distanceInt;

        if (newY >= pointArray.length) {
            if (loop) {
                looped = true;
                newY -= pointArray.length; // first subtract to avoid out of bounds exception due ot negative values if using mod and then subtractions
                newY %= pointArray.length;// if movement caueses to exceed the area multiple times
            } else {
                return null;
            }
        }

        if (newX >= pointArray[0].length) {
            if (loop) {
                looped = true;
                newX -= pointArray[0].length;
                newX %= pointArray[0].length;// if movement caueses to exceed the area multiple times
            } else {
                return null;
            }
        }

        return new Couple(pointArray[newY][newX], looped);
    }

    public Couple<Point, Boolean> southEast(boolean loop, Point pointOfReference) {
        boolean looped = false;

        int newY = pointOfReference.getY() + 1, newX = pointOfReference.getX() + 1;

        if (newY >= pointArray.length) {
            if (loop) {
                looped = true;
                newY -= pointArray.length;
                newY %= pointArray.length;// if movement caueses to exceed the area multiple times
            } else {
                return null;
            }
        }

        if (newX >= pointArray[0].length) {
            if (loop) {
                looped = true;
                newX -= pointArray[0].length;
                newX %= pointArray[0].length;// if movement caueses to exceed the area multiple times
            } else {
                return null;
            }
        }

        return new Couple(pointArray[pointOfReference.getY() + 1][pointOfReference.getX() + 1], looped);
    }

    public Point[][] neighboringPointsMatrix(boolean loopArea, Point referencePoint) {
        Point[][] neighboringPointsMatrix = new Point[3][3];
        neighboringPointsMatrix[0][0] = northWest(loopArea, referencePoint).getFirst();
        neighboringPointsMatrix[0][1] = north(loopArea, referencePoint).getFirst();
        neighboringPointsMatrix[0][2] = northEast(loopArea, referencePoint).getFirst();
        neighboringPointsMatrix[1][0] = west(loopArea, referencePoint).getFirst();
        neighboringPointsMatrix[1][1] = referencePoint;
        neighboringPointsMatrix[1][2] = east(loopArea, referencePoint).getFirst();
        neighboringPointsMatrix[2][0] = southWest(loopArea, referencePoint).getFirst();
        neighboringPointsMatrix[2][1] = south(loopArea, referencePoint).getFirst();
        neighboringPointsMatrix[2][2] = southEast(loopArea, referencePoint).getFirst();

        return neighboringPointsMatrix;
    }

    /**
     * @return the size of the area in square units.
     */
    public int size() {
        return getLengthX() * getLengthY();
    }

    /**
     * @return the central point of the area
     */
    public Point getPointCentral() {
        return getPointAt(lengthX / 2, lengthY / 2);
    }

    /**
     * @return the central/east-edge point of the area
     */
    public Point getPointCentralEast() {
        return getPointAt(lengthX - 1, lengthY / 2);
    }

    /**
     * @return the central/west-edge point of the area
     */
    public Point getPointCentralWest() {
        return getPointAt(0, lengthY / 2);
    }

    /**
     * @return the north-east edge point of the area
     */
    public Point getPointCentralNE() {
        return getPointAt(lengthX - 1, 0);
    }

    /**
     * @return the north-west edge point of the area
     */
    public Point getPointNW() {
        return getPointAt(0, 0);
    }

    /**
     * @return the south-eastedge point of the area
     */
    public Point getPointSE() {
        return getPointAt(lengthX - 1, lengthY - 1);
    }

    /**
     * @return the south-west edge point of the area
     */
    public Point getPointSW() {
        return getPointAt(0, lengthY - 1);
    }

    /**
     * @return the central/north-edge point of the area
     */
    public Point getPointCentralNorth() {
        return getPointAt(lengthX / 2, 0);
    }

    /**
     * @return the central/south-edge point of the area
     */
    public Point getPointCentralSouth() {
        return getPointAt(lengthX / 2, lengthY - 1);
    }

    /**
     * @return a random point from the area
     */
    public Point getPointRandom() {
        int y = simulation.getScenario().getRandomGenerator().randIntInRange(0, lengthY - 1);
        int x = simulation.getScenario().getRandomGenerator().randIntInRange(0, lengthX - 1);
        return getPointAt(x, y);
    }

    @Override
    public final int simID() {
        return simulation.getID();
    }

    @Override
    public final sim.run.SimulationBaseRunner getSimulation() {
        return simulation;
    }

    @Override
    public final int simTime() {
        return getSimulation().simTime();
    }

    @Override
    public String simTimeStr() {
        return "[" + simTime() + "]";
    }

    @Override
    public final CellRegistry simCellRegistry() {
        return getSimulation().getCellRegistry();
    }

    public Point[] getPoints(Collection<String> positions) {
        String[] arr = new String[positions.size()];
        return getPoints(
                positions.toArray(arr)
        );
    }

    public Point[] getPoints(String[] positions) {
        Point[] points = new Point[positions.length];
        int i = 0;
        for (String pos : positions) {
            points[i++] = getPoint(pos);
        }

        return points;
    }

    public Point getPoint(String position) {
        String errMsg = "Unsupported or malformed value: " + position;// in case needed
        String separator = "|";// in case of getCoordinates

        if (position.startsWith("[")) {
            if (position.endsWith("]") || !position.contains(separator)) {
                String coordinates = position.substring(1, position.length() - 1).replaceAll("\\s+", "");
                String x_str = coordinates.substring(0, coordinates.indexOf(separator));
                String y_str = coordinates.substring(coordinates.indexOf(separator) + 1);

                int x = Integer.valueOf(x_str), y = Integer.valueOf(y_str);
                if (coordinatesWithinArea(x, y)) {
                    return getPointAt(x, y);
                } else {
                    throw new InconsistencyException("Cordinates [%d,%d] are out of area. "
                            + "Valid values are x=[0,%d], y=[0,%d].", new Object[]{x, y, lengthX - 1, lengthY - 1});
                }
            } else {
                throw new RuntimeException(errMsg);
            }
        }

        StartPosition orientation = StartPosition.valueOf(position);

        switch (orientation) {
            case random:
                return getPointRandom();
            case center:
                return getPointCentral();
            case west:
                return getPointCentralWest();
            case east:
                return getPointCentralEast();

            case north:
                return getPointCentralNorth();
            case north_east:
                return getPointCentralNE();
            case north_west:
                return getPointNW();

            case south:
                return getPointCentralSouth();
            case south_east:
                return getPointSE();
            case south_west:
                return getPointSW();
            default:
                throw new UnsupportedOperationException(errMsg);
        }

    }

    public double overlappingSCsPerPoint() {
        double average = 0.0;
        for (Point[] nxt_pointRaw : pointArray) {
            for (Point nxt_point : nxt_pointRaw) {
                average += nxt_point.getCoveringSCs().size();
            }
        }

        average /= size();
        return average;
    }

    public double coveragePerPoint_SC() {
        double sum = 0.0;
        for (Point[] row : pointArray) {
            for (Point point : row) {
                sum += point.getCoveringSCs().size();
            }
        }
        return sum / size();
    }

    public double noCoveragePerPoint_SC() {
        double sum = 0.0;
        for (Point[] row : pointArray) {
            for (Point point : row) {
                if (point.getCoveringSCs().isEmpty()) {
                    sum++;
                }
            }
        }
        return sum / size();
    }

    public boolean coordinatesWithinArea(int x, int y) {
        return x >= 0
                && x < lengthX
                && y >= 0
                && y < lengthY;
    }

    @Override
    /**
     * @return true iff same hashID.
     */
    public boolean equals(Object b) {
        if (b == null) {
            return false;
        }

        if (!(b instanceof Area)) {
            return false;
        }

        Area area = (Area) b;
        return area.getSimulation() == this.getSimulation();
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 43 * hash + this.lengthY;
        hash = 43 * hash + this.lengthX;
        hash = 43 * hash + Objects.hashCode(this.simulation);
        return hash;
    }

    @Override
    public String toSynopsisString() {
        StringBuilder _toString = new StringBuilder();
        _toString.append("Height=").append(lengthY);
        _toString.append("; Length=").append(lengthX);

        return _toString.toString();
    }

    @Override
    public String toString() {
        StringBuilder _toString = new StringBuilder(toSynopsisString());
        _toString.append("\n List of cells:").append(CommonFunctions.toString("\n\t", _scs));
        _toString.append("\n List of mobile users:").append(CommonFunctions.toString("\n\t", _mus));

        return _toString.toString();
    }

    public void setRealAreaDimensions(int minX, int minY, int maxX, int maxY) {
        REAL_AREA = new RealArea(minX, minY, maxX, maxY);
    }

    /**
     * @return the real area dimensions that is simulated.
     *
     */
    public RealArea getREAL_AREA() {
        return REAL_AREA;
    }

    /**
     * The real area dimensions that is simulated.
     */
    public final class RealArea {

        public final int minX;
        public final int minY;
        public final int maxX;
        public final int maxY;

        private RealArea(int minX, int minY, int maxX, int maxY) {
            this.minX = minX;
            this.minY = minY;
            this.maxX = maxX;
            this.maxY = maxY;
        }

    }

}
