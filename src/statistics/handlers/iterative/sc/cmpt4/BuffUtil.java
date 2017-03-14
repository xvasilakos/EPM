package statistics.handlers.iterative.sc.cmpt4;

import caching.base.AbstractCachingPolicy;
import caching.base.AbstractPricing;
import caching.interfaces.rplc.IGainRplc;
import sim.space.cell.smallcell.PricedBuffer;
import sim.space.cell.smallcell.SmallCell;
import statistics.StatisticException;

/**
 *
 * @author Xenofon Vasilakos xvas@aueb.gr
 */
public class BuffUtil extends ComputeAllPoliciesImpl {

    public BuffUtil(AbstractCachingPolicy cachingMethodUsed) {
        super(cachingMethodUsed);
    }

    @Override
    public double compute4(SmallCell cell) throws StatisticException {

        AbstractCachingPolicy cachingPolicy = getCachingMethod();

        if (cachingPolicy instanceof IGainRplc) {
            if (cachingPolicy instanceof AbstractPricing) {
                AbstractPricing pricePolicy = (AbstractPricing) cachingPolicy;
                PricedBuffer buffer = cell.getBuffer(pricePolicy);
                try {
                    return buffer.utilization4Rplc((IGainRplc) cachingPolicy);
                } catch (Throwable ex) {
                    throw new StatisticException(ex);
                }
            } else {//e.g. case pf EPCPopFull without pricing
                return cell.buffUtilization(getCachingMethod());
            }
        } else {
            return cell.buffUtilization(getCachingMethod());
        }

    }

    @Override
    public String title() {
        return "BF%(" + getCachingMethod() + ")";
    }
}
