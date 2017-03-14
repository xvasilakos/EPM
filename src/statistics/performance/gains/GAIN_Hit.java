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
public class GAIN_Hit extends AbstractPerformanceStat<CachingUser, SmallCell, DocumentRequest>  {

    public GAIN_Hit(AbstractCachingPolicy cachingMethod) {
        super(cachingMethod);
    }

    @Override
    public final double computeGain(CachingUser user, DocumentRequest r) throws StatisticException {

        List<Chunk> consumedChunksFromSC = r.getChunksCacheHitsHistory(getCachingPolicy());
        double gainSum = consumedChunksFromSC.size() * r.gainOfTransferSCCacheHit();

        return gainSum;
    }

    @Override
    public String title() {
        return "G_Hit" + "-" + getCachingPolicy().nickName();
    }

    @Override
    public String title(String str) {
        return "G_Hit" + "_" + str + "_" + getCachingPolicy().nickName();
    }

    
}
