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
 * Chunks consumed when connected only to the macrocell
 *
 * @author Xenofon Vasilakos xvas@aueb.gr
 */
public class MCNumBeforeConn2SC extends AbstractPerformanceStat<CachingUser, SmallCell, DocumentRequest> {

    public MCNumBeforeConn2SC(AbstractCachingPolicy cachingMethod) {
        super(cachingMethod);
    }

    @Override
    public final double computeGain(CachingUser user, DocumentRequest nxtRequest) throws StatisticException {
        List<Chunk> consumedChunksFromMCWhenDisconnected
                = nxtRequest.getChunksConsumedHistoryFromMCBeforeEnteringSC(getCachingPolicy());
        return consumedChunksFromMCWhenDisconnected.size();
    }
}
