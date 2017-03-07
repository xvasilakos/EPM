/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sim.space.users;

import exceptions.InconsistencyException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import sim.ISimulationMember;
import utils.ISynopsisString;
import sim.run.SimulationBaseRunner;
import sim.space.ISpaceMember;
import sim.content.request.DocumentRequest;
import sim.content.Chunk;
import sim.space.cell.CellRegistry;
import sim.space.cell.MacroCell;
import sim.space.cell.smallcell.SmallCell;

/**
 *
 * @author xvas
 */
public abstract class User implements ISimulationMember, ISpaceMember, ISynopsisString {

    private final SimulationBaseRunner<?> _simulation;
    private final List<DocumentRequest> _requests;
    private final List<Chunk> _requestsInChunks;
    private double _lastResidenceDuration;

    private UserGroup _userGroup;
    protected int _lastTimeReqsUpdt;
    protected final int _id;
    protected MacroCell _currConnectedMC;
    protected SmallCell _currentlyConnectedSC;
    protected int _connectedSinceSC;

    protected User(int id, SimulationBaseRunner<?> sim) {
        _id = id;
        _simulation = sim;

        _requests = new ArrayList<>();
        _requestsInChunks = new ArrayList<>();

        _lastResidenceDuration = -1;

    }

    public User(int id, SimulationBaseRunner<?> sim, int connectedSinceSC, SmallCell connectionSC, MacroCell connectionMC) {
        _requests = new ArrayList<>();
        _requestsInChunks = new ArrayList<>();
        _id = id;
        _lastTimeReqsUpdt = -1;
        _simulation = sim;
        _connectedSinceSC = connectedSinceSC;
        _currConnectedMC = connectionMC;
        _currConnectedMC.connectUser(this);
        _currentlyConnectedSC = connectionSC;
        connectionSC.connectUser(this);

        _lastResidenceDuration = -1;
    }

    public boolean isConnected() {
        return _currentlyConnectedSC != null;
    }

    @Override
    public boolean equals(Object b) {
        if (b == null) {
            return false;
        }
        if (b.getClass() != this.getClass()) {
            return false;
        }
        User u = (User) b;
        return u._id == this._id;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 89 * hash + this._id;
        return hash;
    }

    @Override
    public final SimulationBaseRunner<?> getSim() {
        return _simulation;
    }

    @Override
    public final int simTime() {
        return getSim().simTime();
    }

    @Override
    public String simTimeStr() {
        return "[" + simTime() + "]";
    }

    @Override
    public final int simID() {
        return getSim().getID();
    }

    @Override
    public final int getX() {
        return getCoordinates().getX();
    }

    @Override
    public final int getY() {
        return getCoordinates().getY();
    }

    @Override
    public String toString() {
        return "User{"
                + "id=" + _id
                + "@<" + getX() + "," + getY() + ">"
                + ", _simulation=" + _simulation
                + ", _requests=" + _requests
                + ", _requestsInChunks=" + _requestsInChunks
                + ", _lastResidenceDuration=" + _lastResidenceDuration
                + ", _userGroup=" + _userGroup
                + ", _lastTimeReqsUpdt=" + _lastTimeReqsUpdt
                + ", _currConnectedMC=" + _currConnectedMC
                + ", _currentlyConnectedSC=" + _currentlyConnectedSC
                + ", _connectedSinceSC=" + _connectedSinceSC + '}';
    }

    @Override
    public String toSynopsisString() {
        return "User{"
                + "id=" + _id
                + "@<" + getX() + "," + getY() + ">"
                + ", _simulation=" + _simulation.getID()
                + ", _requests=" + _requests.size()
                + ", _requestsInChunks=" + _requestsInChunks.size()
                + ", _lastResidenceDuration=" + _lastResidenceDuration
                + ", _userGroup=" + _userGroup.getId()
                + ", _lastTimeReqsUpdt=" + _lastTimeReqsUpdt
                + (_currConnectedMC!=null ? ", _currConnectedMC=" + _currConnectedMC.getID() : "NONE")
                + (_currentlyConnectedSC!=null ? ", _currentlyConnectedSC=" + _currentlyConnectedSC.getID() : "NONE")
                + ", _connectedSinceSC=" + _connectedSinceSC + '}';
    }

