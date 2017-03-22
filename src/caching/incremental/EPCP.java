package caching.incremental;

import caching.base.AbstractEMPC;
import caching.base.AbstractCachingModel;
import caching.interfaces.incremental.IIncremental;
import caching.Utils;
import caching.base.AbstractPricingModel;
import java.util.Collection;
import sim.run.SimulationBaseRunner;
import sim.content.Chunk;
import static sim.space.cell.smallcell.BufferBase.BufferAllocationStatus.Success;
import sim.space.cell.smallcell.SmallCell;
import sim.space.users.CachingUser;

/**
 * EPC with content popularity extentions.
 *
 * @author xvas
 */
public final class EPCP extends AbstractEMPC implements IIncremental {

    private static final EPCP singleton = new EPCP();

    /**
     * @return the singleton instance of this class according to its placement
     * in the hierarchy of AbstractMethod class descendants.
     */
    public static AbstractCachingModel instance() {
        return singleton;
    }

    EPCP() {
    }

    @Override
    public String nickName() {
        return "EPC+Pop";
    }

    @Override
    public int cacheDecision(
            SimulationBaseRunner sim, CachingUser cu, Collection<Chunk> requestChunks, SmallCell hostSC, SmallCell targetSC) throws Throwable {
//        return EPC.cacheDecision(this, sim, cu, requestChunks, hostSC, hostSC);

        int totalSizeCached = 0;
        for (Chunk nxtChunk : requestChunks) {

            double cachePrice = targetSC.cachePrice(this);
            double assessment = -1.0;

            assessment = assess(cu, nxtChunk, targetSC);

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

    @Override
    public double assess(CachingUser mu, Chunk item, SmallCell sc) throws Throwable {
        return Utils.assessEPCWithPop(mu, item, sc, this);
    }

}
