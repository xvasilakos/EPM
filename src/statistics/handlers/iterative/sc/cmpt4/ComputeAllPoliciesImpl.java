package statistics.handlers.iterative.sc.cmpt4;

import caching.base.AbstractCachingModel;
import sim.space.cell.smallcell.SmallCell;
import statistics.handlers.BaseHandler;
import statistics.handlers.ICompute4;

/**
 * @author Xenofon Vasilakos xvas@aueb.gr
 */
public abstract class ComputeAllPoliciesImpl extends BaseHandler implements ICompute4<SmallCell>{
   private final AbstractCachingModel _cachingMethod;

   public ComputeAllPoliciesImpl(AbstractCachingModel cachingMethod) {
      super();
      _cachingMethod = cachingMethod;
   }
  

   /**
    * @return the caching method
    */
   public AbstractCachingModel getCachingMethod() {
      return _cachingMethod;
   }

}
