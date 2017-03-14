package sim.space.users.mobile;

import caching.base.AbstractCachingPolicy;
import exceptions.InconsistencyException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import sim.run.SimulationBaseRunner;
import sim.content.request.DocumentRequest;
import sim.space.Area;
import sim.space.Point;

/**
 *
 * @author xvas
 */
public abstract class Builder {

    protected final SimulationBaseRunner __simulation;
    protected final MobileGroup __group;
    protected final Point __startCoordinates;
    protected final List<String> __connectionPolicySC;
    protected final List<DocumentRequest> __requests;

    protected int __id = MobileUser.ID_UNDEFINED;

    protected Area __area;
    protected String __transitionDecisions;

    protected Collection<AbstractCachingPolicy> _cachingPolicies;

    public Builder(
            SimulationBaseRunner simulation, MobileGroup group,
            Point startPoint,
            List<String> connectionPolicySC,
            Collection<AbstractCachingPolicy> cachingPolicies) {
        __simulation = simulation;

        __group = group;

        this.__startCoordinates = startPoint;
        if (this.__startCoordinates == null) {
            throw new InconsistencyException(
                    "MU that is a member of group " + __group.getId() + " must have a start point defined within area."
            );
        }

        this.__connectionPolicySC = connectionPolicySC;

        this.__requests = new ArrayList<>();

        this._cachingPolicies = cachingPolicies;
    }

    public Builder setId(int id) {
        this.__id = id;
        return this;
    }

    public Builder setArea(Area area) {
        this.__area = area;
        return this;
    }

    public Builder setTransitionDecisions(String transitionDecisions) {
        this.__transitionDecisions = transitionDecisions;
        return this;
    }

    public Builder setRequests(Set<DocumentRequest> requests) {
        this.__requests.addAll(requests);
        return this;
    }

    public Builder setRequests(DocumentRequest... requests) {
        this.__requests.addAll(Arrays.asList(requests));
        return this;
    }

    public abstract MobileUser build();

}
