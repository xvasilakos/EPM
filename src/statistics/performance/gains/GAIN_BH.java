package statistics.performance.gains;

import caching.base.AbstractCachingPolicy;
import java.util.Iterator;
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
public class GAIN_BH extends AbstractPerformanceStat<CachingUser, SmallCell, DocumentRequest> {

    public GAIN_BH(AbstractCachingPolicy cachingMethod) {
        super(cachingMethod);
    }

    @Override
    public final double computeGain(CachingUser user, DocumentRequest r) throws StatisticException {

        List<Chunk> consumedChunksFromBH = r.getChunksConsumedHistoryFromBH(getCachingPolicy());
        double gainSum = consumedChunksFromBH.size() * r.gainOfTransferSCThroughBH();

        return gainSum;
    }

    @Override
    public String title() {
        return "G_BH" + "-" + getCachingPolicy().nickName();
    }

    @Override
    public String title(String str) {
        return "G_BH" + "_" + str + "_" + getCachingPolicy().nickName();
    }
}
