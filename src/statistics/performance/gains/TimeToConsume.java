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
public class TimeToConsume extends AbstractPerformanceStat<CachingUser, SmallCell, DocumentRequest> {

    public TimeToConsume(AbstractCachingPolicy cachingMethod) {
        super(cachingMethod);
    }

    @Override
    public final double computeGain(CachingUser user, DocumentRequest r) throws StatisticException {

        int issuedAtSimTime = r.getIssuedAtSimTime();
        int competionSimTime = r.getCompetionSimTime(getCachingPolicy());

        return competionSimTime - issuedAtSimTime;
    }

    @Override
    public String title() {
        return "TTC" + "-" + getCachingPolicy().nickName();
    }
    @Override
    public String title(String str) {
        return "TTC"+ "_" + str + "_" + getCachingPolicy().nickName();
    }

}
