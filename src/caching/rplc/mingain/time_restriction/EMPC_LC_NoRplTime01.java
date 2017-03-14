package caching.rplc.mingain.time_restriction;

import caching.base.AbstractCachingPolicy;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Set;
import sim.content.Chunk;
import sim.space.cell.demand_registry.PCDemand;
import sim.space.cell.smallcell.SmallCell;
import sim.space.users.CachingUser;
import sim.space.users.StationaryUser;
import sim.space.users.mobile.MobileUser;
import sim.space.util.DistanceComparator;

/**
 * Must check functionality after severe changes in code
 *
 * Same as EPCPop, only items do not get replaced when at least one of the
 * requesting mobiles gets close enough to the small cell.
 *
 * @author Xenofon Vasilakos xvas@aueb.gr
 */
@Deprecated
public class EMPC_LC_NoRplTime01 extends caching.rplc.mingain.EMPC_R_Full_Priced {

    private static AbstractCachingPolicy singelton = new EMPC_LC_NoRplTime01();

    public static AbstractCachingPolicy instance() {
        return singelton;
    }

    protected boolean checkAbortRplc(PCDemand.RegistrationInfo dmdRegInfo, 
            SmallCell sc, double minTimeRplcAllowed) {

        if (dmdRegInfo.cachingUsers().isEmpty()) {// redudant but easy to understand
            return false;
        }

        for (CachingUser nxtCU : dmdRegInfo.cachingUsers()) {

            double distance = DistanceComparator.euclidianDistance(
                    nxtCU, sc) - sc.getRadius();
            double velocity = -1;
            if (nxtCU instanceof MobileUser) {
                velocity = ((MobileUser) nxtCU).getVelocity();
            } else if (nxtCU instanceof StationaryUser) {
                velocity = 0;
            } else {
                throw new UnsupportedOperationException();
            }
            double time = distance / velocity; // time to handoff
            if (time < minTimeRplcAllowed) {
                return true; // check the next item
            }
        }
        return false;
    }

    protected boolean checkAbortRplc(PCDemand.RegistrationInfo dmdRegInfo, SmallCell sc) {
        return checkAbortRplc(dmdRegInfo, sc, 1/* minimum time to handoff 
       that replacement is allowed*/);
    }

    EMPC_LC_NoRplTime01() {
    }

    /**
     * Does not replace items requested by at least one mobile that is close to
     * the center of the small cell, as defined by parameter
     * space.mu.group.prohibit_rplc_minDist.
     *
     * @param sc
     * @param item
     * @param orderedCached
     * @return
     * @throws Throwable
     */
    @Override
    public Set<Chunk> optForEviction(SmallCell sc, Chunk item,
            PriorityQueue<Chunk> orderedCached) throws Throwable {
        //see "kanonas"        
        double aggrEvictGain = 0.0, aggrEvictSize = 0.0;
        double maxGainThshld = assess(item, sc) / item.sizeInMBs();

        long minSpaceRequired = item.sizeInBytes();
        // with high chance of error during comparisons

        Set<Chunk> optForEviction = new HashSet<>();
        long freeSpace = sc.getCacheAvailable(this);

        NEXT_CACHED_ITEM:
        while (!orderedCached.isEmpty() && freeSpace < minSpaceRequired) {
            Chunk minItem = orderedCached.poll();

            Set<CachingUser> cus = null; // MUs currently requesting minItem from sc 

            PCDemand.RegistrationInfo dmdRegInfo // info about the currently 
                    // requested cache space by this SC 
                    = sc.dmdRegInfoPC(minItem, this);
            if (dmdRegInfo != null) {/* 
                * Otherwise if null, then no mobile is currently 
                * requesting for minItem, i.e. it has remained in the cache
                * by prior requests which are not active any more.
                *
                * Therefore, for the else case, let it be tested for being removed..
                 */

                cus = dmdRegInfo.cachingUsers();

                if (checkAbortRplc(dmdRegInfo, sc)) {
                    continue NEXT_CACHED_ITEM;
                }

            }

/////kanonas = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = =   
//            PCDemand.RegistrationInfo dmdRegInfo // info about the currently 
//                    // requested cache space by this SC 
//                    = sc.dmdRegInfoPC(minItem);
//
            if (dmdRegInfo != null) {
                if (!dmdRegInfo.cachingUsers().isEmpty()) {// dont check if legacy cached    
                    aggrEvictSize += minItem.sizeInMBs();
                    aggrEvictGain += assess(minItem, sc);
                    if (aggrEvictGain / aggrEvictSize > maxGainThshld) {
                        return new HashSet<>();// abort and return an empty set    
                    }
                }
            }
//// = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = =  
            optForEviction.add(minItem);
            freeSpace += minItem.sizeInBytes();
        }

        if (freeSpace < minSpaceRequired) {
            return new HashSet<>();// return an empty set. Cannot find enought space
        }

        return optForEviction;
    }
}
