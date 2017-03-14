package caching.incremental;

import caching.base.AbstractOracle;
import caching.interfaces.incremental.IIncrementalBase;
import sim.content.Chunk;
import static sim.space.cell.smallcell.BufferBase.BufferAllocationStatus.ItemAlreadyCachedByOtherMUs;
import static sim.space.cell.smallcell.BufferBase.BufferAllocationStatus.Success;
import sim.space.cell.smallcell.SmallCell;
import sim.space.users.CachingUser;

/**
 * Used for optimal caching, i.e. it tries to cache the items in the cell where
 * the mobile will handoff next. Essentially, it is a naive method, thus it uses
 * the type01 naive implementation of caching decisions.
 *
 *
 * @author Xenofon Vasilakos xvas@aueb.gr
 */
public final class Oracle extends AbstractOracle implements IIncrementalBase {

    private static final Oracle singleton = new Oracle();

    public static Oracle instance() {
        return singleton;
    }

    private Oracle() {
    }

    @Override
    public String nickName() {
        return getClass().getSimpleName();
    }

    public int cacheDecision(CachingUser mu, SmallCell targetSC, Chunk... chunksRequested) {

        int totalSizeCached = 0;
        for (Chunk nxtItem : chunksRequested) {
            switch (targetSC.cacheItemAttempt(mu, this, nxtItem)) {
                case Success:
                    totalSizeCached += nxtItem.sizeInBytes();
                    break;
                case ItemAlreadyCachedByOtherMUs:

                    break;
            }
        }
        return totalSizeCached;
    }

    public long cacheDecision(CachingUser mu, SmallCell targetSC, Chunk c) {
        switch (targetSC.cacheItemAttempt(mu, this, c)) {
            case Success:
                return c.sizeInBytes();
//            case ItemAlreadyCachedByOtherMUs:
//                return -c.sizeInBytes();
            default:
                return 0;
        }
    }

}
