package caching;

import caching.base.AbstractCachingModel;
import caching.base.AbstractPop;
import caching.base.IEMC;
import caching.base.IEMPC;
import caching.base.IPop;
import exceptions.CriticalFailureException;
import exceptions.WrongOrImproperArgumentException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import sim.content.Chunk;
import sim.content.request.DocumentRequest;
import sim.space.cell.demand_registry.PCDemand;
import sim.space.cell.smallcell.ITimeBuffer;
import sim.space.cell.smallcell.SmallCell;
import sim.space.users.CachingUser;
import sim.space.users.StationaryUser;
import sim.space.users.mobile.MobileUser;
import utilities.Couple;

/**
 * A utility class with methods for:
 *
 * Canceling caching requests
 *
 *
 * @author Xenofon Vasilakos xvas@aueb.gr
 */
public abstract class Utils {

    public static final double SMOOTH_FACTOR_FOR_LEGACY_VALUE = 0.95;

    /**
     * Tries to cancel each cached item requested by the mobile, unless it is
     * also cached on behalf of other mobiles.
     *
     * @param cu
     * @param model
     * @param targetSC
     * @param nxtRequest
     * @return the canceled requests
     */
    public static Set<Chunk> cancelCachingOrders(MobileUser cu,
            AbstractCachingModel model, SmallCell targetSC, DocumentRequest nxtRequest) {

        Set<Chunk> itemsTotallyRmvd = new HashSet<>();

        for (Chunk nxtReqChunk : nxtRequest.referredContentDocument().chunks()) {
            if (targetSC.bufferContains(model, cu, nxtReqChunk)) {
                Set<CachingUser> stillRequesting = targetSC.bufferTryEvict(cu, model, nxtReqChunk);
                if (stillRequesting.isEmpty()) {
                    /*
                     * Note: There may be still requesting mus, but they may be requesting
                     * with zero probability, because the item was previously
                     * cached!
                     */
                    itemsTotallyRmvd.add(nxtReqChunk);
                }

            }
        }
        return itemsTotallyRmvd;
    }

    public static Set<Chunk> optForEvictionLRUAccess(ITimeBuffer lruBuffer, Chunk item) {

        long minSpaceRequired = item.sizeInBytes();
        long freeSpace = lruBuffer.availableSpaceInBytes();

        Set<Chunk> optToEvict = new HashSet<>();

        Couple<Integer, Set<Chunk>> leastRecentlyAccessed = lruBuffer.getLeastRecentlyAccessed();
        if (leastRecentlyAccessed == null) {
            return new HashSet<>();
        }
        Set<Chunk> lRUAccessedSet = new HashSet(leastRecentlyAccessed.getSecond());
        do {
            Iterator<Chunk> lruIter = lRUAccessedSet.iterator();

            while (lruIter.hasNext() && freeSpace < minSpaceRequired) {
                Chunk nxtItem = lruIter.next();
                optToEvict.add(nxtItem);
                freeSpace += nxtItem.sizeInBytes();
            }

            leastRecentlyAccessed = lruBuffer.getLeastRecentlyAccessed();
            lRUAccessedSet = new HashSet(leastRecentlyAccessed.getSecond());
        } while (!lRUAccessedSet.isEmpty() && freeSpace < minSpaceRequired);

        if (freeSpace < minSpaceRequired) {
            return new HashSet<>();// return an empty set. Cannot find enought space
        }

        return optToEvict;
    }

