/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package sim.space.cell.smallcell;

import caching.base.AbstractCachingPolicy;
import caching.interfaces.rplc.IRplcBase;
import exceptions.InconsistencyException;
import java.lang.invoke.WrongMethodTypeException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import sim.run.SimulationBaseRunner;
import sim.content.Chunk;
import sim.space.users.CachingUser;
import sim.space.users.mobile.MobileUser;
import utils.CommonFunctions;

/**
 * The base buffer for all types of buffers. This base class in particular can
 * be _used for naive methods of caching.
 *
 * @author Xenofon Vasilakos xvas@aueb.gr
 */
public class BufferBase {

    /**
     * The issue time of the last request encountered.
     */
    private double _lastRequestTime;
    /**
     * The issue time of the penultimate request encountered.
     */
    private double _penultimateRequestTime;

    /**
     * The _cell that this buffer is installed.
     */
    protected final SmallCell _cell;
    /**
     * this buffer's capacity.
     */
    private final double _capacityInBytes; // double for it helps computations e.g. when deviding for utilization percetage

    protected long _used;

    /**
     * The mobiles that have cached the item.
     */
    protected final Map<Chunk, Set<CachingUser>> _cachingUsersPerChunk;
    protected final Map<CachingUser, Set<Chunk>> _chunksPerCachingUser;
    protected final SimulationBaseRunner _simulation;

    public BufferBase(SimulationBaseRunner sim, SmallCell cell, long capacity) throws InconsistencyException {
        _lastRequestTime = -1;
        _penultimateRequestTime = -1;
        _simulation = sim;
        _cell = cell;
        _cachingUsersPerChunk = new HashMap<>(5);
        _chunksPerCachingUser = new HashMap<>();
        _capacityInBytes = capacity;
        _used = 0;
    }

    public boolean isCached(Chunk item) {
        return _cachingUsersPerChunk.containsKey(item);
    }

    public boolean hasCached(CachingUser cu) {
        return _chunksPerCachingUser.containsKey(cu);
    }

    public boolean hasCachedMapping(CachingUser cu, Chunk chunk) {
        Set<CachingUser> musOfChunk = _cachingUsersPerChunk.get(chunk);
        Set<Chunk> chunksOfMu = _chunksPerCachingUser.get(cu);

        return musOfChunk != null && chunksOfMu != null && musOfChunk.contains(cu) && chunksOfMu.contains(chunk);

//        boolean contains
//                = // 1)
//                musOfItm != null
//                && //2)
//                itmsOfMu != null
//                        ? musOfItm.contains(cu) && itmsOfMu.contains(item)
//                        : false;
//        return contains;
    }

    /**
     * Return an unmodifiable set with the currently cached items
     *
     * @return the items that are currently cached
     */
    public Set<Chunk> cachedChunksUnmodifiable() {
        return Collections.unmodifiableSet(_cachingUsersPerChunk.keySet());
    }

    /**
     * Tries to evict the item. The item may not be totally evicted due to
     * caching policy constraints or due to remaining cache orders by other
     * mobiles.
     *
     * If allowed by the caching policy, then if cu is the only one currently
     * caching this item, then this method forces the eviction of the item.
     *
     * Note that if cu is passed as null, then this method forces the eviction
     * of the item regardless of the caching policy.
     *
     * Caching policies that use replacement cause this method invokation to
     * only update the state about requesting mobiles. Finally, it returns the
     * list of mobiles still requesting the cached item. If the latter is empty,
     * then this means that the item was evicted and no requestors are left.
     *
     *
     * @param theChunk
     * @param cu
     * @param policy
     * @param sc
     * @return the list of mobiles still requesting the cached item.
     * @throws NoSuchElementException
     * @throws InconsistencyException
     */
    public Set<CachingUser> deallocateTry(
            Chunk theChunk, MobileUser cu, AbstractCachingPolicy policy, SmallCell sc
    ) throws NoSuchElementException, InconsistencyException {

        if (cu == null) {
            throw new WrongMethodTypeException("Can not deallocate for a null user. "
                    + "Use deallocateForce for forcing the deallocation of chunk: " + theChunk.toSynopsisString());
        }

        // else ..  in this case let the item removed only if it is cached only for the cu
        Set<Chunk> itemsOfMu = _chunksPerCachingUser.get(cu);
        if (itemsOfMu == null) {
            throw new NoSuchElementException(_simulation.simTime()
                    + ": [NULL] No item to evict for cu: " + cu.getID());
        }

        if (!itemsOfMu.remove(theChunk)) {
            throw new InconsistencyException(_simulation.simTime()
                    + ": No such item " + theChunk + " cached for mobile " + cu.getID()
                    + " the items cached for this mobile are " + CommonFunctions.toString(itemsOfMu)
            );
        }

        if (itemsOfMu.isEmpty()) {
            _chunksPerCachingUser.remove(cu);
        }

        Set<CachingUser> mobs = _cachingUsersPerChunk.get(theChunk);

        // cu != null for sure, so in this case add back the record of mobiles for the item if there are more mobile requestors
        if (!mobs.remove(cu)) {
            throw new InconsistencyException(_simulation.simTime()
                    + ": record kept for item " + theChunk + " but not for mobile user " + cu.getID()
                    + " Mobiles registered for order this item are: " + CommonFunctions.toString(mobs)
            );
        }

        // do not evict if the method uses its own replacement policy
        if (mobs.isEmpty() && !(policy instanceof IRplcBase)) {
            _used -= theChunk.sizeInBytes();
            _cachingUsersPerChunk.remove(theChunk);
        }
        return Collections.unmodifiableSet(mobs);
    }

