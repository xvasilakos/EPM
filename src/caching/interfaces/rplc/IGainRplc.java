package caching.interfaces.rplc;

import exceptions.CriticalFailureException;
import java.util.Collection;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Set;
import sim.run.SimulationBaseRunner;
import sim.content.Chunk;
import sim.space.cell.smallcell.SmallCell;
import sim.space.users.CachingUser;

/**
 * Base interface for methods with cache replacement.
 *
 * @author xvas
 */
public interface IGainRplc extends IRplcBase {

    /**
     * Computes and returns the delay gain weighted by the local popularity of
     * the item.
     *
     * @param item
     * @param sc
     * @return
     *
     */
    public abstract double assess(Chunk item, SmallCell sc) throws Throwable;

    /**
     * Proposes items from eviction that are currently cached in the buffer of
     * small cell according to the implemented cache replacement policy. More
     * cached items are added to the proposed set of items that are opt for
     * eviction until there is enough free space in the buffer for the item
     * requesting to be cached, and provided that the aggregate gain over size
     * for the evicted items is less than the gain over the size of the item to
     * add.
     *
     * If any of the former conditions is not met, then the method returns an
     * empty set so that no items are proposed for eviction.
     *
     * @param sc
     * @param chunk
     * @param orderedCached
     * @return the set of evicted items
     *
     */
    public Set<Chunk> optForEviction(SmallCell sc, Chunk chunk, PriorityQueue<Chunk> orderedCached) throws Throwable;

    /**
     * Takes cache decisions for a small cell regarding the request by mobile
     * user mu that is currently hosted at small cell scHost.
     *
     *
     *
     * @param sim
     * @param mu the mobile user.
     * @param requestChunks
     * @param hostSC
     * @param targetSC
     * @param chunksRplcd
     * @param cachedOrderByGain the value of cachedOrderByGain
     *
     * @return the int
     *
     */
    public int cacheDecision(SimulationBaseRunner sim, CachingUser mu, Collection<Chunk> requestChunks, SmallCell hostSC, SmallCell targetSC, Set<Chunk> chunksRplcd, PriorityQueue<Chunk> cachedOrderByGain) throws Throwable;

    public Comparator<Chunk> evictionPriorityComparator(final SmallCell sc) throws CriticalFailureException;

}
