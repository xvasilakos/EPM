package sim.space.users.mobile;

import app.properties.Space;
import app.properties.valid.Values;
import caching.MaxPop;
import caching.Utils;
import caching.base.AbstractCachingPolicy;
import caching.incremental.Oracle;
import exceptions.CriticalFailureException;
import exceptions.InconsistencyException;
import exceptions.InvalidOrUnsupportedException;
import exceptions.WrongOrImproperArgumentException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import sim.Scenario;
import sim.content.Chunk;
import sim.space.Area;
import sim.content.request.DocumentRequest;
import sim.space.Point;
import sim.space.cell.CellRegistry;
import sim.space.cell.CellUtilities;
import sim.space.connectivity.ConnectionStatusUpdate;
import static sim.space.connectivity.ConnectionStatusUpdate.GOT_DISCONNECTED;
import static sim.space.connectivity.ConnectionStatusUpdate.REMAINS_DISCONNECTED_WAS_AT_SOME_POINT_CONNECTED;
import static sim.space.connectivity.ConnectionStatusUpdate.REMAINS_DISCONNECTED_WAS_NEVER_CONNECTED;
import sim.space.cell.smallcell.SmallCell;
import static sim.space.connectivity.ConnectionStatusUpdate.CONNECTED_FIRST_TIME_TO_SC;
import static sim.space.connectivity.ConnectionStatusUpdate.GOT_RECONNECTED_TO_SC_AFTER_TMP_DISCON;
import static sim.space.connectivity.ConnectionStatusUpdate.HANDOVER_AFTER_DISCONNECTION_PERIOD;
import static sim.space.connectivity.ConnectionStatusUpdate.HANDOVER_DIRECTLY;
import static sim.space.connectivity.ConnectionStatusUpdate.REMAINS_CONNECTED_TO_SAME_SC;
import sim.space.users.CachingUser;
import utilities.Couple;

/**
 *
 * @author Xenofon Vasilakos xvas@aueb.gr
 */
public class MobileUser extends CachingUser {

    private static final Logger LOG = Logger.getLogger(MobileUser.class.getCanonicalName());

    public static final MobileUser DUMMY_MU = new MobileUser(-2000000);

    protected ConnectionStatusUpdate _mostRecentConnStatusUpdate;
    private int _minResidencePeriodInSCExpiration;
    private int _minHandoffPeriodInSCExpiration;
    private final double _velocity;

    /**
     * Number of performed handoffs before beeing reset;
     */
    private int _handoffsPerformed;
    private Point _startCoordinates;

    private Point _currentCoordinates;

    private Point _previousCoordinates;
    private int _penultimateTimeMoved;
    private int _lastTimeMoved;
    protected final Area _area;
    /**
     * The small cell that this user was initially connected at startup (when it
     * was introduced to the simulation), or null if it was unconnected at
     * startup.
     */
    protected SmallCell _startConnectedSC;
    /**
     * The small cell that this user was lastly known to have been connected.
     * The user may still be connected to this small cell. If the value of this
     * filed is null, then the user was previously not connected to any small
     * cell, which may apply to newly introduced mobile users. unconnected.
     */
    protected SmallCell _lastKnownConnectedSC;
    protected SmallCell _previouslyConnectedSC;
    /**
     * The point in space that the mobile was last seen connected
     */
    private Point _lastKnownConnectedSCPoint;

    /**
     * SimulationBaseRunner time that this user got connected to a given small
     * cell, mapped to the small cell's ID.
     *
     * The simTime is mapped to the cell's ID.
     */
    private final Map<Integer, Integer> _lastKnownGotConnected;
    /**
     * SimulationBaseRunner time that this user got disconnected to a given
     * small cell, mapped to the small cell's ID.
     *
     * The simTime is mapped to the cell's ID.
     */
    private final Map<Integer, Integer> _lastKnownGotdisconnected;
    /**
     * SimulationBaseRunner simTime of the last known handoff performed by this
     * user. If not any performed so far, value -1 is used.
     */
    private int _lastHandoffTime;
    private int _lastHandoverDuration;

    /**
     * SimulationBaseRunner simTime of the last status reset of this user. If
     * not any performed so far, value -1 is used.
     */
    private int _lastResetStatusTime;
    private SmallCell _lastResetSC;

    /**
     * Used for handover and residence time accuracy
     */
    private final double _mobAccuracy;

    private final double[] _probsTransition;
    private final double[] _probsTransitionInitState;
    /**
     * The direction, i.e. position in the probabilities array, which has the
     * highest probability.
     */
    private int _maxProbDirection;
    /**
     * used when reseting the mobile to reset __maxProbDirection to its initial
     * state.
     */
    private final int _maxProbDirectionAtInitState;

    private final int _startRoamingTime;

    private final String _muTransitionDecisions;
    private final List<String> _connectionPolicySC;

    protected static final int ID_UNDEFINED = -1;
    private static int __idGen;
    private final boolean _softUser;

    ;