    protected void deallocateForce(Chunk theChunk) {
        // mobs stays removed
        Set<CachingUser> cachingUsers = new HashSet(_chunksPerCachingUser.keySet());
        for (CachingUser nxtCU : cachingUsers) {
            Set<Chunk> chunksOfNxtCU = null;
            if ((chunksOfNxtCU = _chunksPerCachingUser.get(nxtCU)) == null) {
                continue;
            }

            chunksOfNxtCU.remove(theChunk); // try to remove the item
            if (chunksOfNxtCU.isEmpty()) {
                _chunksPerCachingUser.remove(nxtCU);
            }
        }

        _used -= theChunk.sizeInBytes();
        _cachingUsersPerChunk.remove(theChunk);
    }

    public boolean isEmpty() {
        return _chunksPerCachingUser.isEmpty();
    }

    public long availableSpaceInBytes() {
        return (long) (_capacityInBytes - _used);
    }

    public final double availableSpaceInMBs() {
        return availableSpaceInBytes() / Math.pow(1024, 2);
    }

    public final double availableSpaceInMBsRounded() {
        return Math.round(10 * availableSpaceInMBs()) / 10.0;
    }

    /**
     * The mobiles that have cached an item, per item cached. Note that map is
     * unmodifiable.
     *
     * @return the mobiles that have cached an item, per item cached in the form
     * of an unmodifiable map
     */
    public Map<Chunk, Set<CachingUser>> getItemCachers() {
        return Collections.unmodifiableMap(_cachingUsersPerChunk);
    }

    /**
     * An unmodifiable set of mobiles that have cached the item. If null, then
     * there are no caching mobiles for the item, which is valid when using
     * replacement policies.
     *
     * item
     *
     * @return the set of mobiles that have cached the item or null if there are
     * no mobiles currently caching the item.
     */
    public Set<CachingUser> getCachers(Chunk item) {
        Set<CachingUser> cachers = _cachingUsersPerChunk.get(item);
        if (cachers == null) {
            return new HashSet<>();
        }
        return new HashSet(cachers);
    }

    Set<Chunk> getCached(CachingUser cu) {
        Set<Chunk> itms = _chunksPerCachingUser.get(cu);

        if (itms == null) {
            return new HashSet<>();
        }

        return new HashSet<>(itms);
//      return itms == null ? null : Collections.unmodifiableSet(itms);
    }

    /**
     * The size of the currently _used buffered capacity.
     *
     * @return the _used
     */
    public long getUsed() {
        return _used;
    }

    public void clear() {
        _cachingUsersPerChunk.clear();
        _chunksPerCachingUser.clear();
        _used = 0;
    }

    /**
     * @return the capacity of the buffer capacity in bytes
     */
    public double getCapacityInBytes() {
        return _capacityInBytes;
    }

    public enum BufferAllocationStatus {

        ItemAlreadyCachedByOtherMUs(3), ItemAlreadyCachedBySameMU(2), FailItemOverUtilizes(1), Success(0), Unknown(-1);

        private static int max = 4;
        private static BufferAllocationStatus[] numericsMap = new BufferAllocationStatus[max];

        static {
            numericsMap[0] = Success;
            numericsMap[1] = FailItemOverUtilizes;
            numericsMap[2] = ItemAlreadyCachedBySameMU;
            numericsMap[3] = ItemAlreadyCachedByOtherMUs;
        }

