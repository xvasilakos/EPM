/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sim.space.users;

import caching.base.AbstractCachingPolicy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import sim.run.SimulationBaseRunner;
import sim.content.Chunk;
import sim.content.request.DocumentRequest;
import sim.space.cell.MacroCell;
import sim.space.cell.smallcell.SmallCell;

/**
 *
 * @author xvas
 */
public abstract class CachingUser extends User {

    private final Collection<AbstractCachingPolicy> _cachingPolicies;
    private SmallCell _lastSCForCacheDecisions;
    private final Map<AbstractCachingPolicy, List<Chunk>> _mostRecentlyConsumedMC;
    private final Map<AbstractCachingPolicy, List<Chunk>> _mostRecentlyConsumedFromCacheHits;
    private final Map<AbstractCachingPolicy, List<Chunk>> _mostRecentlyConsumedBH;
    private final Map<AbstractCachingPolicy, List<Chunk>> _mostRecentlyCacheMissesPerPolicy;
    protected boolean _allowedToCache;

    protected CachingUser(int id, SimulationBaseRunner<?> sim,
            Collection<AbstractCachingPolicy> cachingPolicies) {

        super(id, sim);
        this._allowedToCache = true; // by default, true. Let subtypes redefine this filed

        _lastSCForCacheDecisions = null;
        _cachingPolicies = cachingPolicies;

        _mostRecentlyConsumedMC = new HashMap<>(5);
        _mostRecentlyConsumedFromCacheHits = new HashMap<>(5);
        _mostRecentlyConsumedBH = new HashMap<>(5);
        _mostRecentlyCacheMissesPerPolicy = new HashMap<>(5);

        if (sim != null) {// dummy user
            for (AbstractCachingPolicy policy : sim.getCachingPolicies()) {
                _mostRecentlyConsumedMC.put(policy, new ArrayList<Chunk>());
                _mostRecentlyConsumedFromCacheHits.put(policy, new ArrayList<Chunk>());
                _mostRecentlyConsumedBH.put(policy, new ArrayList<Chunk>());
                _mostRecentlyCacheMissesPerPolicy.put(policy, new ArrayList<Chunk>());
            }
        }
    }

    protected CachingUser(int id, SimulationBaseRunner<?> sim, int connectedSinceSC, SmallCell connectionSC,
            MacroCell connectionMC,
            Collection<AbstractCachingPolicy> cachingPolicies) {

        super(id, sim, connectedSinceSC, connectionSC, connectionMC);
        this._allowedToCache = true; // by default, true. Let subtypes redefine this filed

        this._lastSCForCacheDecisions = null;
        _cachingPolicies = cachingPolicies;

        _mostRecentlyConsumedMC = new HashMap<>(5);
        _mostRecentlyConsumedFromCacheHits = new HashMap<>(5);
        _mostRecentlyConsumedBH = new HashMap<>(5);
        _mostRecentlyCacheMissesPerPolicy = new HashMap<>(5);

        if (sim != null) {// dummy user
            for (AbstractCachingPolicy policy : sim.getCachingPolicies()) {
                _mostRecentlyConsumedMC.put(policy, new ArrayList<Chunk>());
                _mostRecentlyConsumedFromCacheHits.put(policy, new ArrayList<Chunk>());
                _mostRecentlyConsumedBH.put(policy, new ArrayList<Chunk>());
                _mostRecentlyCacheMissesPerPolicy.put(policy, new ArrayList<Chunk>());
            }
        }
    }

    /**
     * @return the _lastSCForCacheDecisions
     */
    public SmallCell getLastSCForCacheDecisions() {
        return _lastSCForCacheDecisions;
    }

    /**
     * _lastSCForCacheDecisions the _lastSCForCacheDecisions to set
     */
    public void setLastSCForCacheDecisions(SmallCell _lastSCForCacheDecisions) {
        this._lastSCForCacheDecisions = _lastSCForCacheDecisions;
    }

    /**
     * @return the _cachingPolicies
     */
    public Collection<AbstractCachingPolicy> getCachingPolicies() {
        return _cachingPolicies;
    }

