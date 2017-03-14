package statistics.performance.gains;

import caching.base.AbstractCachingModel;
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

    public GAIN_Hit(AbstractCachingModel cachingMethod) {
        super(cachingMethod);
    }

    @Override
    public final double computeGain(CachingUser user, DocumentRequest r) throws StatisticException {

        List<Chunk> consumedChunksFromSC = r.getChunksCacheHitsHistory(getCachingModel());
        double gainSum = consumedChunksFromSC.size() * r.gainOfTransferSCCacheHit();

        return gainSum;
    }

    @Override
    public String title() {
        return "G_Hit" + "-" + getCachingModel().nickName();
    }

    @Override
    public String title(String str) {
        return "G_Hit" + "_" + str + "_" + getCachingModel().nickName();
    }

    
}
