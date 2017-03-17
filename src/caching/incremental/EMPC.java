package caching.incremental;

import caching.Utils;
import caching.base.AbstractCachingModel;
import caching.base.AbstractEMPC;
import caching.base.IEMPC;
import caching.interfaces.incremental.IIncrementalAggregate;
import java.util.Collection;
import sim.run.SimulationBaseRunner;
import sim.content.Chunk;
import static sim.space.cell.smallcell.BufferBase.BufferAllocationStatus.Success;
import sim.space.cell.smallcell.SmallCell;
import sim.space.users.CachingUser;

/**
 * 
 * @author xvas
 */
public final class EMPC extends AbstractEMPC implements IIncrementalAggregate, IEMPC {

    private static EMPC singleton = new EMPC();

    /**
     * @return the singleton instance of this class according to its placement
     * in the hierarchy of AbstractMethod class descendants.
     */
    public static AbstractCachingModel instance() {
        return singleton;
    }

    EMPC() {
    }

    @Override
    public String nickName() {
        return "EMPC";
    }

    @Override
    public int cacheDecision(
            SimulationBaseRunner sim, CachingUser cu, Collection<Chunk> requestChunks, SmallCell hostSC, SmallCell targetSC) throws Throwable {

        int totalSizeCached = 0;

        for (Chunk nxtChunk : requestChunks) {

            if (targetSC.isCached(this, nxtChunk)) {
//never do that neighbSC.addCacher(mu, this, nxtItem);
                continue;
            }

            if (!Utils.isSpaceAvail(this, targetSC, nxtChunk.sizeInBytes())) { //since no replacement policy supported
                continue;//cannot add this item
            }

            double cachePrice = targetSC.cachePrice(this);
            if (assess(nxtChunk, targetSC) / nxtChunk.sizeInMBs() >= cachePrice) {
                
                if (targetSC.cacheItemAttemptPriceUpdate(cu, this, nxtChunk) == Success) {
                    totalSizeCached += nxtChunk.sizeInBytes();
                }
                
            }
        }
        return totalSizeCached;
    }

    @Override
    public double assess(Chunk item, SmallCell sc) throws Throwable {
        return Utils.assessEMPC(item, sc, this);
    }

}
