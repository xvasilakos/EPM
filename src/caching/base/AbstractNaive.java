package caching.base;

import sim.space.cell.smallcell.PricedBuffer;

/**
 * Naive Caching base.
 *
 * @author xvas
 */
public abstract class AbstractNaive extends AbstractCachingPolicy {

      
    public static Class bufferType() {
        return PricedBuffer.class;//giati? dioti mallon ayto ftaiei
    }
    
    protected AbstractNaive() {
    }

}
