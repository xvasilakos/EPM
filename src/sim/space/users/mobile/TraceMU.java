package sim.space.users.mobile;

import exceptions.CriticalFailureException;
import exceptions.InconsistencyException;
import exceptions.InvalidOrUnsupportedException;
import exceptions.WrongOrImproperArgumentException;
import java.util.logging.Logger;
import sim.space.Point;
import sim.space.cell.smallcell.SmallCell;
import sim.space.connectivity.ConnectionStatusUpdate;
import utilities.Couple;

/**
 *
 * @author xvas
 */
public class TraceMU extends MobileUser {

    private static final Logger LOG = Logger.getLogger(TraceMU.class.getCanonicalName());

    private double dX;
    private double dY;
    private double speed;
    private final int _areaMaxX;
    private final int _areaMaxY;
    private int dTraceTime;
    private int traceTime;

    public TraceMU(TraceMUBuilder muBuilder) throws CriticalFailureException {
        super(muBuilder);
        this.dTraceTime = 0;
        this.traceTime = 0;
        this.speed = 0;

        _areaMaxX = getSimulation().getTheArea().getLengthX();
        _areaMaxY = getSimulation().getTheArea().getLengthY();

        dX = muBuilder.getDx();
        dY = muBuilder.getDy();
    }

    @Override
    public String toSynopsisString() {
        return "TraceMU:" + super.toSynopsisString();
    }

    @Override
    protected void updtLastHandoverDuration(SmallCell disconFrom, SmallCell conTo, boolean isLooped) {

        if (conTo.equals(disconFrom) && !isLooped) {
            String mthd = new Object() {
            }.getClass().getEnclosingMethod().getName();
            return;
        }

        super.updtLastHandoverDuration(disconFrom, conTo, isLooped);

    }

    @Override
    protected void updtResidenceTime(SmallCell comingFrom, SmallCell residentIn, boolean isLooped) {

        if (residentIn.equals(comingFrom)) {
            String mthd = new Object() {
            }.getClass().getEnclosingMethod().getName();

            return;
        }

        super.updtResidenceTime(comingFrom, residentIn, isLooped);
    }

    /**
     * @return the dX
     */
    public double getdX() {
        return dX;
    }

    /**
     * @param dX the dX to set
     */
    public void setDX(double dX) {
        this.dX = dX;
    }

    /**
     * @return the dY
     */
    public double getdY() {
        return dY;
    }

    /**
     * @param dY the dY to set
     */
    public void setDY(double dY) {
        this.dY = dY;
    }

    private Couple<Point, Boolean> selectNewCoordinate(double dx, double dy) {
        boolean loop = true;

        Couple<Point, Boolean> newPointisLoopedCoupled;

        Point coordinates = this.getCoordinates();
        if (dx < 0) {
            // up
            newPointisLoopedCoupled = _area.west(loop, coordinates, Math.abs(dx));/*
                 * use _velocity for distance because it refers to 1 simTime unit
             */
        } else {
            // up
            newPointisLoopedCoupled = _area.east(loop, coordinates, Math.abs(dx));
        }
        boolean isLoopedX = newPointisLoopedCoupled.getSecond();

        coordinates = newPointisLoopedCoupled.getFirst();
        if (dy < 0) {
            // up
            newPointisLoopedCoupled = _area.north(loop, coordinates, Math.abs(dy));/*
                 * use _velocity for distance because it refers to 1 simTime unit
             */
        } else {
            // up
            double diff = Math.abs(dy) % _areaMaxY; // due to trace, dx or dy can exceed area limits 2,3,..n times which will cause out of bounds exceptions
            newPointisLoopedCoupled = _area.south(loop, coordinates, diff);
        }
        boolean isLoopedY = newPointisLoopedCoupled.getSecond();

        newPointisLoopedCoupled.setSecond(isLoopedX || isLoopedY);

        return newPointisLoopedCoupled;
    }

    @Override
    public ConnectionStatusUpdate moveRelatively(
            boolean overrideMinResidence,
            boolean overrideStartRoaming)
            throws InvalidOrUnsupportedException,
            WrongOrImproperArgumentException {

        if ((!overrideMinResidence && !canMove())
                || (!overrideStartRoaming && !afterStartingRoaming())) {
            return _mostRecentConnStatusUpdate;
        }
        return moveRelatively();
    }

    /**
     * Moves the mobile user by interpreting the latest set coordinates of the
     * mobile user as relative to its current absolute coordinates.
     *
     * @return an update on the connectivity status
     * @throws exceptions.InvalidOrUnsupportedException
     * @throws exceptions.WrongOrImproperArgumentException
     *
     */
    public ConnectionStatusUpdate moveRelatively() throws InvalidOrUnsupportedException,
            WrongOrImproperArgumentException, InconsistencyException {
        /* Point after moving. if looped to the other side of the area,
        * the boolean field is true./* Point after moving. if looped to the other side of the area,
        * the boolean field is true./* Point after moving. if looped to the other side of the area,
        * the boolean field is true./* Point after moving. if looped to the other side of the area,
        * the boolean field is true.
         */
        Couple<Point, Boolean> newPointLoopedCouple = selectNewCoordinate(dX, dY);

        return moveToNewRelativePoint(newPointLoopedCouple);
    }

    /**
     * @return the speed
     */
    public double getSpeed() {
        return speed;
    }

    /**
     * @param speed the speed to set
     */
    public void setSpeed(double speed) {
        this.speed = speed;
    }

    public void setTraceTime(int t) {
        this.traceTime = t;
    }

    public void setDTraceTime(int dt) {
        this.dTraceTime = dt;
    }

    /**
     * @return the dTraceTime
     */
    public int getdTraceTime() {
        return dTraceTime;
    }

    /**
     * @param dTraceTime the dTraceTime to set
     */
    public void setdTraceTime(int dTraceTime) {
        this.dTraceTime = dTraceTime;
    }

    /**
     * @return the traceTime
     */
    public int getTraceTime() {
        return traceTime;
    }

}
