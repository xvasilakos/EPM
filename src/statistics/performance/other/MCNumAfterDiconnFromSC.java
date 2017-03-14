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
public class MCNumAfterDiconnFromSC extends AbstractPerformanceStat<CachingUser, SmallCell, DocumentRequest> {

    public MCNumAfterDiconnFromSC(AbstractCachingPolicy cachingMethod) {
        super(cachingMethod);
    }

    @Override
    public final double computeGain(CachingUser user, DocumentRequest r) throws StatisticException {
        List<Chunk> consumedChunksFromMCWhenDisconnected = r.getChunksConsumedHistoryFromMCAfterExitingSC(getCachingPolicy());
        return consumedChunksFromMCWhenDisconnected.size();
    }
}
