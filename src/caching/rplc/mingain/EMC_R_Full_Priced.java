package caching.rplc.mingain;

import caching.Utils;
import caching.base.AbstractCachingPolicy;
import caching.base.AbstractEPC;
import caching.base.IEMC;
import caching.interfaces.rplc.IGainRplc;
import exceptions.CriticalFailureException;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Set;
import sim.run.SimulationBaseRunner;
import sim.content.Chunk;
import sim.space.cell.demand_registry.PCDemand;
import sim.space.cell.smallcell.SmallCell;
import sim.space.users.CachingUser;

/**
 * Efficient proactive caching with legacy popularity, using a least gain cache
 * replacement policy defined in class AbstractGainRplc. Unlike MinGainAvgProb,
 * this class implementation uses the sum of transition probabilities for
 * assessing the weighted delay gain of a cache decision.
 *
 * @author Xenofon Vasilakos xvas@aueb.gr
 */
public class EMC_R_Full_Priced extends AbstractEPC implements IGainRplc, IEMC {

    private static final AbstractCachingPolicy singelton = new EMC_R_Full_Priced();

    public static AbstractCachingPolicy instance() {
        return singelton;
    }

    public EMC_R_Full_Priced() {
    }

    @Override
    public String nickName(){
        return "EMC-R-priced";
    }
    
    @Override
    public double assess(Chunk item, SmallCell sc) throws Throwable {
        return Utils.assessEMC(item, sc, this);
    }

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
                    return (int) (10000 * assessDiff(t1, t2, sc));
                } catch (Throwable ex) {
                    throw new CriticalFailureException(ex);
                }
            }
        };
    }

    @Override
    public int cacheDecision(SimulationBaseRunner sim, CachingUser mu,
            Collection<Chunk> requestChunks, SmallCell hostSC,
            SmallCell targetSC, Set<Chunk> chunksRplcd,
            PriorityQueue<Chunk> cachedOrderByGain) throws Throwable {

        int totalSizeCached = 0;
        for (Chunk nxtItem : requestChunks) {

            double assessment = assess(nxtItem, targetSC) / nxtItem.sizeInMBs();// due to polymorphism, assess may be different in sub classes
            if (assessment == 0) {
                continue;
            }

            if (targetSC.isCached(this, nxtItem)) {
                targetSC.addCacher(mu, this, nxtItem);
                continue;
            }// otherwise, it may need to evict:

            double cachePrice = targetSC.cachePrice4Rplc(this);

            if (assessment >= cachePrice) {
                if (!Utils.isSpaceAvail(this, targetSC, nxtItem.sizeInBytes())) {
                    //<editor-fold defaultstate="collapsed" desc="if not available space, evict!">
                    /*
                 * Try evicting items
                     */
                    Set<Chunk> opt4Eviction = optForEviction(targetSC, nxtItem,
                            cachedOrderByGain);

                    if (opt4Eviction.isEmpty()) {
                        continue;//cannot add this item
                    }

                    for (Chunk items2evict : opt4Eviction) {
                        targetSC.bufferForceEvict(this, items2evict);
                        chunksRplcd.add(items2evict);
                        cachedOrderByGain.remove(items2evict);
                    }
                    //</editor-fold>
                }// if not available space

                totalSizeCached += nxtItem.sizeInBytes();

                targetSC.cacheItem(mu, this, nxtItem);
                cachedOrderByGain.add(nxtItem);
                targetSC.cachePriceUpdt4Rplc(this);
            }
        }
        return totalSizeCached;
    }

    @Override
    public Set<Chunk> optForEviction(SmallCell sc, Chunk chunk, PriorityQueue<Chunk> orderedCached) throws Throwable {
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
                if (!dmdRegInfo.cachingUsers().isEmpty()) {// dont check if legacy cached    
                    aggrEvictSize += minItem.sizeInMBs();
                    aggrEvictGain += assess(minItem, sc);
                    if (aggrEvictGain / aggrEvictSize > maxThshld) {
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

}
