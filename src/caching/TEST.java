/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package caching;

import caching.base.AbstractCachingPolicy;
import caching.base.IEMC;
import caching.incremental.EMC;
import java.util.Collection;
import java.util.PriorityQueue;
import java.util.Set;
import sim.content.Chunk;
import sim.run.SimulationBaseRunner;
import sim.space.cell.smallcell.SmallCell;
import sim.space.users.CachingUser;
import utils.DebugTool;

/**
 *
 * @author xvas
 */
public class TEST extends  caching.base.no_price.AbstractGainRplc implements IEMC{
 
      private static TEST singleton = new TEST();

     
    /**
     * @return the singleton instance of this class according to its placement
     * in the hierarchy of AbstractMethod class descendants.
     */
    public static AbstractCachingPolicy instance() {
        return singleton;
    }
    
    
    @Override
    public String nickName() {
        return "TEST-R";
    }

    @Override
    public double assess(Chunk theChunk, SmallCell sc) throws Throwable {
        return Utils.assessEMC(theChunk, sc, this);
    }
    
    @Override
    public int cacheDecision(SimulationBaseRunner sim, CachingUser cu,
            Collection<Chunk> requestChunks, SmallCell hostSC,
            SmallCell targetSC, Set<Chunk> chunksRplcd,
            PriorityQueue<Chunk> cachedOrderByGain) throws Throwable {
//        return EPC.cacheDecision(this, sim, mu, requestChunks, hostSC, targetSC);

        int totalSizeCached = 0;
        for (Chunk nxtChunk : requestChunks) {

            double cachePrice = targetSC.cachePrice((EMC) EMC.instance());
            double assessmentEMC = Utils.assessEMC(nxtChunk, hostSC, (EMC) EMC.instance());

            boolean emccached = false;

            if (assessmentEMC / nxtChunk.sizeInMBs() >= cachePrice) {
//                if (targetSC.cacheItemAttempt(cu, (EMC) EMC.instance(), nxtChunk) != Success) {
                if (targetSC.isCached((EMC) EMC.instance(), nxtChunk)) {
                    emccached = true;
                }

                //else test wtf
                // targetSC.cachePriceUpdt((EMC) EMC.instance());
                //////////////////
                if (targetSC.isCached(this, nxtChunk)) {
                    targetSC.addCacher(cu, this, nxtChunk);
//                    if (!emccached) {
//                        DebugTool.appendLn("EMC not cached but  is legacy in EMC-R");
//                    }

                    continue;
                }// otherwise, it may need to evict:

                if (!Utils.isSpaceAvail(this, targetSC, nxtChunk.sizeInBytes())) {

                    //<editor-fold defaultstate="collapsed" desc="if not available space, evict!">
                    /*
                     * Try evicting items
                     */
                    Set<Chunk> opt4Eviction = optForEviction(
                            targetSC, nxtChunk, cachedOrderByGain);

                    if (opt4Eviction.isEmpty()) {
                        DebugTool.appendLn("Fails replacement. emccached=" + emccached);
                        continue;//cannot add this item
                    }

                    for (Chunk itm2evict : opt4Eviction) {
                        targetSC.bufferForceEvict(this, itm2evict);

                        if (targetSC.isCached((EMC) EMC.instance(), nxtChunk)) {
                            DebugTool.appendLn("replacement of" + itm2evict.getID() + " - cached by emc?=" + targetSC.isCached((EMC) EMC.instance(), nxtChunk));
                        }

                    }
                    chunksRplcd.addAll(opt4Eviction);

                    cachedOrderByGain.removeAll(opt4Eviction);
                    //</editor-fold>

                    DebugTool.appendLn("Caches after replacement. emccached=" + emccached);

                }// if not available space

                targetSC.cacheItem(cu, this, nxtChunk);
                cachedOrderByGain.add(nxtChunk);
                totalSizeCached += nxtChunk.sizeInBytes();

            }

        }
        return totalSizeCached;
    }
}
