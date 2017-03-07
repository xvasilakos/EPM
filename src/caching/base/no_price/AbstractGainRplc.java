package caching.base.no_price;

import caching.base.*;
import caching.Utils;
import caching.interfaces.rplc.IGainRplc;
import exceptions.CriticalFailureException;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Set;
import sim.run.SimulationBaseRunner;
import sim.content.Chunk;
import sim.space.cell.smallcell.SmallCell;
import sim.space.cell.demand_registry.PCDemand;
import sim.space.users.CachingUser;
import statistics.handlers.iterative.sc.cmpt6.UnonymousCompute6;

/**
 * Same as caching.base.AbstractGainRplc, only without using pricing
 *
 * @author xvas
 */
public abstract class AbstractGainRplc extends AbstractCachingPolicy implements IGainRplc, IPop {

    protected double assessDiff(Chunk a, Chunk b, SmallCell sc) throws Throwable {
        return assess(a, sc) / a.sizeInMBs() - assess(b, sc) / b.sizeInMBs();
    }

    @Override
    public Comparator<Chunk> evictionPriorityComparator(final SmallCell sc) throws CriticalFailureException {
        return new Comparator<Chunk>() {

            @Override
            public int compare(Chunk t1, Chunk t2) {
                try {
                    // t1 - t2  to use it as a min heap
                    return (int) (1000 * assessDiff(t1, t2, sc));
                } catch (Throwable ex) {
                    throw new CriticalFailureException(ex);
                }
            }
        };
    }

    @Override
    public int cacheDecision(SimulationBaseRunner sim, CachingUser cu,
            Collection<Chunk> requestChunks, SmallCell hostSC,
            SmallCell targetSC, Set<Chunk> chunksRplcd,
            PriorityQueue<Chunk> cachedOrderByGain) throws Throwable {

        // helps to skip some checking, e.g. for cosequent chunks of the same content with same assement. 
        // -10 means last one succeeded
        double lastFailedAssessment = -10;

        int totalSizeCached = 0;
        for (Chunk nxtChunk : requestChunks) {
            double assessment = assess(nxtChunk, targetSC) / nxtChunk.sizeInMBs();// note that assess to be defined in sub-classes
            if (assessment == 0
                    && !Utils.isSpaceAvail(this, targetSC, nxtChunk.sizeInBytes())) {
                continue;
            }
            if (assessment == lastFailedAssessment) {
                continue;
            }
            if (targetSC.isCached(this, nxtChunk)) {
                targetSC.addCacher(cu, this, nxtChunk);
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
                    lastFailedAssessment = assessment;
                    continue;//cannot add this item
                }
                lastFailedAssessment = -10;

                for (Chunk items2evict : opt4Eviction) {
                    targetSC.bufferForceEvict(this, items2evict);
                }
                chunksRplcd.addAll(opt4Eviction);
                cachedOrderByGain.removeAll(opt4Eviction);
                //</editor-fold>

            }// if not available space

            targetSC.cacheItem(cu, this, nxtChunk);
            cachedOrderByGain.add(nxtChunk);
            totalSizeCached += nxtChunk.sizeInBytes();

        }
        return totalSizeCached;
    }

    @Override
    public Set<Chunk> optForEviction(SmallCell sc, Chunk chunk,
            PriorityQueue<Chunk> orderedCached) throws Throwable {
//see "kanonas"      
        double aggrEvictGain = 0.0, aggrEvictSize = 0.0;
        double maxThshld = assess(chunk, sc) / chunk.sizeInMBs();

        long minSpaceRequired = chunk.sizeInBytes();

        Set<Chunk> optForEviction = new HashSet<>();//  suggested to evict

        long freeSpace = sc.getCacheAvailable(this);

        while (!orderedCached.isEmpty() && freeSpace < minSpaceRequired) {
            Chunk minItem = orderedCached.poll();

/////kanonas = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = =   
            PCDemand.RegistrationInfo dmdRegInfo // info about the currently 
                    // requested cache space by this SC 
                    = sc.dmdRegInfoPC(minItem, this);
//                                                                             =
            if (dmdRegInfo != null) {
                if (!dmdRegInfo.cachingUsers().isEmpty()) {// don't check if legacy cached; Proceed straightto replacing it.     
                    aggrEvictSize += minItem.sizeInMBs();
                    aggrEvictGain += assess(minItem, sc);
                    if (aggrEvictGain / aggrEvictSize
                            >= /*check equal too, as it could (almost surelly if same-sized chunks) denote 
                            chunks for same content.*/ maxThshld) {// in this case replacement is not beneficial
                        return new HashSet<>();// abort and return an empty set    
                    }
                }
            }
//// = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = =  
            optForEviction.add(minItem);
            freeSpace += minItem.sizeInBytes();
        }

        if (freeSpace < minSpaceRequired) {
            optForEviction.clear();
        }

        return optForEviction;
    }

