package statistics.handlers.iterative.sc.cmpt4;

import caching.base.AbstractCachingModel;
import caching.base.AbstractPricingModel;
import caching.interfaces.rplc.IGainRplc;
import sim.space.cell.smallcell.PricedBuffer;
import sim.space.cell.smallcell.SmallCell;
import statistics.StatisticException;

/**
 *
 * @author Xenofon Vasilakos xvas@aueb.gr
 */
public class Price extends ComputeAllPoliciesImpl {

   public Price(AbstractCachingModel cachingMethodUsed) {
      super(cachingMethodUsed);
   }

   /**
    * @param cell
    * @return
    * @throws statistics.StatisticException
    */
   @Override
   public final double compute4(SmallCell cell) throws StatisticException {
      AbstractCachingModel cachingMethod = getCachingMethod();
      if (!(cachingMethod instanceof AbstractPricingModel)) {
         return -1.0;
      }
      AbstractPricingModel policy = (AbstractPricingModel) cachingMethod;

      try {
         if (policy instanceof IGainRplc) {
            return ((PricedBuffer) cell.getBuffer(cachingMethod)).getPrice4Rplc();
         } else {
            return cell.cachePrice(policy);
         }
      } catch (Throwable ex) {
         throw new StatisticException(ex);
      }

   }

   @Override
   public String title() {
      return "Price(" + getCachingMethod() + ")";
   }
}