    // only to be used by the builder class in order to build the object
    protected MobileUser(Builder builder) throws CriticalFailureException {
        super(/*id*/(builder.__id == ID_UNDEFINED) ? ++__idGen : builder.__id,
                builder.__simulation,
                builder._cachingPolicies
        );
        this._softUser = Boolean.parseBoolean(getSimulation().getScenario().stringProperty(Space.MU__ISSOFT, false));

        this._lastTimeReqsUpdt = -1;

        this._velocity = builder.__group.gaussianVelocity();
        this._startRoamingTime = builder.__group.startRoamingTime(this._id); // based on id, i.e. getSim() entrance ranking
        setUserGroup(builder.__group);
        getUserGroup().add(this);

        this._previousCoordinates = null;
        this._lastKnownConnectedSC = null;
        this._previouslyConnectedSC = null;
        this._lastKnownConnectedSCPoint = null;

        this._lastKnownGotConnected = new HashMap<>(22);
        this._lastKnownGotdisconnected = new HashMap<>(22);

        try {
            this.addAllRequests(builder.__requests);
        } catch (Throwable ex) {
            throw new CriticalFailureException(ex);
        }

        this._area = builder.__area;

        //<editor-fold defaultstate="collapsed" desc="arrange start and current points">
        this._startCoordinates = builder.__startCoordinates;

        this._startCoordinates.addUser(this);
        this._currentCoordinates = _startCoordinates;
        this._penultimateTimeMoved = simTime();// use sim time. See why needed in code for consuming data 
        this._mostRecentConnStatusUpdate = null;
        this._lastTimeMoved = simTime();
        //</editor-fold>

        if (builder instanceof MUBuilder) {
            MUBuilder muBuilder = (MUBuilder) builder;
            this._probsTransition = new double[muBuilder.__probsTransition.length];
            this._probsTransitionInitState = new double[muBuilder.__probsTransition.length];
            System.arraycopy(muBuilder.__probsTransition, 0, this._probsTransition, 0, muBuilder.__probsTransition.length);
            System.arraycopy(muBuilder.__probsTransition, 0, this._probsTransitionInitState, 0, muBuilder.__probsTransition.length);
            _maxProbDirectionAtInitState = this._maxProbDirection = muBuilder.__maxProbDirection;
        } else {
            this._probsTransition = null;
            this._probsTransitionInitState = null;
            _maxProbDirectionAtInitState = this._maxProbDirection = -1;
        }

        _mobAccuracy = getSimulation().getScenario().doubleProperty(Space.MU__MOBILITYACCURACY);

        _connectedSinceSC = _lastHandoffTime = _lastHandoverDuration = -1;
        _lastResetStatusTime = simTime();

        _lastResetSC = null;

        this._connectionPolicySC = builder.__connectionPolicySC;

        this._muTransitionDecisions = builder.__transitionDecisions;

        connectToMC(getSimulation().macrocell());
        try {
            SmallCell sc;
            if ((sc = CellUtilities.findHandoffcandidates(getSimulation().
                    getCellRegistry(), this, _connectionPolicySC))
                    != null) {
                updtTheMostRecentConnStatus(CONNECTED_FIRST_TIME_TO_SC, sc, false);
            } else {
                updtTheMostRecentConnStatus(REMAINS_DISCONNECTED_WAS_NEVER_CONNECTED, null, false);
            }

        } catch (WrongOrImproperArgumentException | InvalidOrUnsupportedException ex) {
            throw new CriticalFailureException(ex);
        }
    }

    /**
     * Used only for dummy user
     */
    private MobileUser(int dummyID) {
        super(dummyID, null, null);
        _velocity = -1;

        _area = null;

        setUserGroup(null);

        _startCoordinates = null;

        _probsTransition = null;
        _probsTransitionInitState = null;

        _mobAccuracy = -1.0;

        _maxProbDirection = -1;
        _maxProbDirectionAtInitState = -1;
        _connectionPolicySC = null;

        _startRoamingTime = -1;

        _muTransitionDecisions = null;

        _lastKnownGotConnected = new HashMap<>(22);
        _lastKnownGotdisconnected = new HashMap<>(22);

        _softUser = false;// Boolean.parseBoolean(getSim().getScenario().stringProperty(Space.MU__ISSOFT, false));

    }

    protected void updtLastHandoverDuration(SmallCell disconFrom, SmallCell conTo, boolean isLooped) {

        if (disconFrom.equals(conTo) && !isLooped) {
            throw new InconsistencyException(
                    (conTo == null ? "" : "conTo=" + conTo.toSynopsisString())
                    + ", disconFrom=" + disconFrom.toSynopsisString()
                    + (_currentlyConnectedSC == null ? "" : ", _currentlyConnectedSC=" + _currentlyConnectedSC.toSynopsisString())
                    + (_previouslyConnectedSC == null ? "" : ", _previouslyConnectedSC=" + _previouslyConnectedSC.toSynopsisString())
                    + (_lastKnownConnectedSC == null ? "" : ", _lastKnownConnectedSC=" + _lastKnownConnectedSC.toSynopsisString())
            );
        }

        int lastConnTime = _lastKnownGotdisconnected.get(disconFrom.getID());
        _lastHandoverDuration = _lastKnownGotConnected.get(conTo.getID()) - lastConnTime;

        getSimulation().getCellRegistry().updtHandoverTransitionTime(
                this,
                disconFrom,
                conTo,
                _lastHandoverDuration
        );
        getSimulation().getStatsHandle().incHandoverscount();

    }

    protected void updtResidenceTime(SmallCell comingFrom, SmallCell residentIn, boolean isLooped) {

        if (residentIn == null) {
            throw new InconsistencyException();
        }

        //TODO this check is correct btin some scenarios with artificial mobility
        // the trapped if below can happen without being wrong
//        if (residentIn.equals(comingFrom) && !isLooped) {
//            throw new InconsistencyException(
//                    (comingFrom == null ? "" : "comingFrom=" + comingFrom.toSynopsisString())
//                    + ", residentIn=" + residentIn.toSynopsisString()
//                    + (_currentlyConnectedSC == null ? "" : ", _currentlyConnectedSC=" + _currentlyConnectedSC.toSynopsisString())
//                    + (_previouslyConnectedSC == null ? "" : ", _previouslyConnectedSC=" + _previouslyConnectedSC.toSynopsisString())
//                    + (_lastKnownConnectedSC == null ? "" : ", _lastKnownConnectedSC=" + _lastKnownConnectedSC.toSynopsisString())
//            );
//        }
        int residentInID = residentIn.getID();
        Integer connTime = _lastKnownGotConnected.get(residentInID);
        Integer disconnTime = _lastKnownGotdisconnected.get(residentInID);
        setLastResidenceDuration(disconnTime - connTime);

        getSimulation().getCellRegistry().updtResidenceTime(
                this.getUserGroup(), comingFrom, residentIn,
                (int) getLastResidenceDuration()
        );
    }

    @Override
    public MobileGroup getUserGroup() {
        return ((MobileGroup) super.getUserGroup());
    }

