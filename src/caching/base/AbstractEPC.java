package caching.base;

import sim.content.Chunk;
import sim.space.cell.smallcell.SmallCell;
import sim.space.users.CachingUser;

/**
 * Efficient Proactive Caching base class.
 *
 * Provides an implementation for the cacheDecision() and assess() methods. All
 * EPC method class must derive from this class.
 *
 * @author xvas
 */
public abstract class AbstractEPC extends AbstractPricingModel {

    /**
     * @param chnk the chunk to be assessed
     * @param cu the requesting/caching user
     * @param sc the small cell for which the chunks are assessed
     * @return the assessed gain
     */
    public abstract double assess(CachingUser cu, Chunk chnk, SmallCell sc) ;

}
