package caching.incremental;

import caching.Utils;
import caching.base.AbstractPop;
import caching.interfaces.incremental.IIncrementalAggregate;
import caching.interfaces.rplc.IGainNoRplc;
import java.util.Collection;
import sim.run.SimulationBaseRunner;
import sim.content.Chunk;
import static sim.space.cell.smallcell.BufferBase.BufferAllocationStatus.Success;
import sim.space.cell.smallcell.SmallCell;
import sim.space.users.CachingUser;

/**
 * Proactive Caching using POPularity with Cache decisions cancelation (CNC)
 * supported and no support for any cache replacement policies.
 *
 *
 * @author xvas
 */
public final class PopOnly extends AbstractPop implements IIncrementalAggregate, IGainNoRplc {

    private static final PopOnly singleton = new PopOnly();

    public static PopOnly instance() {
        return singleton;
    }

    PopOnly() {
    }

    @Override
    public String nickName() {
        return getClass().getName();
    }

    @Override
    public double assess(Chunk item, SmallCell sc) throws Throwable {
        return Utils.assessOnlyPop(this, item, sc);
    }

    @Override
    public int cacheDecision(
            SimulationBaseRunner sim, CachingUser cu, Collection<Chunk> requestChunks, SmallCell hostSC, SmallCell targetSC) throws Throwable {

        int totalSizeCached = 0;
        for (Chunk nxtChunk : requestChunks) {

            double cachePrice = targetSC.cachePrice(this);
            double assessment = -1.0;

            assessment = assess(nxtChunk, targetSC);

//never do that:
//            if (targetSC.isCached(model, nxtChunk)) {
//                targetSC.addCacher(cu, model, nxtChunk);
//                continue;
//            }
            if (assessment / nxtChunk.sizeInMBs() >= cachePrice) {
//                targetSC.cacheItem(cu, model, nxtChunk);
                if (targetSC.cacheItemAttemptPriceUpdate(cu, this, nxtChunk) == Success) {
                    totalSizeCached += nxtChunk.sizeInBytes();
                }
            }
//            else {
//                throw new RuntimeException(
//                        "policy_ instanceof caching.incremental.EMC" + (policy_ instanceof caching.incremental.EMC) + "\n"
//                        + "policy_ " + (policy_.getClass().getCanonicalName()) + "\n"
//                        + "assessment=" + assessment + "\n"
//                        + "nxtChunk.sizeInMBs()=" + nxtChunk.sizeInMBs() + "\n"
//                        + "assessment / nxtChunk.sizeInMBs()=" + (assessment / nxtChunk.sizeInMBs()) + "\n"
//                        + "cachePrice=" + cachePrice + "\n"
//                );
//            }

        }
        return totalSizeCached;
    }

}