    public void cacheDescisionsPerformRegisterPC(SmallCell hostingSC) throws IOException, Throwable {
        setLastSCForCacheDecisions(hostingSC);

        /* take cache descisions for every caching candidate cell*/
        for (SmallCell targetSC : hostingSC.neighbors()) {

            double handoverProb = simCellRegistry().handoverProbability(this.getUserGroup(), hostingSC, targetSC);
//            if (handoverProb == 0) {
//                if (getSim().getNeighborhoodType().equals(Values.DISCOVER)) {
//                    throw new InconsistencyException("zero probability even though cells are neighbors..");
//                }
//            }

            CellRegistry cellRegistry = getSimulation().getCellRegistry();
            Couple<Double, Double> residenceStats = cellRegistry.getResidenceDurationBetween(this.getUserGroup(), hostingSC, targetSC, true);

            Double expectedResidenceDuration = residenceStats.getFirst();
            Double conf95ResidenceDur = residenceStats.getSecond();

            Couple<Double, Double> handoverStats = cellRegistry.getHandoverDurationBetween(getUserGroup(), hostingSC, targetSC, true);
            Double expectedHandoffDuration = handoverStats.getFirst();
            Double conf95HandoverDur = handoverStats.getSecond();

            cacheDescisionsPerformRegisterPC(targetSC,
                    expectedResidenceDuration, conf95ResidenceDur,
                    expectedHandoffDuration, conf95HandoverDur,
                    handoverProb
            );
        }

    }

    /**
     * Cancels all proactive cache decisions (according to and iff allowed by
     * each caching method used) and registered demand for requests by this
     * mobile user.
     *
     */
    public void cancelAndDeregisterPCOrders() {

        for (SmallCell nxtTargetSC : getLastSCForCacheDecisions().neighbors()) {
            cancelAndDeregisterPCOrders(nxtTargetSC);
        }

        setLastSCForCacheDecisions(null);
    }

    public final void cancelAndDeregisterPCOrders(SmallCell sc) {
        for (DocumentRequest nxtRequest : getRequests()) {
            for (AbstractCachingPolicy policy : getSimulation().getCachingStrategies()) {
                if (policy instanceof MaxPop) {
                    // cached object stay permanently in cache.
                    continue;
                }
                Utils.cancelCachingOrders(
                        this,
                        policy,
                        sc, nxtRequest
                );
                sc.getDmdPC(policy).deregisterUpdtInfoPC(this, nxtRequest);
            }
        }
    }

    public Point getLastKnownConnectedSCPoint() {
        return _lastKnownConnectedSCPoint;
    }

    public int getLastTimeMoved() {
        return this._lastTimeMoved;
    }

    public ConnectionStatusUpdate getMostRecentConnStatusUpdate() {
        return this._mostRecentConnStatusUpdate;
    }

    public int getPenultimateTimeMoved() {
        return this._penultimateTimeMoved;
    }

    public int getLastKnownConnectedTime(SmallCell sc) {
        return _lastKnownGotConnected.get(sc.getID());
    }

    public int getLastKnownDisconnectedTime(SmallCell sc) {
        return _lastKnownGotdisconnected.get(sc.getID());
    }

    public int getHandoffsPerformed() {
        return _handoffsPerformed;
    }

    private void incHandoffsPerformed() {
        _handoffsPerformed++;
    }

    public SmallCell getLastResetSC() {
        return _lastResetSC;
    }

    public String howToResetPosition() {
        return getUserGroup().getHowToResetPos();
    }

    private boolean isResettable() {
        return !getUserGroup().getHowToResetPos().equals(Values.LOOP_PLUS_NO_RESET);
    }

    /**
     * @return the _currentCoordinates
     */
    @Override
    public Point getCoordinates() {
        return _currentCoordinates;
    }

    /**
     * @return the _velocity
     */
    public double getVelocity() {
        return _velocity;
    }

    /**
     * The direction, i.e. position in the probabilities array, which has the
     * highest probability.
     *
     * @return the __maxProbDirection
     */
    public int getMaxProbDirection() {
        return _maxProbDirection;
    }

    /**
     * @return the _muTransitionDecisions
     */
    public String getMuTransitionDecisions() {
        return _muTransitionDecisions;
    }

    public void setConnectedSinceSC(int i) {
        _connectedSinceSC = i;
    }

    /**
     * The status of the MU is resetProperty, which includes its a position,
     * last simTime it moved, all of its cache actions are canceled, etc..
     *
     * @return either a CONNECTED_FIRST_TIME_TO_SC status if the mobile gets
     * connected to a cell after being reset to the area, or a
     * REMAINS_DISCONNECTED_WAS_NEVER_CONNECTED indicating that the mobile was
     * reset to point in the area where it can not connect to any small cell
     * according to the specified connection policy.
     * @throws exceptions.InvalidOrUnsupportedException
     * @throws exceptions.WrongOrImproperArgumentException
     *
     */
    public ConnectionStatusUpdate resetStatus() throws InvalidOrUnsupportedException, WrongOrImproperArgumentException {
        _handoffsPerformed = 0;
        _lastResetStatusTime = simTime();
        _lastResetSC = _lastKnownConnectedSC;

        // in case probs change during the simulation, reset to original mobility transition probs
        System.arraycopy(_probsTransitionInitState, 0, _probsTransition, 0, _probsTransition.length);
        _maxProbDirection = _maxProbDirectionAtInitState;

        //<editor-fold defaultstate="collapsed" desc="reset points">
        _previousCoordinates = null;
        if (getCoordinates() != null) {
            getCoordinates().removeUser(this);
        }

        if (howToResetPosition().equals(Values.LOOP_PLUS_NO_RESET)) {
            throw new InconsistencyException(""
                    + "Trying to reset a mobile while property parameter "
                    + "\"space.mu.group.reset.pos\" is set to "
                    + howToResetPosition() + " for this group of mobiles ");
        }

        _penultimateTimeMoved = simTime();// use sim time. See why needed in code for consuming data 
        _lastTimeMoved = simTime();//keep the time of reset to compute duration of moveRelatively for consuming data
        _lastHandoffTime = -1;
        _lastHandoverDuration = -1;
        setLastResidenceDuration(-1);
        //</editor-fold>

        //CAUTION Must reset caching decisions before resetting connectivity. 
        // Otherwise, cache decisions at  the new connection cell will not be reset.
        if (getLastSCForCacheDecisions() != null) {
            cancelAndDeregisterPCOrders();
        }

        if (_currentlyConnectedSC != null) { // for previous connection before resetting this MU
            disconnectFromSC();
        }

        _currentCoordinates = pointAfterReseting();

        getCoordinates().addUser(this);

        this._lastKnownConnectedSC = null;
        this._previouslyConnectedSC = null;
        this._lastKnownConnectedSCPoint = null;
        this._lastKnownGotConnected.clear();
        this._lastKnownGotdisconnected.clear();

        this.connectToMC(getSimulation().getCellRegistry().getMacroCell());
        SmallCell sc;
        if ((sc = CellUtilities.findHandoffcandidates(getSimulation().getCellRegistry(), this, _connectionPolicySC))
                != null) {
            updtTheMostRecentConnStatus(CONNECTED_FIRST_TIME_TO_SC, sc, false);
        } else {
            updtTheMostRecentConnStatus(REMAINS_DISCONNECTED_WAS_NEVER_CONNECTED, null, false);
        }
        getSimulation().removeHaveExitedPrevCell(this);// otherwise it can cause issues
        getSimulation().removeHaveHandedOver(this);// otherwise it can cause issues

        return _mostRecentConnStatusUpdate;
    }

