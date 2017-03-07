package sim.space.users;

import static app.properties.Space.SC__DMD__TRACE__STATIONARY_REQUESTS__REQ2CACHE;
import caching.MaxPop;
import caching.base.AbstractCachingPolicy;
import exceptions.InvalidOrUnsupportedException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import sim.run.SimulationBaseRunner;
import sim.content.Chunk;
import sim.content.request.DocumentRequest;
import sim.space.Point;
import sim.space.cell.MacroCell;
import sim.space.cell.smallcell.SmallCell;

/**
 *
 * @author xvas
 */
public class StationaryUser extends CachingUser {

    public StationaryUser(int id, SimulationBaseRunner sim,
            Collection<AbstractCachingPolicy> cachingPolicies) throws InvalidOrUnsupportedException {

        super(id, sim, cachingPolicies);
        _allowedToCache = sim.getScenario().isTrue(SC__DMD__TRACE__STATIONARY_REQUESTS__REQ2CACHE);
    }

    public StationaryUser(int id, SimulationBaseRunner sim, int connectedSinceSC,
            SmallCell connectionSC, MacroCell connectionMC,
            Collection<AbstractCachingPolicy> cachingPolicies) throws InvalidOrUnsupportedException {

        super(id, sim, connectedSinceSC, connectionSC, connectionMC,
                cachingPolicies);
        _allowedToCache = sim.getScenario().isTrue(SC__DMD__TRACE__STATIONARY_REQUESTS__REQ2CACHE);
    }

    /**
     * This is a stationary user. Thus, the default getCoordinates are the
     * getCoordinates of the center of the connection cell.
     *
     * @return the getCoordinates of the center of the connection cell
     */
    @Override
    public Point getCoordinates() {
        return getCurrentlyConnectedSC().getCenter();
    }

    @Override
    public void addAllRequests(Collection<DocumentRequest> requests) throws Throwable {
        for (DocumentRequest r : requests) {
            addRequest(r);
        }
    }

    /**
     * try to cache whatever not already in the cache that you just downloaded.
     *
     * @throws Throwable
     */
    public void tryCacheRecentFromBH() throws Throwable {

        // we care to request to tryCacheRecentFromBH only the ones not hit in the tryCacheRecentFromBH, i.e:
        // 1) the ones from the BH
        Map<AbstractCachingPolicy, List<Chunk>> mostRecentlyConsumedBH
                = getMostRecentlyConsumedBH();

//commented out the following
        for (AbstractCachingPolicy policy : getCachingPolicies()) {
            if (policy instanceof MaxPop //                    || policy instanceof Oracle
                    ) {
                // cached object stay permanently in tryCacheRecentFromBH.
                continue;
            }

            List<Chunk> recentlyConsumedLst = new ArrayList();
            recentlyConsumedLst.addAll(mostRecentlyConsumedBH.get(policy));

            _currentlyConnectedSC.cacheDecisions(policy, this, _currentlyConnectedSC,
                    recentlyConsumedLst, recentlyConsumedLst /* Otherwise it benefits naive too much */
            );

        }
    }

    public void forceCompleteRequests() {
        for (DocumentRequest r : getRequests()) {
            r.forceComplete(simTime());
        }
    }

    @Override
    public void addRequest(DocumentRequest r) throws Throwable {
        super.addRequest(r);
        _currentlyConnectedSC.getPopInfo().registerPopInfo(r);
    }
}
