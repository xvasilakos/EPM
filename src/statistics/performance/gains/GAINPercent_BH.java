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
 * @author Xenofon Vasilakos xvas@aueb.gr
 */
public class GAINPercent_BH extends AbstractPerformanceStat<CachingUser, SmallCell, DocumentRequest> {

    public GAINPercent_BH(AbstractCachingPolicy cachingMethod) {
        super(cachingMethod);
    }

    @Override
    public final double computeGain(CachingUser user, DocumentRequest r) throws StatisticException {

        List<Chunk> consumedChunksFromBH = r.getChunksConsumedHistoryFromBH(getCachingPolicy());
        double backhaul = r.gainOfTransferSCThroughBH() * consumedChunksFromBH.size();

        double ifAllFromMC = r.costOfTransferMC_BH() * r.referredContentDocument().chunks().size();

        if (ifAllFromMC == 0) {
            throw new StatisticException("zero cost from 100% MC consumption is impossible. "
                    + "#Chunks: " + r.referredContentDocument().chunks().size()
                    + "costOfTransferMC: " + r.costOfTransferMC_BH()
            );
        }

        return backhaul / ifAllFromMC;
    }

    @Override
    public String title() {
        return "%G_BH" + "-" + getCachingPolicy().nickName();
    }

    @Override
    public String title(String str) {
        return "%G_BH" + "_" + str + "_" + getCachingPolicy().nickName();
    }

}
