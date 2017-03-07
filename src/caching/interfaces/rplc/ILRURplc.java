package caching.interfaces.rplc;

import java.util.Set;
import sim.content.Chunk;
import sim.space.cell.smallcell.ITimeBuffer;
import sim.space.cell.smallcell.SmallCell;
import sim.space.users.CachingUser;

/**
 * Do not use until checking functionality
 *
 * Base interface for methods using LRU cache replacement.
 *
 * @author xvas
 */
@Deprecated
public interface ILRURplc extends IRplcBase {

    /**
     * Proposes items for eviction according to some LRU implementation
     *
     * @param evictionBuffer
     * @param item the item requesting to be cached
     * @return the set of proposed evicted items
     *
     */
    public Set<Chunk> optForEviction(ITimeBuffer evictionBuffer, Chunk item) throws Throwable;

    /**
     *
     * @param mu
     * @param sc
     * @return the items replaced
     * @throws Throwable
     */
    public Set<Chunk> cacheDecision(CachingUser mu, SmallCell sc) throws Throwable;

}
