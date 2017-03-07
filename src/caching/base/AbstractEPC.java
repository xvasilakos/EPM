package caching.base;

import caching.Utils;
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
public abstract class AbstractEPC extends AbstractPricing {

    /**
     * @param item
     * @param cu
     * @param sc
     * @return the delay gain weighted by the transition probability
     */
    public double assess(CachingUser cu, Chunk item, SmallCell sc) {
       return Utils.assessEPC(cu, item, sc);
    }

}