    private Point pointAfterReseting() {
        Point newPoint = null;
        switch (howToResetPosition()) {
            case Values.INIT:
                newPoint = _startCoordinates;
                break;
            case Values.RANDOM:
                int randX = getSimulation().getRandomGenerator().randIntInRange(0, _area.getLengthX() - 1);
                int randY = getSimulation().getRandomGenerator().randIntInRange(0, _area.getLengthY() - 1);
                _startCoordinates = newPoint = _area.getPointAt(randX, randY);
                break;
            case Values.RANDOM_X:
                randX = getSimulation().getRandomGenerator().randIntInRange(0, _area.getLengthX() - 1);
                _startCoordinates = newPoint = _area.getPointAt(randX, _startCoordinates.getY());
                break;
            case Values.RANDOM_Y:
                randY = getSimulation().getRandomGenerator().randIntInRange(0, _area.getLengthY() - 1);
                _startCoordinates = newPoint = _area.getPointAt(_startCoordinates.getX(), randY);
                break;
            default:
                /*the only option left is [x|y]. Seee javadoc for #getPoint()*/
                _startCoordinates = newPoint = _area.getPoint(howToResetPosition());
                break;
        }
        return newPoint;
    }

    @Override
    public final void connectToSC(SmallCell sc) {
        int scID = sc.getID();
        int simTime = simTime();
        _lastKnownGotConnected.put(scID, simTime);

        if (!sc.equals(_lastKnownConnectedSC)) {// if not just a (re-)connection, but a handover 
            _previouslyConnectedSC = _lastKnownConnectedSC;
            // invoke this method after only after updating connectivity information
        }
        _lastKnownConnectedSC = _currentlyConnectedSC = sc;
        _lastKnownConnectedSCPoint = getCoordinates();
        _connectedSinceSC = simTime();
        _currentlyConnectedSC.connectUser(this);
    }

    @Override
    public SmallCell disconnectFromSC() {
        _lastKnownGotdisconnected.put(_currentlyConnectedSC.getID(), simTime());
        _connectedSinceSC = -1;

        _previouslyConnectedSC = _lastKnownConnectedSC = _currentlyConnectedSC;
        _currentlyConnectedSC = null;
        _previouslyConnectedSC.disconnectUser(this);

        return _lastKnownConnectedSC;
    }

    private Couple<Point, Boolean> selectNewCoordinate(int pos) {

        double muVelocity = getVelocity();
        muVelocity = muVelocity > 0 ? muVelocity : -muVelocity;
        Couple<Point, Boolean> newPointisLoopedCoupled = null;
        boolean loop = howToResetPosition().equals(Values.LOOP_PLUS_NO_RESET);

        switch (pos) {
            case 0: // up left
                newPointisLoopedCoupled = _area.northWest(loop, this.getCoordinates(), muVelocity);
                /*
                 * use _velocity for distance because it refers to 1 simTime unit
                 */

                break;
            case 1: // up
                newPointisLoopedCoupled = _area.north(loop, this.getCoordinates(), muVelocity);/*
                 * use _velocity for distance because it refers to 1 simTime unit
                 */

                break;
            case 2: // up right
                newPointisLoopedCoupled = _area.northEast(loop, this.getCoordinates(), muVelocity);/*
                 * use _velocity for distance because it refers to 1 simTime unit
                 */

                break;
            case 3: // left
                newPointisLoopedCoupled = _area.west(loop, this.getCoordinates(), muVelocity);/*
                 * use _velocity for distance because it refers to 1 simTime unit 
                 */

                break;
            case 4: // does not moveRelatively
                return new Couple<>(getCoordinates(), false); // no need to check for resetting
            case 5: // right
                newPointisLoopedCoupled = _area.east(loop, this.getCoordinates(), muVelocity);/*
                 * use _velocity for distance because it refers to 1 simTime unit
                 */
                break;
            case 6: // down left
                newPointisLoopedCoupled = _area.southWest(loop, this.getCoordinates(), muVelocity);/*
                 * use _velocity for distance because it refers to 1 simTime unit
                 */

                break;
            case 7: // down
                newPointisLoopedCoupled = _area.south(loop, this.getCoordinates(), muVelocity);/*
                 * use _velocity for distance because it refers to 1 simTime unit
                 */

                break;
            case 8: // down right
                newPointisLoopedCoupled = _area.southEast(loop, this.getCoordinates(), muVelocity);/*
                 * use _velocity for distance because it refers to 1 simTime unit
                 */

                break;
            default:
                String msg = "Bug in moving position. "
                        + "Moving pos = " + pos;
                throw new RuntimeException(msg);
        }//switch
        return newPointisLoopedCoupled;
    }

