package statistics.performance.other;

import caching.base.AbstractCachingPolicy;
import java.util.List;
import sim.content.request.DocumentRequest;
import sim.content.Chunk;
import sim.space.cell.smallcell.SmallCell;
import sim.space.users.CachingUser;
import statistics.StatisticException;
import statistics.handlers.AbstractPerformanceStat;

/**
 *
 * @author Xenofon Vasilakos xvas@aueb.gr
 */
public class BHNum extends AbstractPerformanceStat<CachingUser, SmallCell, DocumentRequest> {

    public BHNum(AbstractCachingPolicy cachingMethod) {
        super(cachingMethod);
    }

    @Override
    public final double computeGain(CachingUser user, DocumentRequest r) throws StatisticException {
        List<Chunk> consumedChunksFromBH = r.getChunksConsumedHistoryFromBH(getCachingPolicy());
        return consumedChunksFromBH.size();
    }

}
