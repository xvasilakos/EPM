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
public class HITPercent extends AbstractPerformanceStat<CachingUser, SmallCell, DocumentRequest> {

    public HITPercent(AbstractCachingPolicy cachingMethod) {
        super(cachingMethod);
    }

    @Override
    public final double computeGain(CachingUser user, DocumentRequest r) throws StatisticException {

        List<Chunk> consumedChunksHitsHistory = r.getChunksCacheHitsHistory(getCachingPolicy());

        double cacheHit = consumedChunksHitsHistory.size();

        double allChunks = r.referredContentDocument().chunks().size();
        if (allChunks == 0) {
            throw new StatisticException(
                    "#Chunks: " + r.referredContentDocument().chunks().size()
            );
        }

        return cacheHit / allChunks;
    }

    @Override
    public String title() {
        return "%Hit" + "-" + getCachingPolicy().nickName();
    }

    @Override
    public String title(String str) {
        return "%Hit" + "_" + str + "_" + getCachingPolicy().nickName();
    }

}