    /**
     * Moves the mobile user by interpreting the latest set coordinates of the
     * mobile user as relative to its current absolute coordinates.
     *
     * @param newPointLoopedCouple
     * @return
     * @throws InconsistencyException
     * @throws WrongOrImproperArgumentException
     * @throws InvalidOrUnsupportedException
     */
    protected ConnectionStatusUpdate moveToNewRelativePoint(Couple<Point, Boolean> newPointLoopedCouple)
            throws InconsistencyException, WrongOrImproperArgumentException, InvalidOrUnsupportedException {
        if (newPointLoopedCouple != null) {// if not got out of area and not looped to the other side of the area.
            // in case did not moveRelatively due to transition probability [1][1] 
            if (getCoordinates().equals(newPointLoopedCouple.getFirst())) {
                return _mostRecentConnStatusUpdate;
            }
            return moveForce(newPointLoopedCouple.getFirst(), newPointLoopedCouple.getSecond());
        } else if (isResettable()) {
            if (_currentlyConnectedSC != null) {
                // stat is kept for the previously connected cell, aka the residence cell
                SmallCell originatingFromSC = _previouslyConnectedSC;
                SmallCell residentInSC = _lastKnownConnectedSC;

                disconnectFromSC();

                if (originatingFromSC != null && residentInSC != null) {
                    updtResidenceTime(originatingFromSC, residentInSC, false /*always false as newPointLoopedCouple==null in this case*/);
                }
            }
            return resetStatus();
        } else {
            throw new InconsistencyException("Mobile was allowed to exit the area without looping back to the area.");
        }
    }

    /**
     * Moves this MU. The mobile moves to another position are based only on
     * transition probabilities. Therefore, the mobile may not moveRelatively if
     * the inertia transition probability [1][1] is non-zero. The mobile may
     * also not moveRelatively if the force parameter is passed as false, in
     * which case it allows this mobile to moveRelatively if and only if a) this
     * mobile has remained connected for the minimum specified time and b) the
     * specified minimum simulation time before starting to moveRelatively has
     * elapsed.
     *
     *
     * overrideResidencePeriodInSCExpiration overrideStartRoamingTime
     *
     * @param overrideResidencePeriodInSCExpiration
     * @param overrideStartRoamingTime
     * @throws exceptions.WrongOrImproperArgumentException
     *
     * #resetStatus()
     *
     * @throws InvalidOrUnsupportedException UnsupportedOperationException
     * @return an update on the connectivity status
     *
     */
    public ConnectionStatusUpdate moveRelatively(
            boolean overrideResidencePeriodInSCExpiration,
            boolean overrideStartRoamingTime)
            throws InvalidOrUnsupportedException,
            UnsupportedOperationException,
            WrongOrImproperArgumentException,
            CriticalFailureException {

        if ((!overrideResidencePeriodInSCExpiration && !canMove())
                || (!overrideStartRoamingTime && !afterStartingRoaming())) {
            return _mostRecentConnStatusUpdate;
        }


        /* Point after moving. if looped to the other side of the area, 
         * the boolean field is true.
         */ Couple<Point, Boolean> newPointLoopedCouple = null;

        int pos = -1;
        double rand = getSimulation().getRandomGenerator().randProbability();
        double sum = 0;

        switch (_muTransitionDecisions) {
            case Values.PER_MU__PLUS__CENTRIFY__PLUS__CHANGE_DIRECTION:
                updtMoveDirectionClockwise();
            // no break! use the following code 
            case Values.PER_MU:
            case Values.PER_MU__PLUS__CENTRIFY:
                pos = _maxProbDirection;
                newPointLoopedCouple = selectNewCoordinate(pos);
                break;

            case Values.PER_MU__PLUS__RANDPROB:
                while (pos < _probsTransition.length) {
                    sum += _probsTransition[++pos];
                    if (sum >= rand) {
                        break;
                    }
                }

                newPointLoopedCouple = selectNewCoordinate(pos);
                break;

            case Values.PER_CELL_NEIGHBOURHOOD:
                CellRegistry cellRegistry = getSimulation().getCellRegistry();

                Map<Integer, Double> cell_probs = cellRegistry.getTransitionNeighborsOf(_currentlyConnectedSC);

                SmallCell chosenSC = null;
                //<editor-fold defaultstate="collapsed" desc="select a neighbor with uniformly random choice">
                Iterator<Integer> neighbIter = cell_probs.keySet().iterator();
                while (neighbIter.hasNext()) {
                    int theID = neighbIter.next();
                    chosenSC = cellRegistry.scByID(theID);

                    sum += cell_probs.get(chosenSC.getID());
                    if (sum >= rand) {
                        break;
                    }
                }
                //</editor-fold>
                newPointLoopedCouple = chosenSC == SmallCell.NONE ? null : new Couple(false, chosenSC.randomPointInCoverage());
                break;

            default:
                throw new UnsupportedOperationException("Option \"" + _muTransitionDecisions + "\" is not supported for MUs' transition decisions ");

        }

        return moveToNewRelativePoint(newPointLoopedCouple);
    }

    /**
     * Arranges connectivity status after mu moves to another position.
     *
     * @throws WrongOrImproperArgumentException
     * @throws InvalidOrUnsupportedException
     * @throws CriticalFailureException
     */
    private void connStatusUpdt(boolean isLooped)
            throws WrongOrImproperArgumentException,
            InvalidOrUnsupportedException,
            CriticalFailureException {
        Scenario scenario = getSimulation().getScenario();
        CellRegistry cellReg = getSimulation().getCellRegistry();

        SmallCell handoffSC = null;
        if (!this.getCoordinates().getCoveringSCs().isEmpty()) {
            List<String> handoffPolicy = scenario.parseConnPolicySC();

            handoffSC = CellUtilities.findHandoffcandidates(
                    cellReg, this, handoffPolicy);
        }

        if (handoffSC == null) {/*A: mobile not connected after moving*/
            _mostRecentConnStatusUpdate = connStatusHandoffCellNotExists(isLooped);
        } else /*B: mobile connected after moving*/ {
            _mostRecentConnStatusUpdate = connStatusHandoffCellExists(handoffSC, isLooped);
        }
    }

