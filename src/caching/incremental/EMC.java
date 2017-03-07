package caching.incremental;

import caching.Utils;
import caching.base.AbstractCachingPolicy;
import caching.base.IEMC;
import exceptions.CriticalFailureException;
import java.util.Collection;
import sim.content.Chunk;
import sim.run.SimulationBaseRunner;
import static sim.space.cell.smallcell.BufferBase.BufferAllocationStatus.Success;
import sim.space.cell.smallcell.SmallCell;
import sim.space.users.CachingUser;

/**
 * Efficient Proactive Caching implementation with Cache decisions cancelation
 * supported and no support for any cache replacement policies. Unlike the
 * Incremental1 implementation of the same package, this implementation uses the
 * sum of probabilities to asses the gain of a cache decision. And like
 * Incremental1,if any of the mobile requestors tries to cancel a request for an
 * item, then the item is evicted, which differentiates this implementation from
 * IncrementalSumProb2.
 *
 * @author xvas
 */
public class EMC extends caching.incremental.EPC implements IEMC {

    private static EMC singleton = new EMC();

     
    /**
     * @return the singleton instance of this class according to its placement
     * in the hierarchy of AbstractMethod class descendants.
     */
    public static AbstractCachingPolicy instance() {
        return singleton;
    }

    protected EMC() {
    }

    @Override
    public String nickName() {
        return "EMC";
    }

    
     
    
    @Override
    public int cacheDecision(SimulationBaseRunner sim, CachingUser cu, 
            Collection<Chunk> requestChunks, SmallCell hostSC, 
            SmallCell targetSC) throws Throwable {
//        return EPC.cacheDecision(this, sim, mu, requestChunks, hostSC, targetSC);

        int totalSizeCached = 0;
        for (Chunk nxtChunk : requestChunks) {

//never do that: No need to, it is done by methods called by cacheItemAttempt
//            if (targetSC.isCached(this, nxtChunk)) {
//                targetSC.addCacher(cu, this, nxtChunk);
//                continue;
//            }

            double cachePrice = targetSC.cachePrice(this);
            double assessment = assess(cu, nxtChunk, hostSC);

            if (assessment / nxtChunk.sizeInMBs() >= cachePrice) {
//                targetSC.cacheItem(cu, policy, nxtChunk);
                if (targetSC.cacheItemAttempt(cu, this, nxtChunk) == Success) {
                    totalSizeCached += nxtChunk.sizeInBytes();
                    targetSC.cachePriceUpdt(this);
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
    public double assess(CachingUser mu, Chunk chunk, SmallCell sc) throws CriticalFailureException {
        double assessment = Utils.assessEMC(chunk, sc, this);
        return assessment;
    }

}
