package sim.space.cell;

import sim.space.cell.demand_registry.LocalDemand;
import exceptions.InconsistencyException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import sim.ISimulationMember;
import utils.ISynopsisString;
import sim.run.SimulationBaseRunner;
import sim.space.Area;
import sim.space.ISpaceMember;
import sim.space.Point;
import sim.space.cell.demand_registry.PopularityInfo;
import sim.space.users.mobile.MobileUser;
import sim.space.users.User;
import sim.space.util.DistanceComparator;

/**
 *
 * @author Xenofon Vasilakos xvas@aueb.gr
 */
public abstract class AbstractCell implements ISimulationMember, ISpaceMember, ISynopsisString {

    protected final SimulationBaseRunner _sim;
    /////////////////////////////////////////
    private static int _idGen = 0;

    private static int generateNxtID() {
        return ++_idGen;
    }
    private final int _id;
    /////////////////////////////////////////
    private final Point _center;
    private final double _radius;
    private final Area area;
    protected final Set<Point> _coveredAreaPoints;
    protected final Set<MobileUser> _connectedMUs;
    protected final Set<User> _connectedAllUsers;

    /**
     * A registry for keeping track of information regarding the requests demand
     * by the mobile users which are currently connected to the cell.
     */
    private final LocalDemand _lclDmd;
    private final PopularityInfo _popInfo;
    protected final String _computePopularityType;

    protected AbstractCell(int id) {
        this._lclDmd = new LocalDemand(this);
        this._popInfo = new PopularityInfo(this);
        this._id = id;

        this._sim = null;

        this._center = null;
        this._radius = -1;
        this.area = null;

        this._coveredAreaPoints = null;
        this._connectedMUs = null;
        this._connectedAllUsers = null;

        this._computePopularityType = null;
    }

    /**
     * Initializes but does not compute coverage of points. ID is sequentially
     * assigned.
     *
     * @param sim
     * @param centerY
     * @param centerX
     * @param radius
     * @param area the area to register this new cell
     */
    public AbstractCell(SimulationBaseRunner sim, int centerY, int centerX, double radius,
            Area area) {
        this._lclDmd = new LocalDemand(this);
        this._popInfo = new PopularityInfo(this);

        this._id = generateNxtID();

        String sanityMsg = "Center out range: ";
        if (centerY > area.getLengthY() || centerY < 0) {
            throw new InconsistencyException(sanityMsg + "center Y = " + centerY + ". Max Y = " + area.getLengthY());
        }
        if (centerX > area.getLengthX() || centerX < 0) {
            throw new InconsistencyException(sanityMsg + "center X = " + centerX + ". Max X = " + area.getLengthX());
        }

        this._sim = sim;

        this._center = area.getPointAt(centerX, centerY);
        this._radius = radius;
        this.area = area;

        this._coveredAreaPoints = new HashSet<>();
        this._connectedMUs = new HashSet<>();
        this._connectedAllUsers = new HashSet<>();

        this._computePopularityType = sim.getItemPopCmptType();
    }

    /**
     * Initializes but does not compute coverage of points.
     *
     * @param id
     * @param sim
     * @param centerY
     * @param centerX
     * @param radius
     * @param area the area to register fo this new cell
     * @param transitionNeighbors
     */
    public AbstractCell(int id, SimulationBaseRunner sim, int centerY, int centerX,
            double radius, Area area, Map<Integer, Double> transitionNeighbors) {
        this._lclDmd = new LocalDemand(this);
        this._popInfo = new PopularityInfo(this);

        this._id = id;
        //<editor-fold defaultstate="collapsed" desc="check _center getCoordinates">
        String sanityMsg = "Center out of range: ";
        if (centerY >= area.getLengthY() || centerY < 0) {
            throw new InconsistencyException(sanityMsg + "centerY = " + centerY + ". Max Y = " + (area.getLengthY() - 1));
        }
        if (centerX >= area.getLengthX() || centerX < 0) {
            throw new InconsistencyException(sanityMsg + "centerX = " + centerX + ". Max X = " + (area.getLengthX() - 1));
        }
        //</editor-fold>

        this._sim = sim;

        this._center = area.getPointAt(centerX, centerY);
        this._radius = radius;
        this.area = area;

        this._coveredAreaPoints = new HashSet<>();
        this._connectedMUs = new HashSet<>();
        this._connectedAllUsers = new HashSet<>();

        this._computePopularityType = sim.getItemPopCmptType();
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 17 * hash + Objects.hashCode(this._sim);
        hash = 17 * hash + this._id;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final AbstractCell other = (AbstractCell) obj;
        if (this._id != other._id) {
            return false;
        }
        return Objects.equals(this._sim, other._sim);
    }

    /**
     * @param p the point to check if in circular range.
     * @return true iff the point is covered by the cell
     */
    public boolean covers(Point p) {
        if (this._coveredAreaPoints.contains(p)) {
            return true;
        } else if (DistanceComparator.euclidianDistance(getCoordinates(), p) < getRadius()) {
            _coveredAreaPoints.add(p);

        } else if (p == null) {
            throw new NullPointerException("Point is null");
        }
        return false;
    }

