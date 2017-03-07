package caching.interfaces.rplc;

import sim.content.Chunk;
import sim.space.cell.smallcell.SmallCell;

/**
 * Base interface for methods using LRU cache replacement.
 *
 * @author xvas
 */
public interface ILRUPricedRplc extends ILRURplc {

    /**
     * Computes and returns the delay gain weighted by the local popularity of
     * the item.
     *
     * @param chunk
     * @param sc
     * @return
     *
     */
    public abstract double assess(Chunk chunk, SmallCell sc) throws Throwable;

}
