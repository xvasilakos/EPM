package sim.space.users.mobile;

import exceptions.CriticalFailureException;
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

    private static final Logger LOGGER = Logger.getLogger(TraceMU.class.getCanonicalName());

    private double _dx;
    private double _dy;
    private final int _areaMaxX;
    private final int _areaMaxY;

    public TraceMU(TraceMUBuilder muBuilder) throws CriticalFailureException {
        super(muBuilder);

        _areaMaxX = getSim().getArea().getLengthX();
        _areaMaxY = getSim().getArea().getLengthY();

        _dx = muBuilder.getDx();
        _dy = muBuilder.getDy();
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
//xxx            LOGGER.log(Level.WARNING,
//                    "{0} invocation with disconFrom and conTo cells being the "
//                    + "same. This may happen if the mobile moves back and forth "
//                    + "and will be ignored. "
//                    + "Previous coordinates: {1}; "
//                    + "Current coordinates: {2}",
//                    new Object[]{
//                        mthd,
//                        getCoordinatesPrevious().toSynopsisString(),
//                        getCoordinates().toSynopsisString()
//                    }
//            );
            return;
        }

        super.updtLastHandoverDuration(disconFrom, conTo, isLooped);

    }

    @Override
    protected void updtResidenceTime(SmallCell comingFrom, SmallCell residentIn, boolean isLooped) {

        if (residentIn.equals(comingFrom)) {
            String mthd = new Object() {
            }.getClass().getEnclosingMethod().getName();
//xxx            LOGGER.log(Level.WARNING,
//                    "{0} invocation with residence and origins cells being the "
//                    + "same. This may happen if the mobile moves back and forth "
//                    + "and will be ignored. "
//                    + "Previous coordinates: {1}; "
//                    + "Current coordinates: {2}",
//                    new Object[]{
//                        mthd,
//                        getCoordinatesPrevious().toSynopsisString(),
//                        getCoordinates().toSynopsisString()
//                    }
//            );
            return;
        }

        super.updtResidenceTime(comingFrom, residentIn, isLooped);
    }

    /**
     * @return the _dx
     */
    public double getDx() {
        return _dx;
    }

    /**
     * @param _dx the _dx to set
     */
    public void setDx(double _dx) {
        this._dx = _dx;
    }

    /**
     * @return the _dy
     */
    public double getDy() {
        return _dy;
    }

    /**
     * @param _dy the _dy to set
     */
    public void setDy(double _dy) {
        this._dy = _dy;
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

    /**
     * Moves this MU based on the most recently set dx, dy marginal coordinate
     * differences.
     *
     * @param overrideMinResidence
     * @param overrideStartRoaming
     * @return an update on the connectivity status
     * @throws exceptions.InvalidOrUnsupportedException
     * @throws exceptions.WrongOrImproperArgumentException
     *
     */
    @Override
    public ConnectionStatusUpdate move(
            boolean overrideMinResidence,
            boolean overrideStartRoaming)
            throws InvalidOrUnsupportedException,
            WrongOrImproperArgumentException {

        if ((!overrideMinResidence && !canMove())
                || (!overrideStartRoaming && !afterStartingRoaming())) {
            return _mostRecentConnStatusUpdate;
        }


        /* Point after moving. if looped to the other side of the area, 
         * the boolean field is true.
         */ Couple<Point, Boolean> newPointLoopedCouple = selectNewCoordinate(_dx, _dy);

        return moveToNewPoint(newPointLoopedCouple);
    }

}
