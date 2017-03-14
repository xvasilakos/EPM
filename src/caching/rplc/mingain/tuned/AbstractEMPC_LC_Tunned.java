package caching.rplc.mingain.tuned;

import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Set;
import sim.content.Chunk;
import sim.space.cell.demand_registry.PCDemand;
import sim.space.cell.smallcell.SmallCell;

/**
 * Same as EPCPop, only items do not get replaced when at least one of the
 * requesting mobiles gets close enough to the small cell.
 *
 * @author Xenofon Vasilakos xvas@aueb.gr
 */
public abstract class AbstractEMPC_LC_Tunned extends caching.rplc.mingain.EMPC_R_Full_Priced {

    AbstractEMPC_LC_Tunned() {
    }

    /**
     * Does not replace items requested by at least one mobile that is close to
     * the center of the small cell (based on the method checkRplcAbort()).
     *
     * @param sc
     * @param item
     * @param cachedOrderByGain
     * @return
     * @throws Throwable
     */
    @Override
    public Set<Chunk> optForEviction(SmallCell sc, Chunk item, 
            PriorityQueue<Chunk> cachedOrderByGain) throws Throwable {

//        //appendLog("Selecting items to evict.. ", sc, this);
        //see "kanonas"       
        double aggrEvictGain = 0.0, aggrEvictSize = 0.0;
        double maxThshld = assess(item, sc) / item.sizeInMBs();

        long minSpaceRequired = item.sizeInBytes();

        Set<Chunk> optForEviction = new HashSet<>();//  suggested to evict

        long freeSpace = sc.getCacheAvailable(this);

        NEXT_CACHED_ITEM:
        while (!cachedOrderByGain.isEmpty() && freeSpace < minSpaceRequired) {
            Chunk minItem = cachedOrderByGain.poll();

            if (assess(minItem, sc) == 0) {
                optForEviction.add(minItem);
                freeSpace += minItem.sizeInBytes();
//                print21("item will be evicted: " + minItem.toString(), sc);
                continue;
            }

            PCDemand.RegistrationInfo dmdRegInfo // info about the currently requested cache space by this SC 
                    = sc.dmdRegInfoPC(minItem, this);

            if (dmdRegInfo != null && checkRplcAbort(dmdRegInfo, sc)) {

                /*
                * Otherwise if null, then no mobile is currently
                * requesting for minItem, i.e. it has remained in the cache
                * by prior requests which are not active any more.
                *
                * Therefore, for the else case, let it be tested for being removed..
                 */
                continue NEXT_CACHED_ITEM;
            }

/////kanonas = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = =   
//            PCDemand.RegistrationInfo dmdRegInfo // info about the currently 
//                    // requested cache space by this SC 
//                    = sc.dmdRegInfoPC(minItem);
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

    /**
     * Uses no replacement interval "??" which is defined by sub classes
     *
     * @param dmdRegInfo
     * @param sc
     * @return
     */
    protected abstract boolean checkRplcAbort(PCDemand.RegistrationInfo dmdRegInfo, SmallCell sc);

}
