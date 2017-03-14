package statistics.performance.gains;

import caching.base.AbstractCachingPolicy;
import sim.content.request.DocumentRequest;
import sim.space.cell.smallcell.SmallCell;
import sim.space.users.CachingUser;
import statistics.StatisticException;
import statistics.handlers.AbstractPerformanceStat;

/**
 *
 * @author Xenofon Vasilakos xvas@aueb.gr
 */
public class GAIN extends AbstractPerformanceStat<CachingUser, SmallCell, DocumentRequest> {

    private GAIN_Hit ghit;
    private GAIN_BH gbh;

    public GAIN(AbstractCachingPolicy cachingMethod) {
        super(cachingMethod);
        ghit = new GAIN_Hit(cachingMethod);
        gbh = new GAIN_BH(cachingMethod);
    }

    @Override
    public final double computeGain(CachingUser user, DocumentRequest r) throws StatisticException {
        return ghit.computeGain(user, r) + gbh.computeGain(user, r);
    }

    @Override
    public String title() {
        return "G" + "-" + getCachingPolicy().nickName();
    }

    @Override
    public String title(String str) {
        return "G" + "_" + str + "_" + getCachingPolicy().nickName();
    }

}
