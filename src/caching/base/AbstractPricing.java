package caching.base;

import sim.space.cell.smallcell.PricedBuffer;

/**
 * Base abstract class in the hierarchy of congestion pricing caching methods.
 *
 * @author Xenofon Vasilakos xvas@aueb.gr
 */
public abstract class AbstractPricing extends AbstractCachingPolicy {

    public static Class bufferType() {
        return PricedBuffer.class;
    }

    
}