    private ConnectionStatusUpdate connStatusHandoffCellNotExists(boolean isLooped) {
//A: mobile not connected after moving, 

//A1: mobile not connected after moving, mobile not connected before moving 
        if (_currentlyConnectedSC == null) {
//A1.1: mobile not connected after moving, mobile not connected before moving, mobile was never previously connected                
            if (_lastKnownConnectedSC == null) {
                return updtTheMostRecentConnStatus(REMAINS_DISCONNECTED_WAS_NEVER_CONNECTED, null, isLooped);
            }
//A1.2: mobile not connected after moving, mobile not connected before moving, but was connected at some point in the past 
            // case lastKnownConnSC != null 
            return updtTheMostRecentConnStatus(REMAINS_DISCONNECTED_WAS_AT_SOME_POINT_CONNECTED, null, isLooped);
        }

//A2: mobile not connected after moving, mobile was connected before moving             
//if (_currentlyConnectedSC != null) 
//A2.1: mobile not connected after moving, mobile was connected before moving
        return updtTheMostRecentConnStatus(GOT_DISCONNECTED, null, isLooped);
    }

    private ConnectionStatusUpdate connStatusHandoffCellExists(SmallCell handoffSC, boolean isLooped) {

//B1: mobile connected after moving, mobile not connected before moving 
        if (_currentlyConnectedSC == null) {
            if (handoffSC.equals(_lastKnownConnectedSC)) {
                return updtTheMostRecentConnStatus(GOT_RECONNECTED_TO_SC_AFTER_TMP_DISCON, handoffSC, isLooped);
            }
//B1.1: mobile connected after moving, mobile not connected before moving, mobile was never previously connected                
            if (_previouslyConnectedSC == null) {
                return updtTheMostRecentConnStatus(CONNECTED_FIRST_TIME_TO_SC, handoffSC, isLooped);
            }
//B1.2: mobile connected after moving, mobile not connected before moving, but was connected at some point in the past connected
            // if (_previouslyConnectedSC != null) 
            return updtTheMostRecentConnStatus(HANDOVER_AFTER_DISCONNECTION_PERIOD, handoffSC, isLooped);
        }

//B2: mobile connected after moving, mobile was connected before moving             
//case _currentlyConnectedSC != null
//
//B2.1: mobile connected after moving, mobile was connected before moving
//case _currentlyConnectedSC.equals(lastKnownConnSC) 
//B2.1.1 mobile connected after moving, mobile was connected before moving to a DIFFERENT cell
        if (!_currentlyConnectedSC.equals(handoffSC)) {
            return updtTheMostRecentConnStatus(HANDOVER_DIRECTLY, handoffSC, isLooped);
        } else {
//B2.1.2 mobile connected to SAME cell before moving                    
            if (isLooped) {
                return updtTheMostRecentConnStatus(HANDOVER_DIRECTLY, handoffSC, isLooped);
            }
            return updtTheMostRecentConnStatus(REMAINS_CONNECTED_TO_SAME_SC, null, isLooped);
        }
    }

    private ConnectionStatusUpdate updtTheMostRecentConnStatus(ConnectionStatusUpdate connectionStatusUpdate, SmallCell sc, boolean isLooped) {
        this._mostRecentConnStatusUpdate = connectionStatusUpdate;

        switch (connectionStatusUpdate) {

            case REMAINS_DISCONNECTED_WAS_NEVER_CONNECTED:
            case REMAINS_DISCONNECTED_WAS_AT_SOME_POINT_CONNECTED:
            case REMAINS_CONNECTED_TO_SAME_SC:
                break;

            case CONNECTED_FIRST_TIME_TO_SC:
                // if first time do not stay still ... 
                // _residencePeriodInSCExpiration = simTime() + _userGroup.getMinResidenceTimeInSC();
                connectToSC(sc);

                break;

            case HANDOVER_AFTER_DISCONNECTION_PERIOD:
                _minResidencePeriodInSCExpiration = simTime() + getUserGroup().getResidenceDelayInSC();

                connectToSC(sc);

                updtLastHandoverDuration(_previouslyConnectedSC, sc, isLooped);

                getSimulation().addHaveHandedOver(this);

                break;

            case HANDOVER_DIRECTLY:
                _minResidencePeriodInSCExpiration = simTime() + getUserGroup().getResidenceDelayInSC();

                // stat is kept for the previously connected cell, aka the residence cell
                // caution! decide here before altering _previouslyConnectedSC and _lastKnownConnectedSC
                //  with disconnection/connection
                SmallCell originatingFromSC = _previouslyConnectedSC;//before the former
                SmallCell residentInSC = _lastKnownConnectedSC;// the former

                disconnectFromSC();
                connectToSC(sc);

                if (originatingFromSC != null) {// to avoid the case it was for first time connected
                    updtResidenceTime(originatingFromSC, residentInSC, isLooped); // call it here. 
                    // Must call after disconnection of residentInSC
                    // so to use correct times for disconnection
                }

                updtLastHandoverDuration(_previouslyConnectedSC, sc, isLooped);

                getSimulation().addHaveExitedPrevCell(this);
                getSimulation().addHaveHandedOver(this);

                break;

            case GOT_RECONNECTED_TO_SC_AFTER_TMP_DISCON:
                connectToSC(sc);
                break;

            case GOT_DISCONNECTED:
                _minHandoffPeriodInSCExpiration = simTime() + getUserGroup().getResidenceDelayInSC();
                ///////// keep order of invokations intact! ///////

                // stat is kept for previously previously connected cell, 
                // aka the residence cell
                originatingFromSC = _previouslyConnectedSC;
                residentInSC = _currentlyConnectedSC;

                disconnectFromSC();// disconnect in this order to avoid changing the _previouslyConnectedSC and _currentlyConnectedSC early

                if (originatingFromSC != null) {
                    // do it here to avoid case of reconnection to the same cell 
                    updtResidenceTime(originatingFromSC, residentInSC, isLooped);
                }

                getSimulation().addHaveExitedPrevCell(this);
                break;

            default:
                throw new UnsupportedOperationException("Case " + connectionStatusUpdate + " not known..");
        }

        return this._mostRecentConnStatusUpdate;
    }