    public double getLastResidenceDuration() {
        return _lastResidenceDuration;
    }

    public void setLastResidenceDuration(double duration) {
        _lastResidenceDuration = duration;
    }

    @Override
    public final CellRegistry simCellRegistry() {
        return getSim().getCellRegistry();
    }

    public void updtLastTimeReqsUpdt() {
        _lastTimeReqsUpdt = simTime();
    }

    public final int getLastTimeReqsUpdt() {
        return _lastTimeReqsUpdt;
    }

    public final boolean hasRequests() {
        return !_requests.isEmpty();
    }

    public final int getID() {
        return _id;
    }

    public void connectToSC(SmallCell sc) {
        _currentlyConnectedSC = sc;
        _currentlyConnectedSC.connectUser(this);
    }

    public final void connectToMC(MacroCell mc) {
        _currConnectedMC = mc;
        _currConnectedMC.connectUser(this);
    }

    public SmallCell disconnectFromSC() {
        if (_currentlyConnectedSC == null) {
            /*
             * Check connectivity status before invoking this method.
             * CAUTION this is an important check otherwise it will
             * erase lastKnownConnectedSC too.
             */
            throw new InconsistencyException("Cannot disconnect from a null cell.");
        }
        this._connectedSinceSC = -1;
        SmallCell lastKnownConnectedSC = this._currentlyConnectedSC;
        this._currentlyConnectedSC.disconnectUser(this);
        this._currentlyConnectedSC = null;

        return lastKnownConnectedSC;
    }

    private MacroCell disconnectMC() {
        MacroCell prev = _currConnectedMC;
        // prev.unregisterDmdLocal(this);//@todo uncomment this part. it is taken out to save resources at simulation stage
        this._currConnectedMC = null;
        return prev;
    }

    /**
     * MobileUser#alterRequests()
     *
     * @return returns a unmodifiable version of the requests by this mobile
     * user.
     */
    public final Collection<DocumentRequest> getRequests() {
        return Collections.unmodifiableList(_requests);
    }

    public final int clearAllRequests() {
        int cleared = _requests.size();
        _requests.clear();
        _requestsInChunks.clear();

        return cleared;
    }

    public final int clearCompletedRequests() {
        int cleared = _requests.size();

        Iterator<DocumentRequest> iterator = _requests.iterator();
        while (iterator.hasNext()) {
            DocumentRequest nxtRequest = iterator.next();
            if (nxtRequest.isFullyConsumed()) {
                iterator.remove();
                _requestsInChunks.removeAll(nxtRequest.referredContentDocument().chunks());
            }
        }

        cleared -= _requests.size();

        return cleared;
    }

    /**
     *
     * r item.
     *
     * @throws java.lang.Throwable
     */
    public void addRequest(DocumentRequest r) throws Throwable {
        _requests.add(r);
        _requestsInChunks.addAll(r.referredContentDocument().chunks());
    }

    /**
     *
     * requests
     *
     * @throws java.lang.Throwable
     */
    public void addAllRequests(Collection<DocumentRequest> requests) throws Throwable {
        for (DocumentRequest r : requests) {
            addRequest(r);
        }
    }

    public final SmallCell getCurrentlyConnectedSC() {
        return this._currentlyConnectedSC;
    }

    public final MacroCell getCurrentMacroCellConnection() {
        return this._currConnectedMC;
    }

    public final int getGroupID() {
        return _userGroup.getId();
    }

    public UserGroup getUserGroup() {
        return _userGroup;
    }

    public final int getConnectedSinceSC() {
        return _connectedSinceSC;
    }

    public abstract void consumeDataTry(int timeWindow) throws Throwable;
    public abstract void consumeTryAllAtOnceFromSC() throws Throwable;

    /**
     * _userGroup the _userGroup to set
     */
    protected final void setUserGroup(UserGroup _userGroup) {
        this._userGroup = _userGroup;
    }

}
