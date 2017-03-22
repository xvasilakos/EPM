package caching.incremental;

import caching.base.AbstractNaive;
import caching.interfaces.incremental.IIncrementalBase;
import caching.interfaces.rplc.IGainNoRplc;
import java.util.Collection;
import sim.run.SimulationBaseRunner;
import sim.content.Chunk;
import static sim.space.cell.smallcell.BufferBase.BufferAllocationStatus.Success;
import sim.space.cell.smallcell.SmallCell;
import sim.space.users.CachingUser;
import statistics.handlers.iterative.sc.cmpt6.UnonymousCompute6;

/**
 * @author xvas
 */
public class Naive extends AbstractNaive implements IGainNoRplc, IIncrementalBase {

    private static final Naive SINGLETON = new Naive();

    public static Naive instance() {
        return SINGLETON;
    }

    protected Naive() {
    }

    @Override
    public String nickName() {
        return getClass().getSimpleName();
    }

    @Override
    public int cacheDecision(
            SimulationBaseRunner sim, CachingUser cu, Collection<Chunk> chunks,
            SmallCell hostSC, SmallCell targetSC) throws Throwable {

        int ttlEviction = 0;
        // clear based on TTL first..
        ttlEviction = targetSC.checkNAIVETTL4Cached(sim.simTime(), this);

        int totalSizeCached = 0;
        for (Chunk nxtChunk : chunks) {

//never do that: No need to, it is done by methods called by cacheItemAttempt
//            if (targetSC.isCached(this, nxtChunk)) {
//                targetSC.addCacher(cu, this, nxtChunk);
//                continue;
//            }
            if (targetSC.cacheItemAttempt(cu, this, nxtChunk) == Success) {
                totalSizeCached += nxtChunk.sizeInBytes();
                targetSC.updtNAIVETTL4Cached(nxtChunk, sim.simTime());
            }
        }

//        sim.getStatsHandle().updtSCCmpt6(chunks.size(),//totalSizeCached - ttlEviction,
//                new UnonymousCompute6(
//                        new UnonymousCompute6.WellKnownTitle("[DMD(Naive)]"))
//        );
//        
//        //hack
//        sim.getStatsHandle().updtSCCmpt6(chunks.size(),
//                new UnonymousCompute6(
//                        new UnonymousCompute6.WellKnownTitle("[DMD(MaxPop)]"))
//        );

        return totalSizeCached;
    }

}
