package statistics.handlers.iterative.sc.cmpt4;

import caching.base.AbstractCachingPolicy;
import sim.space.cell.smallcell.SmallCell;
import statistics.handlers.BaseHandler;
import statistics.handlers.ICompute4;

/**
 * @author Xenofon Vasilakos xvas@aueb.gr
 */
public abstract class ComputeAllPoliciesImpl extends BaseHandler implements ICompute4<SmallCell>{
   private final AbstractCachingPolicy _cachingMethod;

   public ComputeAllPoliciesImpl(AbstractCachingPolicy cachingMethod) {
      super();
      _cachingMethod = cachingMethod;
   }
  

   /**
    * @return the caching method
    */
   public AbstractCachingPolicy getCachingMethod() {
      return _cachingMethod;
   }

}