    /**
     * @param mu the Mobile User.
     * @return true iff the point the mobile user is currently at, is in covered
     * area
     */
    public boolean covers(MobileUser mu) {
        return AbstractCell.this.covers(mu.getCoordinates());
    }

    /**
     *
     * @param cell
     * @return true iff there is at least one common Point in the coverage
     * areas.
     */
    public boolean intersectsWith(AbstractCell cell) {
        for (Point nxt_point : _coveredAreaPoints) {
            if (cell.covers(nxt_point)) {
                return true;
            }
        }

        return false;
    }

    /**
     *
     * @param pointsSet
     * @return a subset of the past points, i.e. the intersection of the
     * coverage area of this cell with the set passed as a parameter.
     */
    public Set<Point> computeIntersectionWith(Set<Point> pointsSet) {
        AbstractCell currCell = this;
        Set<Point> intersectionSet = new HashSet<>();

        for (Point nxtCovrdPoint : currCell._coveredAreaPoints) {
            if (pointsSet.contains(nxtCovrdPoint)) {
                intersectionSet.add(nxtCovrdPoint);
            }
        }

        if (intersectionSet.isEmpty()) {
            return null;
        }
        return intersectionSet;
    }

    Set<Point> computeIntersectionWith(AbstractCell abstractCell) {
        return computeIntersectionWith(abstractCell.getCoverageArea());
    }

    /**
     * @param range
     * @return If the range of this cell coinsides with the range of points
     * passed as a parameter
     */
    public boolean rangeCoinsides(Set<Point> range) {
        Set<Point> whichInRange = computeIntersectionWith(range);
        return range.containsAll(whichInRange) && whichInRange.containsAll(range);
    }

    /**
     * @return an unmodifiable set of connected MUs
     */
    public Set<MobileUser> getConnectedMUs() {
        return Collections.unmodifiableSet(_connectedMUs);
    }

    public Set<User> getConnectedAllUsers() {
        return Collections.unmodifiableSet(_connectedAllUsers);
    }

    public void connectUser(User usr) {
        _connectedAllUsers.add(usr);
        if (usr instanceof MobileUser) {
            _connectedMUs.add((MobileUser) usr);
        }
    }

    public void disconnectUser(User usr) {
        _connectedAllUsers.remove(usr);
        if (usr instanceof MobileUser) {
            _connectedMUs.remove((MobileUser) usr);
        }
    }

    public int getID() {
        return _id;
    }

    @Override
    /**
     * @return the center of this Cell.
     */
    public Point getCoordinates() {
        return getCenter();
    }

    /**
     * @return the X coordinate of the _center of this cell.
     */
    @Override
    public int getX() {
        return getCenter().getX();
    }

    /**
     * @return the Y coordinate of the _center of this cell.
     */
    @Override
    public int getY() {
        return getCenter().getY();
    }

    /**
     * @return the _radius of range of this cell.
     */
    public double getRadius() {
        return this._radius;
    }

    /**
     * Returns the set points that belong to the covered area by this cell. Note
     * that the returned set returned is unmodifiable.
     *
     * @return a set points that belong to the covered area by this cell
     */
    public Set<Point> getCoverageArea() {
        return Collections.unmodifiableSet(this._coveredAreaPoints);
    }

    public String getCoverageAreaStringMap() {
        StringBuilder b = new StringBuilder();

        int count = 0;
        for (Point p : this._coveredAreaPoints) {
            if (count % 2 * _radius == 0) {
                b.append("\n").append("Line: ").append(count++ / 2 * _radius);
            }
            b.append(p.toSynopsisString()).append('\t');
        }

        return b.toString();
    }

    /**
     * Adds a point to the area covered by this cell.
     *
     * @param p the point that is added to the area of coverage
     */
    public void addCoverage(Point p) {
        this._coveredAreaPoints.add(p);
    }

    /**
     * @return a synoptic string representation with less details than the
     * toString() method
     */
    @Override
    public String toSynopsisString() {
        StringBuilder builder = new StringBuilder();
        builder.append(getClass().getSimpleName());
        builder.append("ID=");
        builder.append(getID());
        builder.append("; center=");
        builder.append(getCoordinates().toSynopsisString());
        builder.append(";radius=");
        builder.append(getRadius());

        return builder.toString();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(getClass().getSimpleName());
        builder.append("<");

        builder.append(toSynopsisString());

        builder.append(">");
        return builder.toString();
    }

    @Override
    public final int simID() {
        return getSimulation().getID();
    }

    @Override
    public final SimulationBaseRunner getSimulation() {
        return _sim;
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

    /**
     * A registry for keeping track of information regarding the requests demand
     * by the mobile users which are currently connected to the cell.
     *
     * @return the localDmd
     */
    public final LocalDemand getDmdLclForW() {
        return _lclDmd;
    }

    public final PopularityInfo getPopInfo() {
        return _popInfo;
    }

    /**
     * @return the _center
     */
    public Point getCenter() {
        return _center;
    }

  
}