    /**
     * Moves the mobile user to a particular point.
     *
     * newPoint isLooped
     *
     * @param newPoint
     * @param isLooped
     * @throws InconsistencyException upon passing a null newPoint parameter
     * value.
     * @throws exceptions.WrongOrImproperArgumentException
     * @throws exceptions.InvalidOrUnsupportedException
     *
     * @return either the connectivity change status after this mobile got
     * moved.
     */
    public ConnectionStatusUpdate moveForce(Point newPoint, boolean isLooped) throws InconsistencyException,
            WrongOrImproperArgumentException,
            InvalidOrUnsupportedException {

        _previousCoordinates = getCoordinates();

        getCoordinates().removeUser(this);

        _currentCoordinates = newPoint;
        newPoint.addUser(this);

        _penultimateTimeMoved = _lastTimeMoved;
        _lastTimeMoved = simTime();

        connStatusUpdt(isLooped);

        if (_mostRecentConnStatusUpdate.isHandedOver()) {
//////////// centrify if needed            
            if (getMuTransitionDecisions().equals(Values.PER_MU__PLUS__CENTRIFY)) {
                _currentCoordinates = _currentlyConnectedSC.getCenter();
                _currentlyConnectedSC.getCenter().addUser(this);
            }

//////////// updt local demand if no stationary requests used            
            if (!getSimulation().stationaryRequestsUsed()) {
                // in this case, local demand is defined by the connected mobiles
                _currentlyConnectedSC.getDmdLclForW().registerLclDmdForW(this, 1.0 / _currentlyConnectedSC.getConnectedMUs().size());
                _previouslyConnectedSC.getDmdLclForW().deregisterLclDmdForW(this, 1.0 / 1.0 / _previouslyConnectedSC.getConnectedMUs().size());
            }

//////////// take oracle cache decisions            
            Couple<Double, Double> residenceDuration = getSimulation().getCellRegistry().getResidenceDurationBetween(getUserGroup(),
                    _previouslyConnectedSC,
                    _currentlyConnectedSC, true
            );

//            long chunkSizeInBytes = getSim().chunkSizeInBytes();
//            double sliceSC = (double) getSim().getRateSCWlessInBytes() / getRequests().size();          
//            double howManyChunks = Math.round(sliceSC / chunkSizeInBytes);
//            for (DocumentRequest nxtReq : getRequests()) {
//                nxtReq.cacheForOracle(_currentlyConnectedSC);
////                nxtReq.cacheForOracle(_currentlyConnectedSC, residenceDuration, howManyChunks);
//            }
//////////// updt             
            incHandoffsPerformed();
            updtLastHandoverTime();
            updtHandoverProbs();

        }

        return _mostRecentConnStatusUpdate;
    }

    /**
     * Decides weather to change moving direction or not. If it does change
     * direction, then it makes a clockwise rotation of the transition
     * probability matrix until the the maxProbability value points at the new
     * direction.
     */
    public void updtMoveDirectionClockwise() {
        int pos = -1;
        double rand = getSimulation().getRandomGenerator().randProbability();
        double sum = 0;

        while (pos < _probsTransition.length) {
            sum += _probsTransition[++pos];
            if (sum >= rand) {
                break;
            }
        }

        while (pos != _maxProbDirection) {
            rotateProbsClockwise();
        }

    }

    public void rotateProbsClockwise() {
        double tmpProbs[] = new double[_probsTransition.length];
        System.arraycopy(_probsTransition, 0, tmpProbs, 0, _probsTransition.length);
        _probsTransition[0] = tmpProbs[3];
        _probsTransition[1] = tmpProbs[0];
        _probsTransition[2] = tmpProbs[1];
        _probsTransition[3] = tmpProbs[6];
//never changes     __probsTransition[4]= tmpProbs[4];
        _probsTransition[5] = tmpProbs[2];
        _probsTransition[6] = tmpProbs[7];
        _probsTransition[7] = tmpProbs[8];
        _probsTransition[8] = tmpProbs[5];

        switch (_maxProbDirection) {
            case 0:
                _maxProbDirection = 1;
                break;
            case 1:
                _maxProbDirection = 2;
                break;
            case 2:
                _maxProbDirection = 5;
                break;
            case 3:
                _maxProbDirection = 0;
                break;
            case 5:
                _maxProbDirection = 8;
                break;
            case 6:
                _maxProbDirection = 3;
                break;
            case 7:
                _maxProbDirection = 6;
                break;
            case 8:
                _maxProbDirection = 7;
                break;

        }

    }

    public SmallCell getLastKnownConnectedSC() {
        return this._lastKnownConnectedSC;
    }

    public SmallCell getPreviouslyConnectedSC() {
        return this._previouslyConnectedSC;
    }

    protected boolean canMove() {
        return simTime() >= _minResidencePeriodInSCExpiration
                && simTime() >= _minHandoffPeriodInSCExpiration;
    }

    /**
     * @return true if this MU has started (entered the) getSim().
     */
    public boolean afterStartingRoaming() {
        return simTime() >= _startRoamingTime;
    }

    public int getStartRoamingTime() {
        return _startRoamingTime;
    }

    public Point getCoordinatesPrevious() {
        return _previousCoordinates;
    }

    private void updtLastHandoverTime() {
        _lastHandoffTime = simTime();
    }

    private void updtHandoverProbs() {
        getSimulation().getCellRegistry().updtHandoffProbs(
                this,
                getPreviouslyConnectedSC(),
                getCurrentlyConnectedSC()
        );
    }

