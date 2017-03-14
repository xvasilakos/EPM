package caching.base;

import sim.space.cell.smallcell.BufferBase;

/**
 * A common ancestor base class for caching methods. The cacheDecision()
 * implementation is left to the classes extending this class.
 *
 * @author Xenofon Vasilakos xvas@aueb.gr
 */
public abstract class AbstractCachingModel {

    
    
    public static Class bufferType() {
        return BufferBase.class;
    }

    /**
     * @return A string representation derived by the canonical name of this object's
     * class or its decedent class objects, which is equivalent to calling getClass().getCanonicalName(). 
     */
   @Override
    public final String toString() {
        return getClass().getCanonicalName();
    }

    /**
     *
     * @param otherModel
     * @return
     */
    @Override
    public boolean equals(Object otherModel) {
        return toString().equals(otherModel.toString()) && otherModel instanceof AbstractCachingModel;
    }

    @Override
    public final int hashCode() {
        return toString().hashCode();
    }

    public String nickName() {
        return toString().substring(8);
    }

}
