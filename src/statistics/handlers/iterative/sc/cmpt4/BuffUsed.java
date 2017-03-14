package statistics.handlers.iterative.sc.cmpt4;

import caching.base.AbstractCachingModel;
import sim.space.cell.smallcell.SmallCell;

/**
 * @author Xenofon Vasilakos xvas@aueb.gr
 */
public class BuffUsed extends ComputeAllPoliciesImpl {

   public BuffUsed(AbstractCachingModel cachingMethodUsed) {
      super(cachingMethodUsed);
   }

   @Override
   public String title() {
      return "BF_USED(" + getCachingMethod() + ")";
   }

   @Override
   public double compute4(SmallCell cell)  {
         return cell.buffUsed(getCachingMethod())/1024.0/1024.0;// in MBs
   }

}