    /**
     * targetSC expectedResidenceDuration expectedHandoffDuration handoverProb
     *
     * @throws java.io.IOException
     * @throws InvalidOrUnsupportedException
     */
    private void cacheDescisionsPerformRegisterPC(
            SmallCell targetSC,
            double expectedResidenceDuration, double conf95ResidenceDur,
            double expectedHandoffDuration, double conf95HandoffDur,
            double handoverProb) throws Throwable {

        expectedResidenceDuration *= _mobAccuracy;
        conf95ResidenceDur *= _mobAccuracy;
        expectedHandoffDuration *= _mobAccuracy;
        conf95HandoffDur *= _mobAccuracy;

        setLastSCForCacheDecisions(targetSC);

//////////////////////// rates are split per each user's request
        float slice = this.getRequests().size();
        int mcRateSliceBytes = Math.round(getSimulation().getRateMCWlessInBytes() / slice);
        int scRateSliceBytes = Math.round(getSimulation().getRateSCWlessInBytes() / slice);
        int bhRateSliceBytes = Math.round(getSimulation().getRateBHInBytes() / slice);

///////////////////////select chunks and 
///////////////////////update popularity info for requests
        for (AbstractCachingPolicy policy : getCachingPolicies()) {
            if (policy instanceof MaxPop
                    || policy instanceof Oracle) {
                // cached object stay permanently in cache.
                continue;
            }

            List<Chunk> predictedChunks = new ArrayList();
            List<Chunk> predictedChunksNaive = new ArrayList();
            for (DocumentRequest nxtReq : getRequests()) {
// update popularity info for requests
                //targetSC.getPopInfo().registerPopInfo(nxtReq);
// select chunks
                predictedChunks.addAll(nxtReq.predictChunks2Request(//                                targetSC.getID() == 13 && policy == caching.incremental.EMC.instance(),
                        policy, handoverProb, isSoftUser(),
                        expectedHandoffDuration, conf95HandoffDur,
                        expectedResidenceDuration, conf95ResidenceDur, mcRateSliceBytes,
                        bhRateSliceBytes,
                        scRateSliceBytes)
                );
                predictedChunksNaive.addAll(nxtReq.predictChunks2Request(//                                false,
                        policy, handoverProb, isSoftUser(),
                        getSimulation().getRandomGenerator().
                        randDoubleInRange(0, expectedHandoffDuration),
                        0,
                        getSimulation().getRandomGenerator().
                        randDoubleInRange(0, expectedResidenceDuration),
                        0,
                        mcRateSliceBytes,
                        bhRateSliceBytes,
                        scRateSliceBytes)
                );

                for (Chunk nxtChunk : predictedChunks) {
                    targetSC.getDmdPC(policy).registerUpdtInfoPC(nxtChunk, this, handoverProb);
                }

///////////////////////take cache decisions    
                targetSC.cacheDecisions(policy, this, targetSC,
                        predictedChunks, predictedChunks);
            }// FOR EVERY REQUEST
        }// FOR EVERY POLICY
    }

    public int getLastHandoffTime() {
        return _lastHandoffTime;
    }

    public int getLastHandoverDuration() {
        return _lastHandoverDuration;
    }

    public int getLastResetStatusTime() {
        return _lastResetStatusTime;
    }

    /**
     * Changes the gaussianVelocity of this MU based on its group of mobiles
     * setup.
     *
     * @return the gaussianVelocity
     */
    public double changeVelocity() {
        return getUserGroup().gaussianVelocity();
    }

    /**
     * The transition probabilities of this MU's. Modifying this array
     * externally affects this MU's transition probabilities.
     *
     * @return the transition probabilities of this MU.
     */
    public double[] transitionProbabilities() {
        return this._probsTransition;
    }

    @Override
    public String toString() {
        StringBuilder _toString = new StringBuilder();

        _toString.append(super.toString());

        _toString.append(", ");
        _toString.append("_lastTimeMoved=");
        _toString.append(this._lastTimeMoved);

        _toString.append(", ");
        _toString.append("_handoffsPerformed=");
        _toString.append(this._handoffsPerformed);

        _toString.append(", ");
        _toString.append("_mostRecentConnStatusUpdate=");
        _toString.append(this._mostRecentConnStatusUpdate);

        _toString.append(", ");
        _toString.append("_lastHandoffDuration=");
        _toString.append(this._lastHandoverDuration);

        _toString.append(", ");
        _toString.append("_lastHandoffTime=");
        _toString.append(this._lastHandoffTime);

        _toString.append(", ");
        _toString.append("_mobilityAccuracy=");
        _toString.append(this._mobAccuracy);

        _toString.append(", ");
        _toString.append("_lastKnownConnectedSC=");
        if (_lastKnownConnectedSC != null) {
            _toString.append(this._lastKnownConnectedSC.toSynopsisString());
        } else {
            _toString.append("NULL");
        }

        _toString.append(", ");
        _toString.append("_lastKnownConnectedSCPoint=");
        if (_lastKnownConnectedSCPoint != null) {
            _toString.append(this._lastKnownConnectedSCPoint.toSynopsisString());
        } else {
            _toString.append("NULL");
        }

        _toString.append(", ");
        _toString.append("_previouslyConnectedSC=");
        if (_previouslyConnectedSC != null) {
            _toString.append(this._previouslyConnectedSC.toSynopsisString());
        } else {
            _toString.append("NULL");
        }

        _toString.append(", ");
        _toString.append("_previousCoordinates=");
        if (_previousCoordinates != null) {
            _toString.append(this._previousCoordinates.toSynopsisString());
        } else {
            _toString.append("NULL");
        }

        _toString.append(", ");
        _toString.append("_currentCoordinates=");
        if (getCoordinates() != null) {
            _toString.append(this.getCoordinates().toSynopsisString());
        } else {
            _toString.append("NULL");
        }

        _toString.append(", ");
        _toString.append("_lastResetSC=");
        if (_lastResetSC != null) {
            _toString.append(this._lastResetSC.toSynopsisString());
        } else {
            _toString.append("NULL");
        }

        _toString.append(", ");
        _toString.append("_lastResetStatusTime=");
        _toString.append(this._lastResetStatusTime);

        return _toString.toString();
    }

    public void setReqsConsumeReady() {
        for (DocumentRequest r : getRequests()) {
            r.setConsumeReady(true);
        }

    }

    /**
     * @return the _mobAccuracy
     */
    public double getMobAccuracy() {
        return _mobAccuracy;
    }

    /**
     * @return the _softUser
     */
    public boolean isSoftUser() {
        return _softUser;
    }

    public boolean isHardUser() {
        return !_softUser;
    }

}
