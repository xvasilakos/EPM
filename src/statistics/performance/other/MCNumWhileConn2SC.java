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
 * Chunks consumed from macro cell when connected to a small cell as well
 *
 * @author Xenofon Vasilakos xvas@aueb.gr
 */
public class MCNumWhileConn2SC extends AbstractPerformanceStat<CachingUser, SmallCell, DocumentRequest> {

    public MCNumWhileConn2SC(AbstractCachingPolicy cachingMethod) {
        super(cachingMethod);
    }

    @Override
    public final double computeGain(CachingUser user, DocumentRequest r) throws StatisticException {
        List<Chunk> consumedChunksFromMC = r.getChunksConsumedHistoryFromMCwSCConn(getCachingPolicy());
        return consumedChunksFromMC.size();
    }
}
