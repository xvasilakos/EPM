package caching.incremental;

import caching.Utils;
import caching.base.AbstractEPC;
import caching.base.AbstractCachingPolicy;
import caching.base.AbstractPricing;
import caching.interfaces.incremental.IIncremental;
import caching.interfaces.rplc.IGainNoRplc;
import java.util.Collection;
import sim.run.SimulationBaseRunner;
import sim.content.Chunk;
import static sim.space.cell.smallcell.BufferBase.BufferAllocationStatus.Success;
import sim.space.cell.smallcell.SmallCell;
import sim.space.users.CachingUser;

/**
 * Efficient Proactive Caching implementation with Cache decisions cancellation
 * supported and no support for any cache replacement policies. Note that if any
 * of the mobile requesters tries to cancel a request for an item, then the item
 * is evicted, which differentiates this implementation from Incremental2.
 *
 * @author xvas
 */
public class EPC extends AbstractEPC implements IIncremental, IGainNoRplc {

    private static EPC singleton = new EPC();

    /**
     * @return the singleton instance of this class according to its placement
     * in the hierarchy of AbstractMethod class descendants.
     */
    public static AbstractCachingPolicy instance() {
        return singleton;
    }

    protected EPC() {
    }

    @Override
    public String nickName() {
        return "EPC";
    }

    @Override
    public int cacheDecision(SimulationBaseRunner sim, CachingUser mu, Collection<Chunk> requestChunks, SmallCell hostSC, SmallCell targetSC) throws Throwable {
//        int totalSizeCached = 0;
//        for (Chunk nxtChunk : requestChunks) {
//
//            double cachePrice = targetSC.cachePrice(this);
//            double assessment = assess(mu, nxtChunk, targetSC);
//
////never do that:
////            if (targetSC.isCached(this, nxtItem)) {
////                targetSC.addCacher(mu, this, nxtItem);
////                continue;
////            }
//            if (targetSC.isCached(this, nxtChunk) || !Utils.isSpaceAvail(this, targetSC, nxtChunk.sizeInBytes())) {
//                continue; //since no eviction policy supported
//            }
//
//            if (assessment / nxtChunk.sizeInMBs() >= cachePrice) {
//                totalSizeCached += nxtChunk.sizeInBytes();
//                targetSC.cacheItem(mu, this, nxtChunk);
//                targetSC.cachePriceUpdt(this);
//            }
//
//        }
//        return totalSizeCached;
        return cacheDecision(this, sim, mu, requestChunks, hostSC, targetSC);
    }

    static int cacheDecision(IGainNoRplc policy_, SimulationBaseRunner sim,
            CachingUser cu, Collection<Chunk> requestChunks, SmallCell hostSC, SmallCell targetSC) throws Throwable {

        AbstractPricing policy = (AbstractPricing) policy_;

        int totalSizeCached = 0;
        for (Chunk nxtChunk : requestChunks) {

            double cachePrice = targetSC.cachePrice(policy);
            double assessment = -1.0;

            if (policy_ instanceof caching.incremental.EMC) {
                assessment = ((caching.incremental.EMC) policy_).assess(cu, nxtChunk, hostSC);
            }
            if (policy_ instanceof caching.incremental.EPC) {
                assessment = ((caching.incremental.EPC) policy_).assess(cu, nxtChunk, hostSC);
            }
            if (policy_ instanceof EPC_with_Pop) {
                assessment = ((EPC_with_Pop) policy_).assess(cu, nxtChunk, hostSC);
            }
            if (policy_ instanceof PopOnly) {
                assessment = ((PopOnly) policy_).assess(nxtChunk, hostSC);
            }
            if (assessment == -1.0) {
                throw new UnsupportedOperationException("Not supported for: " + policy_.getClass().getCanonicalName());
            }

//never do that:
//            if (targetSC.isCached(policy, nxtChunk)) {
//                targetSC.addCacher(cu, policy, nxtChunk);
//                continue;
//            }

            if (assessment / nxtChunk.sizeInMBs() >= cachePrice) {
//                targetSC.cacheItem(cu, policy, nxtChunk);
                if (targetSC.cacheItemAttempt(cu, policy, nxtChunk) == Success) {
                    totalSizeCached += nxtChunk.sizeInBytes();
                    targetSC.cachePriceUpdt(policy);
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