    public void consumeRemainderFromMC() {
        double slices = this instanceof StationaryUser ? 1
                : getRequests().size();

        double mcRateSlice = Math.round((double) getSim().getRateMCWlessInBytes() / slices);

        for (DocumentRequest nxtRequest : getRequests()) {
            for (AbstractCachingPolicy policy : getSim().getCachingPolicies()) {
                nxtRequest.consumeChunksRemainderFromMC(policy, mcRateSlice, _mostRecentlyConsumedMC);
            }
        }

    }

    @Override
    public void consumeDataTry(int timeWindow) throws Throwable {
        // because one stationary emulates multiple stationary users with one request
        double slices = this instanceof StationaryUser ? 1
                : getRequests().size();

        // equal (fair) slicing of rates to each request
        // caution, use double numbers to keep precision after divisions
        double mcRateSlice = Math.round((double) timeWindow * getSim().getRateMCWlessInBytes() / slices);
        double scRateSlice = Math.round((double) timeWindow * getSim().getRateSCWlessInBytes() / slices);
        double bhRateSlice = Math.round((double) timeWindow * getSim().getRateBHInBytes() / slices);
        bhRateSlice = Math.min(bhRateSlice, scRateSlice); // you download with minimum flow from the BH+SC network.

        // clear from previous move
        for (List<Chunk> recentChunks : _mostRecentlyConsumedMC.values()) {
            recentChunks.clear();
        }
        for (List<Chunk> recentChunks : _mostRecentlyConsumedFromCacheHits.values()) {
            recentChunks.clear();
        }
        for (List<Chunk> recentChunks : _mostRecentlyConsumedBH.values()) {
            recentChunks.clear();
        }
        for (List<Chunk> recentChunks : _mostRecentlyCacheMissesPerPolicy.values()) {
            recentChunks.clear();
        }

        for (DocumentRequest nxtRequest : getRequests()) {
            nxtRequest.consumeTry(mcRateSlice, _mostRecentlyConsumedMC,
                    scRateSlice, _mostRecentlyConsumedFromCacheHits,
                    bhRateSlice, _mostRecentlyConsumedBH,
                    _mostRecentlyCacheMissesPerPolicy
            );
        }
    }
    
    @Override
    public void consumeTryAllAtOnceFromSC() throws Throwable {
        // because one stationary emulates multiple stationary users with one request
        double slices = this instanceof StationaryUser ? 1
                : getRequests().size();

        
        // clear from previous move
        for (List<Chunk> recentChunks : _mostRecentlyConsumedMC.values()) {
            recentChunks.clear();
        }
        for (List<Chunk> recentChunks : _mostRecentlyConsumedFromCacheHits.values()) {
            recentChunks.clear();
        }
        for (List<Chunk> recentChunks : _mostRecentlyConsumedBH.values()) {
            recentChunks.clear();
        }
        for (List<Chunk> recentChunks : _mostRecentlyCacheMissesPerPolicy.values()) {
            recentChunks.clear();
        }

        for (DocumentRequest nxtRequest : getRequests()) {
            nxtRequest.consumeTryAllAtOnceFromSC(
                    _mostRecentlyConsumedFromCacheHits,
                    _mostRecentlyConsumedBH,
                    _mostRecentlyCacheMissesPerPolicy
            );
            
        }
    }

    /**
     * @return the _mostRecentlyConsumedMC
     */
    public Map<AbstractCachingPolicy, List<Chunk>> getMostRecentlyConsumedMC() {
        return _mostRecentlyConsumedMC;
    }

    /**
     * @return the _mostRecentlyConsumedFromCacheHits
     */
    public Map<AbstractCachingPolicy, List<Chunk>> getMostRecentlyConsumedFromCacheHits() {
        return _mostRecentlyConsumedFromCacheHits;
    }

    /**
     * @return the _mostRecentlyConsumedBH
     */
    public Map<AbstractCachingPolicy, List<Chunk>> getMostRecentlyConsumedBH() {
        return _mostRecentlyConsumedBH;
    }

    /**
     * @return the _mostRecentlyCacheMissesPerPolicy
     */
    public Map<AbstractCachingPolicy, List<Chunk>> getMostRecentlyCacheMissesPerPolicy() {
        return _mostRecentlyCacheMissesPerPolicy;
    }

    public boolean isAllowedToCache() {
        return _allowedToCache;
    }

}