        private double _numeric;

        private BufferAllocationStatus(int numeric) {
            _numeric = numeric;
        }

        public double numericValue() {
            return _numeric;
        }

        /**
         *
         * numeric
         *
         * @return the BufferAllocationStatus that corresponds to the numeric
         * value or unknown for wrong or unknown numerics.
         */
        public static BufferAllocationStatus getStatusForNumeric(int numeric) {
            return max < numeric || numeric < 0 ? Unknown : numericsMap[numeric];
        }

    }

    public Set<Chunk> getCachedItems() {
        return Collections.unmodifiableSet(_cachingUsersPerChunk.keySet());
    }

    /**
     * Tries to cache the item and returns a status integer value.
     *
     * cu chunk cachingMthd sc
     *
     * @return Success if the items is successfully cached, otherwise
     * ItemAlreadyCachedByOtherMUs if an item with the same id already exists in
     * the buffer, or FailItemOverUtilizes if there is not enough available
     * space for caching the item.
     */
    BufferAllocationStatus allocateAttempt(CachingUser cu, Chunk chunk, SmallCell sc) {
        if (_cachingUsersPerChunk.containsKey(chunk)) {
            if (_chunksPerCachingUser.containsKey(cu)) {
                if (_chunksPerCachingUser.get(cu).contains(chunk)) {
                    return BufferAllocationStatus.ItemAlreadyCachedBySameMU;
                }
            }// else case take care by code below

            _cachingUsersPerChunk.get(chunk).add(cu);
            Set<Chunk> itemsOfMU = _chunksPerCachingUser.get(cu);
            if (itemsOfMU != null) {
                itemsOfMU.add(chunk);
            } else {
                itemsOfMU = new HashSet<>();
                itemsOfMU.add(chunk);
                _chunksPerCachingUser.put(cu, itemsOfMU);
            }

            return BufferAllocationStatus.ItemAlreadyCachedByOtherMUs;
        }

        if (_used + chunk.sizeInBytes() > _capacityInBytes) {
            return BufferAllocationStatus.FailItemOverUtilizes;
        }

        // if reached so far, it is ok to add ..
        Set<CachingUser> cusOfItm = _cachingUsersPerChunk.get(chunk);
        if (cusOfItm == null) {
            cusOfItm = new HashSet();
            _cachingUsersPerChunk.put(chunk, cusOfItm);
        }
        cusOfItm.add(cu);

        Set<Chunk> itmSet = _chunksPerCachingUser.get(cu);
        if (itmSet == null) {
            itmSet = new HashSet<>();
            _chunksPerCachingUser.put(cu, itmSet);
        }
        itmSet.add(chunk);

        _used += chunk.sizeInBytes();
        return BufferAllocationStatus.Success;
    }

    /**
     * Special allocation attempt on behalf of no user. This method is useful
     * during a warmup phase while pre-loading data to the cache.
     *
     * chunk sc
     *
     * @return
     */
    BufferAllocationStatus initCacheAttempt(Chunk chunk, SmallCell sc) {
        if (_cachingUsersPerChunk.containsKey(chunk)) {
            return BufferAllocationStatus.ItemAlreadyCachedByOtherMUs;
        }

        if (_used + chunk.sizeInBytes() > _capacityInBytes) {
            return BufferAllocationStatus.FailItemOverUtilizes;
        }

        // if reached so far, it is ok to add ..
        Set<CachingUser> cusOfItm = _cachingUsersPerChunk.get(chunk);
        if (cusOfItm == null) {
            cusOfItm = new HashSet();
            _cachingUsersPerChunk.put(chunk, cusOfItm);
        }

        _used += chunk.sizeInBytes();
        return BufferAllocationStatus.Success;
    }

    void allocate(CachingUser cu, Chunk item, SmallCell sc) {
        BufferAllocationStatus result = allocateAttempt(cu, item, sc);

        if (result != BufferAllocationStatus.Success) {
            throw new InconsistencyException("Can not cache. Reason: " + result);
        }

//        if (_used + item.sizeInBytes() > _capacityInBytes) {
//            return BufferAllocationStatus.FailItemOverUtilizes;
//        }
//
//        // if reached so far, it is ok to add ..
//        Set<CachingUser> musOfItm = _cachingUsersPerChunk.get(item);
//        if (musOfItm == null) {
//            musOfItm = new HashSet();
//            _cachingUsersPerChunk.put(item, musOfItm);
//        }
//        musOfItm.add(cu);
//
//        Set<Chunk> itmSet = _chunksPerCachingUser.get(cu);
//        if (itmSet == null) {
//            itmSet = new HashSet<>();
//            _chunksPerCachingUser.put(cu, itmSet);
//        }
//        itmSet.add(item);
//
//        _used += item.sizeInBytes();
//        return BufferAllocationStatus.Success;
    }

