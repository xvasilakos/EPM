package statistics.performance.gains;

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
public class HitsNum extends AbstractPerformanceStat<CachingUser, SmallCell, DocumentRequest> {

    public HitsNum(AbstractCachingPolicy cachingMethod) {
        super(cachingMethod);
    }

    @Override
    public final double computeGain(CachingUser user, DocumentRequest r) throws StatisticException {
        List<Chunk> consumedChunksHitsHistory = r.getChunksCacheHitsHistory(getCachingPolicy());
        return consumedChunksHitsHistory.size();
    }
}