//    private void yyyStat2(SmallCell sc, Chunk nxtItem, Class classType) throws Throwable {//yyy
//        if (!getClass().equals(classType)
//                && !(sc.getID() == 1
//                || sc.getID() == 12
//                || sc.getID() == 17
//                || sc.getID() == 21)) {
//            return;
//        }
    private void yyy(SmallCell sc, Chunk theChunk, CachingUser mu) throws Exception {
//        double cachePrice = sc.cachePrice4Rplc(this);
//        double cacheUtil = sc.cacheUtilUpdt4Rplc(this);
//        double assessment = assess(nxtItem, sc);// due to polymorphism, assess may be different in sub classes
//        sc.getSim().getStatsHandle().updtSCCmpt6(
//                assessment,
//                new UnonymousCompute6(
//                        UnonymousCompute6.WellKnownTitle.ASSESS_LC.concat(getClass().getName())
//                )
//        );

//        if (!(mu.getID() % 1 != 0 //                || sc.getID() == 1
//                //                || sc.getID() == 13
//                //                || sc.getID() == 20
//                //                || sc.getID() == 25
//                )) {
//            return;
//        }
        PCDemand.RegistrationInfo nfo = sc.dmdRegInfoPC(theChunk, this);
        double sumProb = nfo != null ? nfo.sumTransProbs() : 0.0;

        double Q = sumProb;
        double w = sc.getDmdLclForW().computeAvgW();
        double f = sc.dmdPopularity(theChunk.referredContentDocument(), this);

//        sc.getSim().getStatsHandle().updtSCCmpt6(
//                cachePrice,
//                new UnonymousCompute6(
//                        UnonymousCompute6.WellKnownTitle.PRICE.
//                        concat(
//                                getClass().getSimpleName().concat("-cell_" + sc.getID())
//                        )
//                )
//        );
//        sc.getSim().getStatsHandle().updtSCCmpt6(
//                cacheUtil,
//                new UnonymousCompute6(
//                        new UnonymousCompute6.WellKnownTitle("Util").
//                        concat(
//                                getClass().getSimpleName().concat("-cell_" + sc.getID())
//                        )
//                )
//        );
//        sc.getSim().getStatsHandle().updtSCCmpt6(
//                assessment/ - cachePrice,
//                new UnonymousCompute6(
//                        UnonymousCompute6.WellKnownTitle.P_G_DIFF.concat(getClass().getName())
//                )
//        );
        sc.getSim().getStatsHandle().updtSCCmpt6(
                Q,
                new UnonymousCompute6(
                        UnonymousCompute6.WellKnownTitle.Q.
                        concat(
                                getClass().getSimpleName()//.concat("-cell_" + sc.getID())
                        )
                )
        );

        if (sumProb != 0) {// otherwise it counts the probabilities
            // from objects that are not requested any more by any mobile user.
            sc.getSim().getStatsHandle().updtSCCmpt6(
                    Q,
                    new UnonymousCompute6(
                            UnonymousCompute6.WellKnownTitle.Q_NONZERO//.concat(getClass().getName())
                    )
            );
        }

        sc.getSim().getStatsHandle().updtSCCmpt6(
                f,
                new UnonymousCompute6(
                        UnonymousCompute6.WellKnownTitle.F_POP.
                        concat(
                                getClass().getSimpleName()//.concat("-cell_" + sc.getID())
                        )
                )
        );

        if (w != -1) {
            sc.getSim().getStatsHandle().updtSCCmpt6(
                    w,
                    new UnonymousCompute6(
                            UnonymousCompute6.WellKnownTitle.F_POP.
                            concat(
                                    getClass().getSimpleName()//.concat("-cell_" + sc.getID())
                            )
                    )
            );
        }
//
        sc.getSim().getStatsHandle().updtSCCmpt6(
                sc.getDmdLclForW().getCurrDemandNumForW(),
                new UnonymousCompute6(
                        UnonymousCompute6.WellKnownTitle.LCL_DMD.
                        concat(
                                getClass().getSimpleName()//.concat("-cell_" + sc.getID())
                        )
                )
        );

        sc.getSim().getStatsHandle().updtSCCmpt6(
                sc.getDmdPC(this).getCurrDemandNum(),
                new UnonymousCompute6(
                        UnonymousCompute6.WellKnownTitle.PC_DMD.
                        concat(
                                getClass().getSimpleName()//.concat("-cell_" + sc.getID())
                        )
                )
        );

        sc.getSim().getStatsHandle().updtSCCmpt6(
                theChunk.gainOfTransferSCCacheHit(),
                new UnonymousCompute6(
                        new UnonymousCompute6.WellKnownTitle("G_HIT").
                        concat(
                                getClass().getSimpleName()//.concat("-cell_" + sc.getID())
                        )
                )
        );
        sc.getSim().getStatsHandle().updtSCCmpt6(
                theChunk.gainOfTransferSCThroughBH(),
                new UnonymousCompute6(
                        new UnonymousCompute6.WellKnownTitle("G_BH").
                        concat(
                                getClass().getSimpleName()//.concat("-cell_" + sc.getID())
                        )
                )
        );
        sc.getSim().getStatsHandle().updtSCCmpt6(
                theChunk.costOfTransferMC_BH(),
                new UnonymousCompute6(
                        new UnonymousCompute6.WellKnownTitle("_MC_BH").
                        concat(
                                getClass().getSimpleName()//.concat("-cell_" + sc.getID())
                        )
                )
        );
        sc.getSim().getStatsHandle().updtSCCmpt6(
                theChunk.costOfTransferSCCacheHit(),
                new UnonymousCompute6(
                        new UnonymousCompute6.WellKnownTitle("_SC_HIT").
                        concat(
                                getClass().getSimpleName()//.concat("-cell_" + sc.getID())
                        )
                )
        );
        sc.getSim().getStatsHandle().updtSCCmpt6(
                theChunk.costOfTransferSC_BH(),
                new UnonymousCompute6(
                        new UnonymousCompute6.WellKnownTitle("_SC_BH").
                        concat(
                                getClass().getSimpleName()//.concat("-cell_" + sc.getID())
                        )
                )
        );
    }

}