    /**
     * Adds a cacher provided the item is already cached. Caution: this methods
     * does not allocate space for the item. use initCacheAttempt() instead.
     *
     * cu item
     */
    void addCacher(CachingUser cu, Chunk item) {
        if (_cachingUsersPerChunk.containsKey(item)) {
            _cachingUsersPerChunk.get(item).add(cu);
        } else {
            throw new InconsistencyException("Item not cached. Item id = " + item);
        }

        Set<Chunk> muItems = _chunksPerCachingUser.get(cu);
        if (muItems == null) {
            _chunksPerCachingUser.put(cu, (muItems = new HashSet<>()));
        }
        muItems.add(item);
    }

    /**
     * Removes a cacher provided the item is already cached. Caution: this
 methods does not deallocateTry space for the item. use deallocateTry() instead.

 cu item
     */
    public void removeCacher(CachingUser cu, Chunk chk) {
        Set<CachingUser> musOfItm;
        if ((musOfItm = _cachingUsersPerChunk.get(chk)) == null) {
            return;
//            throw new InconsistencyException(
//                    "Can not remove item "
//                    + chk.toSynopsisString() + " which seems not to be "
//                    + "curently in the cache."
//            );
        }

        musOfItm.remove(cu);
        /**
         * Caution1: leave it empty if no mus left caching.. can happen with
         * replacement policies ... Caution 2: if null pointer exception, then
         * this was a wrong call
         */

        Set<Chunk> chunksOfMu;
        if ((chunksOfMu = _chunksPerCachingUser.get(cu)) == null) {
            return;
        }

        // dont check. after all it will throw a null pointer exception if it is null
//        if (chunksOfMu == null) {
//            throw new InconsistencyException(
//                    "Cannot remove item " + item.getID()
//                    + " for mobile user " + cu.getID()
//                    + ". There are no items registered as cached for the particular user."
//            );
//        }
        if (!chunksOfMu.contains(chk)) {
            return;
//            throw new InconsistencyException(
//                    "Chunk not cached for particular mobile user."
//                    + " Mobile id = " + cu.getID()
//                    + " Chunk id = " + chk.getID()
//            );
        }

        chunksOfMu.remove(chk);
        if (chunksOfMu.isEmpty()) {
            _chunksPerCachingUser.remove(cu);
        }

    }

    public Set<CachingUser> cachers(Chunk item) {
        return _cachingUsersPerChunk.get(item) == null ? new HashSet() : _cachingUsersPerChunk.get(item);
    }

    /**
     * pollSize the (total) size the item(s) polled for eviction/addition evict
     *
     * @return if polled utilization GT 1.0, it means that there is not enough
     *
     * @throws InconsistencyException in case the polled utilization is negative
     * space for adding item(s).
     */
    public double utilizationPollAndCheck(long pollSize, boolean evict) throws InconsistencyException {
        double utilPolled = evict
                ? (_used - pollSize) / _capacityInBytes
                : (_used + pollSize) / _capacityInBytes;
        if (utilPolled < 0) {
            throw new InconsistencyException(
                    "Negative utilization polled: "
                    + utilPolled + " after polling for size "
                    + pollSize + ". Already Used space: "
                    + _used + ", capacity: " + _capacityInBytes
            );
        }
        return utilPolled;
    }

    /**
     * @return the current buffer utilization.
     */
    public double utilization() {
        return _used / _capacityInBytes;
    }

    //    
//                double lastRequestTime = nxtSC.getLastRequestTime();
//                double penultimateRequestTime = nxtSC.getPenultimateRequestTime();
    public double getLastRequestTime() {
        return _lastRequestTime;
    }

    public double getPenultimateRequestTime() {
        return _penultimateRequestTime;
    }

    /**
     * _lastRequestTime the _lastRequestTime to set
     */
    public void setLastRequestTime(double _lastRequestTime) {
        this._lastRequestTime = _lastRequestTime;
    }

    /**
     * _penultimateRequestTime the _penultimateRequestTime to set
     */
    public void setPenultimateRequestTime(double _penultimateRequestTime) {
        this._penultimateRequestTime = _penultimateRequestTime;
    }

}