    public static final boolean isSpaceAvail(AbstractCachingModel cacheModel, SmallCell sc, long size) {
        return sc.buffAvailable(cacheModel) >= size;
    }

//    public static double assessAvgEMPC(Chunk item, SmallCell sc, AbstractPop cacheModel) throws Throwable {
//        PCDemand.RegistrationInfo nfo = sc.dmdRegInfoPC(item);
//        double avgProb = nfo != null ? nfo.sumTransProbs() / nfo.cachingUsers().size() : 0.0;
//
//        double q = avgProb;
//        double f = sc.dmdPopularity(item.referredContentDocument(), cacheModel);
//        double w = Utils.computeAvgW(sc);
//        //<editor-fold defaultstate="collapsed" desc="tmp commented">
////      sc.getSim().getStatsHandle().updtSCCmpt5(
////            f,
////            new UnonymousCompute5(
////                  cacheModel, UnonymousCompute5.WellKnownTitle.F_POP
////            )
////      );
////      sc.getSim().getStatsHandle().updtSCCmpt5(
////            q,
////            new UnonymousCompute5(
////                  cacheModel, UnonymousCompute5.WellKnownTitle.Q_POP
////            )
////      );
////</editor-fold>
//        return (q + w * f) * gainOfTransferSC(item, sc);
//    }
//
//   
//    public static double assessAvgEMC(Chunk item, SmallCell sc, AbstractEPC cacheModel) throws Throwable {
//        PCDemand.RegistrationInfo nfo = sc.getDmdPC().getRegisteredInfo(item);
//        double prob = nfo != null ? nfo.sumTransProbs() / nfo.cachingUsers().size() : 0;
//        return prob * gainOfTransferSC(item, sc);
//    }
    public static double assessEMC(Chunk theChunk, SmallCell sc, IEMC emc) {
        PCDemand.RegistrationInfo nfo = sc.dmdRegInfoPC(theChunk, (AbstractCachingModel) emc);
        double Q = nfo != null ? nfo.sumTransProbs() : 0.0;

        double assessment = Q * theChunk.gainOfTransferSCCacheHit();
        return assessment;
    }

    public static double assessEMPC(Chunk theChunk, SmallCell sc, IEMPC iemc) {

        PCDemand.RegistrationInfo nfo = sc.dmdRegInfoPC(theChunk, (AbstractCachingModel) iemc);
        double Q = nfo != null ? nfo.sumTransProbs() : 0.0;
        double f = sc.dmdPopularity(theChunk.referredContentDocument(), iemc);
        double w = sc.getDmdLclForW().computeAvgW();

        if (w == -1) {
            w = 0;
            //hack.. 
        }

        double assessment = (Q + w * f) * theChunk.gainOfTransferSCCacheHit();
        //<editor-fold defaultstate="collapsed" desc="tmp commented">
//@todo commented to save computation      sc.getSim().getStatsHandle().updtSCCmpt5(
//            f,
//            new UnonymousCompute5(
//                  this, UnonymousCompute5.WellKnownTitle.F_POP
//            )
//      );
//      sc.getSim().getStatsHandle().updtSCCmpt5(
//            q,
//            new UnonymousCompute5(
//                  this, UnonymousCompute5.WellKnownTitle.Q_POP
//            )
//      );
//</editor-fold>
        return assessment;
    }

    public static double assessEPCWithPop(CachingUser cu, Chunk item, SmallCell sc,
            IPop cacheModel) {

        double q = 0.0;
        if (cu == null) {
            q = 0.0;
        } else {

            if (cu instanceof MobileUser) {
                q = sc.simCellRegistry().handoverProbability(((MobileUser) cu).getUserGroup(), cu.getCurrentlyConnectedSC(), sc);
            }
            if (cu instanceof StationaryUser) {
                q = 1.0;
            }

            throw new UnsupportedOperationException();

        }

        double f = sc.dmdPopularity(item.referredContentDocument(), cacheModel);
        double w = sc.getDmdLclForW().computeAvgW();

        return (q + w * f) * item.gainOfTransferSCCacheHit();
    }

    public static double assessOnlyPop(AbstractPop cacheModel, Chunk item,
            SmallCell sc) {
        double f = sc.dmdPopularity(item.referredContentDocument(), cacheModel);
        return f * item.gainOfTransferSCCacheHit();
    }

    public static double assessEPC(CachingUser cu, Chunk item, SmallCell sc) {
        double prob = 1.0;

        if (cu instanceof MobileUser) {
            prob = sc.simCellRegistry().handoverProbability(((MobileUser) cu).getUserGroup(), cu.getCurrentlyConnectedSC(), sc);
            return prob * item.gainOfTransferSCCacheHit();
        }
        if (cu instanceof StationaryUser) {
            return item.gainOfTransferSCCacheHit();
        }

        throw new CriticalFailureException(new WrongOrImproperArgumentException());
    }

    public static String toMB(double bytes) {
        return "" + (bytes / 1024.0 / 1024.0);
    }

    public static String toMB(long bytes) {
        return "" + (bytes / 1024.0 / 1024.0);
    }

}
