package statistics.performance.gains;

import caching.base.AbstractCachingPolicy;
import static java.lang.Double.NaN;
import java.util.ArrayList;
import java.util.List;
import sim.content.request.DocumentRequest;
import sim.content.Chunk;
import sim.space.cell.smallcell.SmallCell;
import sim.space.users.CachingUser;
import sim.space.users.StationaryUser;
import statistics.StatisticException;
import statistics.handlers.AbstractPerformanceStat;

/**
 * @author Xenofon Vasilakos xvas@aueb.gr
 */
public class ConsumedMCPercent extends AbstractPerformanceStat<CachingUser, SmallCell, DocumentRequest> {

    public ConsumedMCPercent(AbstractCachingPolicy cachingMethod) {
        super(cachingMethod);
    }

    @Override
    public final double computeGain(CachingUser user, DocumentRequest r) throws StatisticException {
        if (user instanceof StationaryUser) {
            return NaN;// ignore stationaries
        }

        AbstractCachingPolicy policy = getCachingPolicy();

        List<Chunk> consumedChunksFromSC = r.getChunksCacheHitsHistory(policy);
        List<Chunk> consumedChunksFromBH = r.getChunksConsumedHistoryFromBH(policy);

        List<Chunk> consumedChunksFromMC = new ArrayList();

        consumedChunksFromMC.addAll(r.getChunksConsumedHistoryFromMCAfterExitingSC(policy));
        consumedChunksFromMC.addAll(r.getChunksConsumedHistoryFromMCBeforeEnteringSC(policy));
        consumedChunksFromMC.addAll(r.getChunksConsumedHistoryFromMCwSCConn(policy));

        double sum = r.getChunksConsumedOverall(policy).size();
//                consumedChunksFromSC.size()
//                + consumedChunksFromBH.size()
//                + consumedChunksFromMC.size();

        double all = r.referredContentDocument().chunks().size();

        if (all != sum) {
            throw new RuntimeException(
                    "sum: " + sum
                    + " but #chunks: " + all
            );
        }

        return consumedChunksFromMC.size() / all;
    }

    @Override
    public String title() {
        return "%ConsmMC" + "-" + getCachingPolicy().nickName();
    }

    @Override
    public String title(String str) {
        return "%ConsmMC" + "_" + str + "_" + getCachingPolicy().nickName();
    }

}
