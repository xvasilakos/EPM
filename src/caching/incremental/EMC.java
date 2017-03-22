package caching.incremental;

import caching.Utils;
import caching.base.AbstractCachingModel;
import caching.base.AbstractEPC;
import caching.base.IEMC;
import exceptions.CriticalFailureException;
import java.util.Collection;
import sim.content.Chunk;
import sim.run.SimulationBaseRunner;
import static sim.space.cell.smallcell.BufferBase.BufferAllocationStatus.Success;
import sim.space.cell.smallcell.SmallCell;
import sim.space.users.CachingUser;
import statistics.handlers.iterative.sc.cmpt6.UnonymousCompute6;

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
public class EMC extends AbstractEPC implements IEMC {

    private static EMC singleton = new EMC();

    /**
     * @return the singleton instance of this class according to its placement
     * in the hierarchy of AbstractMethod class descendants.
     */
    public static AbstractCachingModel instance() {
        return singleton;
    }

    protected EMC() {
    }

    @Override
    public String nickName() {
        return "EMC";
    }

    public int cacheDecision(SimulationBaseRunner sim, CachingUser cu,
            Collection<Chunk> chunks, SmallCell hostSC,
            SmallCell targetSC) throws Throwable {

        int ttlEviction = 0;
        // clear based on TTL first..
        ttlEviction = targetSC.checkEMCTTL4Cached(sim.simTime(), this);

        int totalSizeCached = 0;
        for (Chunk nxtChunk : chunks) {

//never do that: No need to, it is done by methods called by cacheItemAttempt
//            if (targetSC.isCached(this, nxtChunk)) {
//                targetSC.addCacher(cu, this, nxtChunk);
//                continue;
//            }
            double cachePrice = targetSC.cachePrice(this);
            double assessment = assess(cu, nxtChunk, hostSC);

            if (assessment / nxtChunk.sizeInMBs() >= cachePrice) {
//                DebugTool.appendln(
//                        "cache positive" + cachePrice + "\n\t"
//                        + "pre-getUsed()=" + targetSC.getBuffer(singleton).getUsed()
//                        + "\n\t"
//                        + "pre-getPrice()=" + targetSC.getBuffer(singleton).getPrice()
//                );

//                targetSC.cacheItem(cu, model, nxtChunk);
                if (targetSC.cacheItemAttemptPriceUpdate(cu, this, nxtChunk) == Success) {
                    totalSizeCached += nxtChunk.sizeInBytes();
                    targetSC.updtEMCTTL4Cached(nxtChunk, sim.simTime());

                }
            }

        }

//        sim.getStatsHandle().updtSCCmpt6(
//                chunks.size(),//totalSizeCached - ttlEviction,
//                new UnonymousCompute6(
//                        new UnonymousCompute6.WellKnownTitle("[DMD(EMC)]"))
//        );

        return totalSizeCached;
    }

    @Override
    public double assess(CachingUser mu, Chunk chunk, SmallCell sc) throws CriticalFailureException {
        double assessment = Utils.assessEMC(chunk, sc, this);
        return assessment;
    }

}
