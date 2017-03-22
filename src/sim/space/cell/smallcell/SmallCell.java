package sim.space.cell.smallcell;

import static app.properties.Caching.CACHING__RPLC__MINGAIN__SUM__HEURISTIC__TIME__DYNAMIC__READJUSTMENT_PERIOD;
import app.properties.Space;
import app.properties.valid.Values;
import caching.MaxPop;
import caching.ModelsFactory;
import caching.base.AbstractCachingModel;
import caching.base.AbstractOracle;
import caching.base.AbstractPricing;
import caching.base.IPop;
import caching.interfaces.rplc.IGainRplc;
import caching.rplc.mingain.priced.tuned_timened.EMPC_R_Tunned_a;
import caching.rplc.mingain.priced.tuned_timened.EMPC_R_Tunned_b;
import caching.rplc.mingain.priced.tuned_timened.EMPC_R_Tunned_c1;
import caching.rplc.mingain.priced.tuned_timened.EMPC_R_Tunned_c2;
import exceptions.CriticalFailureException;
import exceptions.InconsistencyException;
import exceptions.InvalidOrUnsupportedException;
import exceptions.WrongOrImproperArgumentException;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import sim.Scenario;
import sim.run.SimulationBaseRunner;
import sim.space.Area;
import sim.content.Chunk;
import sim.content.ContentDocument;
import sim.content.request.DocumentRequest;
import sim.space.Point;
import sim.space.cell.AbstractCell;
import sim.space.cell.demand_registry.PCDemand;
import sim.space.users.CachingUser;
import sim.space.users.StationaryUser;
import sim.space.users.mobile.MobileUser;
import sim.time.NormalSimulationEndException;
import statistics.StatisticException;
import statistics.handlers.iterative.sc.cmpt5.UnonymousCompute5;
import traces.dmdtrace.TraceWorkloadRecord;
import static utils.CommonFunctions.PHI;

/**
 *
 * @author Xenofon Vasilakos xvas@aueb.gr
 */
public class SmallCell extends AbstractCell {

    /**
     * NONE is a special type of small _cell _used only to denote that no small
     * _cell exists. do not invoke methods for it always returns null or -1;
     */
    public static final Object NONE = new Object();
    public static final double EXPON_SMOOTH_W = 0.7;
    private final boolean _selfNeighborsAllowed;

    public SmallCell(
            int id, SimulationBaseRunner sim, Point center, double radius,
            Area area,
            Collection<AbstractCachingModel> cachingMdls, long capacity)
            throws InvalidOrUnsupportedException {

        super(id, sim, center.getY(), center.getX(), radius, area);

        _empcLCnoRplcInterval = new EPCLCnoRplcState(0, sim.getScenario().
                intProperty(app.properties.Caching.CACHING__RPLC__MINGAIN__SUM__HEURISTIC__TIME__DYNAMIC_MAX_BOUND));
        _smoothedHandoverDuration = sim.getScenario().doubleProperty(Space.SC__INIT_DURATION__HANDOVER);
        _smoothedResidenceDuration = sim.getScenario().doubleProperty(Space.SC__INIT_DURATION__RESIDENCE);

        String neighborhoodType = sim.getScenario().stringProperty(app.properties.Space.SC__NEIGHBORHOOD);
        _selfNeighborsAllowed
                = sim.getScenario().isTrue(Space.SC__NEIGHBORHOOD__ALLOW_SELF)
                || neighborhoodType.equalsIgnoreCase(Values.ALL_PLUS_SELF)
                || neighborhoodType.equalsIgnoreCase(Values.TRACE);

        _smoothedHandoversCount = 0;

        _proactCachingDmd = new HashMap();
        _scsInRange = new HashSet<>();

        _buffersMap = new HashMap<>(5);
        _orderedCachedByGainMap = new HashMap<>(170);
        fillMaps(cachingMdls, sim, capacity);

        area.addSC(this);
        area.updtCoverageByRadius(this);
    }

    /**
     * Loads configuration from sim properties. Takes care of coverage of points
     * based on properties, e.g., euclidian distance.
     *
     * @param sim
     * @param center
     * @param area
     * @param cachingMdls s
     *
     * @throws Exception
     */
    public SmallCell(SimulationBaseRunner sim, Point center, Area area,
            Collection<AbstractCachingModel> cachingMdls) throws Exception {
        this(sim, center,
                sim.getRandomGenerator().getGaussian(
                        sim.getScenario().doubleProperty(Space.SC__RADIUS__MEAN),
                        sim.getScenario().doubleProperty(Space.SC__RADIUS__STDEV)
                ),
                area,
                cachingMdls, utils.CommonFunctions.parseSizeToBytes(sim.getScenario()
                        .stringProperty(Space.SC__BUFFER__SIZE)
                ));
    }

    /**
     * Takes care of coverage of points based on properties, e.g., euclidian
     * distance.
     *
     * @param sim
     * @param center
     * @param radius
     * @param area
     * @param cachingPolicies
     * @param capacity buffer capacity in bytes
     * @throws InvalidOrUnsupportedException
     */
    public SmallCell(
            SimulationBaseRunner sim, Point center, double radius,
            Area area, Collection<AbstractCachingModel> cachingPolicies,
            long capacity) throws InvalidOrUnsupportedException {
        this(sim, center.getY(), center.getX(), radius,
                area, cachingPolicies, capacity);
    }

    /**
     * Takes care of coverage of points based on properties, e.g., euclidian
     * distance.
     *
     * @param sim
     * @param centerY
     * @param centerX
     * @param radius
     * @param area
     * @param cachingPolicies
     * @param capacity buffer capacity in bytes
     * @throws exceptions.InvalidOrUnsupportedException s
     */
    public SmallCell(
            SimulationBaseRunner sim, int centerY, int centerX, double radius, Area area,
            Collection<AbstractCachingModel> cachingPolicies, long capacity)
            throws InvalidOrUnsupportedException {
        super(sim, centerY, centerX, radius, area);

        _empcLCnoRplcInterval = new EPCLCnoRplcState(0, sim.getScenario().
                intProperty(app.properties.Caching.CACHING__RPLC__MINGAIN__SUM__HEURISTIC__TIME__DYNAMIC_MAX_BOUND));
        _smoothedHandoverDuration = sim.getScenario().doubleProperty(Space.SC__INIT_DURATION__HANDOVER);
        _smoothedResidenceDuration = sim.getScenario().doubleProperty(Space.SC__INIT_DURATION__RESIDENCE);

        String neighborhoodType = sim.getScenario().stringProperty(app.properties.Space.SC__NEIGHBORHOOD);
        _selfNeighborsAllowed
                = sim.getScenario().isTrue(Space.SC__NEIGHBORHOOD__ALLOW_SELF)
                || neighborhoodType.equalsIgnoreCase(Values.ALL_PLUS_SELF)
                || neighborhoodType.equalsIgnoreCase(Values.TRACE);

        _smoothedHandoversCount = 0;
        _proactCachingDmd = new HashMap();

        _scsInRange = new HashSet<>();

        _buffersMap = new HashMap<>(5);
        _orderedCachedByGainMap = new HashMap<>(170);
        fillMaps(cachingPolicies, sim, capacity);

        area.addSC(this);
        area.updtCoverageByRadius(this);
    }

    private void fillMaps(Collection<AbstractCachingModel> cachingPolicies, SimulationBaseRunner sim, long capacity)
            throws InvalidOrUnsupportedException {
        for (AbstractCachingModel thePolicy : cachingPolicies) {
            try {
                Class bufferType = ModelsFactory.bufferTypeOf(thePolicy);
                BufferBase newBf = (BufferBase) bufferType.getConstructor(SimulationBaseRunner.class, SmallCell.class, long.class
                ).newInstance(
                        sim, this, capacity
                );
                _buffersMap.put(thePolicy, newBf);
                _proactCachingDmd.put(thePolicy, new PCDemand(this));
                if (thePolicy instanceof IGainRplc) {
                    updtCachedChunksOrderedByGain((IGainRplc) thePolicy);
                }
            } catch (NoSuchMethodException | SecurityException |
                    InstantiationException | IllegalAccessException |
                    InvocationTargetException ex) {
                throw new InvalidOrUnsupportedException(ex);
            }
        }
    }

    /**
     * @return an approximation of the coverage size in square units based on
     * {@literal *pi * radius^2}
     */
    public int CoverageSize() {
        return (int) (Math.PI * Math.pow(getRadius(), 2));
    }

    /**
     * @return a randomly chosen point or null if the area is empty of points.
     */
    public Point randomPointInCoverage() {
        int size = getCoverageArea().size();
        if (size == 0) {
            return null;
        }
        int rnd = _sim.getRandomGenerator().randIntInRange(0, size - 1);
        int i = 0;
        for (Point nxt_point : getCoverageArea()) {
            if (i == rnd) {
                return nxt_point;
            }
            i++;
        }

        throw new RuntimeException("Wrong random number generated: " + rnd); // if reached up to here and not returned..
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(getClass().getSimpleName());
        builder.append("<");

        builder.append(toSynopsisString());

        builder.append("; center=");
        builder.append(getCoordinates().toSynopsisString());
        builder.append(";radius=");
        builder.append(getRadius());

        builder.append(";#neighbors=");
        builder.append(neighbors().size());

        builder.append(">");
        return builder.toString();
    }

    public double cachePrice(AbstractPricing cachingMdl) {
        return ((PricedBuffer) _buffersMap.get(cachingMdl)).getPrice();
    }

    public void setCachePrice(AbstractPricing cachingMdl, double price) throws Throwable {
        ((PricedBuffer) _buffersMap.get(cachingMdl)).setPrice(price);
    }

    public double cachePriceUpdt4Rplc(IGainRplc cachingMdl) throws Throwable {
        return ((PricedBuffer) _buffersMap.get(cachingMdl)).priceUpdt4Rplc(cachingMdl);
    }

    public double cachePrice4Rplc(IGainRplc cachingMdl) throws Throwable {
        return ((PricedBuffer) _buffersMap.get(cachingMdl)).getPrice4Rplc();
    }

    public double cacheUtilUpdt4Rplc(IGainRplc cachingMdl) throws Throwable {
        return ((PricedBuffer) _buffersMap.get(cachingMdl)).utilization4Rplc(cachingMdl);
    }

    /**
     *
     * @param cu
     * @param cachingMdl
     * @param item
     * @throws WrongOrImproperArgumentException
     */
    public void addCacher(CachingUser cu, AbstractCachingModel cachingMdl,
            Chunk item) throws WrongOrImproperArgumentException {
//@todo have no clue what to do with this piece of... code ..
//     initially, i thought like:   uncomment this        
//            if (!(cachingMdl instanceof IRplcBase)) {
//            throw new exceptions.WrongOrImproperArgumentException(
//                    "Method addCacher() must NOT be called if the caching policy in use"
//                    + "does not support replacements due to "
//                    + "empirical results which show that considering different "
//                    + "caching users without replacemt results in poor performance gains."
//            );
//        }
        _buffersMap.get(cachingMdl).addCacher(cu, item);
    }

    /**
     * @param cu
     * @param policy
     * @param item
     */
    public void removeCacher(CachingUser cu, AbstractCachingModel policy,
            Chunk item) {
        _buffersMap.get(policy).removeCacher(cu, item);
    }

    public Set<CachingUser> cachers(AbstractCachingModel policy,
            Chunk item) {
        return _buffersMap.get(policy).cachers(item);
    }

    /**
     * @param cu
     * @param cachingMdl
     * @param chunk
     * @return
     *
     */
    public BufferBase.BufferAllocationStatus cacheItemAttempt(CachingUser cu,
            AbstractCachingModel cachingMdl, Chunk chunk) {
        BufferBase.BufferAllocationStatus result
                = _buffersMap.get(cachingMdl).allocateAttempt(cu, chunk, this);
        return result;
    }

    public BufferBase.BufferAllocationStatus cacheItemAttemptPriceUpdate(CachingUser cu,
            AbstractPricing cachingMdl, Chunk chunk) {

        BufferBase.BufferAllocationStatus result
                = _buffersMap.get(cachingMdl).allocateAttempt(cu, chunk, this);

        return result;
    }

    public BufferBase.BufferAllocationStatus initCacheAttempt(
            AbstractCachingModel cachingMdl, Chunk chunk) {
        BufferBase.BufferAllocationStatus result
                = _buffersMap.get(cachingMdl).initCacheAttempt(chunk, this);
        return result;
    }

    public void cacheItem(CachingUser cu,
            AbstractCachingModel policy, Chunk item) throws Throwable {
        _buffersMap.get(policy).allocate(cu, item, this);
    }

    /**
     * @param cachingMdl the specified caching policy
     * @return the percentage of utilization of the buffer _used for the
     * specified caching policy.
     */
    public double buffUtilization(AbstractCachingModel cachingMdl) {
        return _buffersMap.get(cachingMdl).utilization();
    }

//    public double buffUtilizationPoll(boolean deallocateSpace,
//            AbstractCachingModel cachingMdl, Chunk... items) {
//        int totalSize = 0;
//        for (Chunk item : items) {
//            totalSize += item.sizeInBytes();
//        }
//        return _buffersMap.get(cachingMdl).utilizationPollAndCheck(totalSize, deallocateSpace);
//    }
//    public double buffUtilizationPoll(boolean deallocateSpace,
//            Set<Chunk> itemsSet, AbstractCachingModel cachingMdl) {
//        int totalSize = 0;
//        for (Chunk item : itemsSet) {
//            totalSize += item.sizeInBytes();
//        }
//        return _buffersMap.get(cachingMdl).utilizationPollAndCheck(totalSize, deallocateSpace);
//    }
//
//    public List<Chunk> deallocatePoll(AbstractCachingModel cachingMdl,
//            Chunk... requests) {
//        List<Chunk> itemsEvicted = new ArrayList<>();
//
//        if (!checkExistsFor(cachingMdl)) {
//            throw new InconsistencyException("No buffer found for caching policy " + cachingMdl);
//        }
//
//        for (Chunk nxt_item : requests) {
//            if (getBuffer(cachingMdl).isCached(nxt_item)) {
//                itemsEvicted.add(nxt_item);
//            }
//        }
//        return itemsEvicted;
//
//    }
    /**
     * Private method to maintain order of method calls for reasons documented
     * internally in this method.
     *
     * @param model
     * @param cacheRequestor
     * @param targetSC
     * @param predictedChunks
     * @param predictedChunksNaive the value of predictedChunksNaive
     * @throws InconsistencyException
     * @throws InvalidOrUnsupportedException
     */
    public void cacheDecisions(
            AbstractCachingModel model, CachingUser cacheRequestor, // MobileUser cacheRequestor,
            SmallCell targetSC, Collection<Chunk> predictedChunks,
            Collection<Chunk> predictedChunksNaive
    ) throws Throwable {

        if (!cacheRequestor.isAllowedToCache()) {
            return;
        }

        /**
         * ************************************************
         * Skip if any of the following.
         */
        if (model instanceof AbstractOracle/*
                *1) Oracle and subtypes have been taken 
                * cared off elsewhere..*/
                || model instanceof MaxPop /*
                *2) Taken cared of in simulation contructor..*/
                || (model instanceof caching.incremental.EMC
                && cacheRequestor instanceof StationaryUser)/*
                *3) Do not allow EMC for stationary users, as it would need 
                * mobility information, i.e. it would waste simulation time 
                * with zero mobile transition probabilities..*/) {
            return;
        }

        if (model instanceof caching.incremental.EMC) {
            ((caching.incremental.EMC) model).
                    cacheDecision(getSimulation(), cacheRequestor,
                            predictedChunks,
                            this, targetSC);
            return;
        }

        if (model instanceof caching.incremental.Naive) {
            ((caching.incremental.Naive) model).
                    cacheDecision(getSimulation(), cacheRequestor,
                            predictedChunks,
                            this, targetSC);
            return;
        }

        /**
         * *************************************************
         * Perform caching for types stemming from IGainRplc
         */
        if (model instanceof IGainRplc) {

            PriorityQueue<Chunk> cachedOrderByGain
                    = targetSC.getCachedChunksOrderedByGain((IGainRplc) model);

            Set<Chunk> rplcd = new HashSet();
            ((IGainRplc) model).
                    cacheDecision(getSimulation(), cacheRequestor, predictedChunks,
                            this, targetSC, rplcd, cachedOrderByGain);

            if (!rplcd.isEmpty()) {
                _sim.getStatsHandle().updtSCCmpt5(
                        rplcd.size(),
                        new UnonymousCompute5(
                                model, UnonymousCompute5.WellKnownTitle.ITMS_RPLCD
                        )
                );
            }

            return;
        }

//TODO recheck implementation of these policies            /*
//          * Perform caching for types stemming from ILRURplc
//             */
//            if (nxtCachingPolicy instanceof ILRURplc) {
//                Set<Item> rplcedNUm = 
//                ((ILRURplc) policy).cacheDecision(cu, targetSC);
//
//
//                continue;
//            }
        // for any other type of caching policy ..
        throw new exceptions.InvalidOrUnsupportedException(model.toString());

    }

    public Set<Chunk> pricedBuffcacheHits(AbstractCachingModel cachingMdl, Chunk[] requests) {
        Set<Chunk> hits = new HashSet<>();
        for (Chunk item : requests) {
            if (_buffersMap.get(cachingMdl).isCached(item)) {
                hits.add(item);
            }
        }
        return hits;
    }

    public Set<Chunk> buffCacheHits(AbstractCachingModel cachingMdl, Collection<Chunk> requests) {
        Set<Chunk> hits = new HashSet<>();
        for (Chunk item : requests) {
            BufferBase buffer = getBuffer(cachingMdl);
            if (buffer.isCached(item)) {
                hits.add(item);
            }
        }
        return hits;
    }

    public Set<Chunk> currentlyCached(AbstractCachingModel cachingMdl) {
        return _buffersMap.get(cachingMdl).cachedChunksUnmodifiable();
    }

    public Set<SmallCell> recomputeAntennasInRange__sc(Set<SmallCell> otherSCs) {
        for (SmallCell nxt_sc : otherSCs) {
            Point antennaPnt
                    = nxt_sc.getCoordinates();
            if (antennaPnt.isCoveredBy(this)) {
                _scsInRange.add(nxt_sc);
            }
        }

        return _scsInRange;
    }

    /**
     * Make sure that recomputeAntennasInRange__sc() is first called.
     *
     * @return
     */
    public Set<SmallCell> getAntennasInRange__sc() {
        return _scsInRange;
    }

    /**
     *
     * @param destCell
     * @return the number of handoffs recorded so far to this destination SC.
     */
    public int updtOutgoingHandoffs(SmallCell destCell) {
        this.totalOutgoingHandoffs++;
        Integer count = outgoingHandoffsPerCell.get(destCell);
        if (count == null) {
            count = 1;
            outgoingHandoffsPerCell.put(destCell, 1);
        }
        outgoingHandoffsPerCell.put(destCell, ++count);
        return count;
    }

    /**
     * A neighbor SC is _used as the key to map the number of handoffs to the
     * neighbor. The former implies that a SC "Dest" is neighbor of this SC
     * "Src" iff there is at least one outgoing RUN__CLASS handoff recorded from
     * Src to Dest.
     *
     * @param sc
     * @return
     */
    public int outgoingHandoffsTo(SmallCell sc) {
        Integer count = outgoingHandoffsPerCell.get(sc);
        return count == null ? 0 : count;
    }

    public Set<SmallCell> neighbors() {
        return Collections.unmodifiableSet(neighbors);
    }

    /**
     *
     * @return
     */
    public Map<SmallCell, Integer> getOutgoingHandoffsPerCell() {
        return Collections.unmodifiableMap(outgoingHandoffsPerCell);
    }

    public boolean hasAsNeighbor(SmallCell sc) {
        return neighbors.contains(sc);
    }

    public void addNeighbor(SmallCell sc) {
        if (this.equals(sc) && !_selfNeighborsAllowed) {
            throw new InconsistencyException("Neighbor to itself not allowed.");
        }
        neighbors.add(sc);
    }

    /**
     * @param cachingMdl the caching policy
     * @return the available tryCacheRecentFromBH space in the buffer
     * corresponding to the caching policy.
     */
    public long buffAvailable(AbstractCachingModel cachingMdl) {
        return _buffersMap.get(cachingMdl).availableSpaceInBytes();
    }

    /**
     *
     * @param cachingMdl
     * @return capacity of buffer capacity in bytes
     */
    public double buffCapacity(AbstractCachingModel cachingMdl) {
        return _buffersMap.get(cachingMdl).getCapacityInBytes();
    }

    public long buffUsed(AbstractCachingModel cachingMdl) {
        return _buffersMap.get(cachingMdl).getUsed();
    }

    public boolean bufferContains(AbstractCachingModel cachingMdl, CachingUser cu, Chunk item) {
        if (cu == null) {
            return _buffersMap.get(cachingMdl).isCached(item);
        }
        return _buffersMap.get(cachingMdl).hasCachedMapping(cu, item);
    }

    public Set<Chunk> bufferCached(AbstractCachingModel cachingMdl, CachingUser cu) {
        try {
            return _buffersMap.get(cachingMdl).getCached(cu);
        } catch (RuntimeException rte) {
            throw new RuntimeException(cachingMdl.toString(), rte);
        }
    }

    /**
     * Tries to evict. The item may not be totally evicted due to caching policy
     * constraints or due to remaining tryCacheRecentFromBH orders by other
     * mobiles.
     *
     * @param cu
     * @param policy
     * @param item
     * @return the list of mobiles still requesting the cached item, or null is
     * nothing was evicted.
     */
    public Set<CachingUser> bufferTryEvict(MobileUser cu, AbstractCachingModel policy, Chunk item)
            throws InconsistencyException {
        return _buffersMap.get(policy).deallocateAttempt(item, cu, policy, this);
    }

    public void bufferForceEvict(AbstractCachingModel cachingMdl, Chunk theChunk) throws InconsistencyException, Throwable {
        _buffersMap.get(cachingMdl).deallocateForce(theChunk);
    }

    public Map<Chunk, Set<CachingUser>> bufferItemCachers(AbstractCachingModel cachingMdl) {
        return _buffersMap.get(cachingMdl).getItemCachers();
    }

    /**
     * *
     *
     * @param cachingMdl
     * @param item
     * @return the mobiles that have requested this ID of a cached item.
     */
    public Set<CachingUser> bufferCachers(AbstractCachingModel cachingMdl, Chunk item) {
        return _buffersMap.get(cachingMdl).getCachers(item);
    }

    /**
     * Computes the popularity based on the latest two past requests recorded
     * for this item by previously connected mobile users to this cell.
     *
     * @param theContent
     * @param popMthd
     * @return the computed popularity, or 0 if no information is available for
     * the item
     */
    public double dmdPopularity(ContentDocument theContent, IPop popMthd) {

        switch (_computePopularityType) {
            case Values.POP__TYPE01:
                return getPopInfo().computePopularity1(theContent);
            case Values.POP__TYPE02:
                return getPopInfo().computePopularity2(theContent,
                        _buffersMap.get((AbstractCachingModel) popMthd));
            case Values.POP__TYPE03__GLOBAL:
                return getPopInfo().computePopularity3(theContent);
            default:
                throw new CriticalFailureException(_computePopularityType);
        }
    }

    /**
     * @param item the item
     * @return the set of mobiles currently demanding the item or null if no
     * such items currently exist
     */
    public Set<CachingUser> musCurrDmdProact(Chunk item, AbstractCachingModel policy) {
        PCDemand.RegistrationInfo registeredInfo = getDmdPC(policy).getRegisteredInfo(item);
        /* There may be no registered info anymore. This can happen with caching policies 
       * which allow cached items to remain cached even if no mobile requests anymore 
       *for the item.
         */
        return registeredInfo == null ? null : registeredInfo.cachingUsers();
    }

    /**
     * A registry for keeping track of information regarding the proactive
     * caching requests demand by the mobile users which may be handed over to
     * this small _cell.
     *
     * @param policy CAUTION: this is safely used only for a particular
     * proactive caching method that uses popularity legacy caching.
     * @return the dmdProactNum
     */
    public final PCDemand getDmdPC(AbstractCachingModel policy) {
        return _proactCachingDmd.get(policy);
    }

    public final void clearDmdPC(AbstractCachingModel policy) {
        _proactCachingDmd.get(policy).clear();
    }

    /**
     * @param item
     * @param policy
     * @return the currently registered information for proactive caching
     * requests regarding the item or null if no registration exists.
     */
    public PCDemand.RegistrationInfo dmdRegInfoPC(Chunk item, AbstractCachingModel policy) {
        return getDmdPC(policy).getRegisteredInfo(item);
    }

    /**
     * Checks if an item with the same id is already cached by any mobile user
     * according to the specified caching policy.
     *
     * @param cachingMdl
     *
     * @param chunk
     * @return
     */
    public boolean isCached(AbstractCachingModel cachingMdl, Chunk chunk) {
        return getBuffer(cachingMdl).isCached(chunk);
    }

    /**
     * Checks if an item with the same id is already cached by cu according to
     * the specified caching policy.
     *
     * @param cachingMdl
     * @param cu
     * @param item
     *
     * @return
     */
    public boolean isCachedBy(CachingUser cu, AbstractCachingModel cachingMdl, Chunk item) {
        return getBuffer(cachingMdl).hasCachedMapping(cu, item);
    }

    public BufferBase getBuffer(AbstractCachingModel cachingMdl) {
        return _buffersMap.get(cachingMdl);
    }

    public void clearBuffer(AbstractCachingModel cachingMdl) {
        _buffersMap.get(cachingMdl).clear();
    }

    public void clearBuffer(IGainRplc cachingMdl) {
        _buffersMap.get(cachingMdl).clear();
        _orderedCachedByGainMap.get(cachingMdl).clear();
    }

    public PricedBuffer getBuffer(AbstractPricing cachingMdl) {
        return (PricedBuffer) _buffersMap.get(cachingMdl);
    }

    public PriorityQueue<Chunk> getCachedChunksOrderedByGain(IGainRplc policy) {
        return _orderedCachedByGainMap.get(policy);
    }

    /**
     * Periodic use of this method is necessary to refresh the ordering in the
     * ordered priority queues of orderedCachedByGainMap, as gains assessment
     * per chunk change, thus the priority ordering must be updated too.
     *
     * @param policy
     */
    public void updtCachedChunksOrderedByGain(IGainRplc policy) {
        Set<Chunk> cachedChunks = getBuffer((AbstractCachingModel) policy).cachedChunksUnmodifiable();

        Comparator<Chunk> priorityComparator = policy.evictionPriorityComparator(this);
        PriorityQueue<Chunk> orderedCached = _orderedCachedByGainMap.get(policy);
        if (orderedCached == null) {
            orderedCached = new PriorityQueue<>(cachedChunks.size() + 1, priorityComparator);
        } else {
            orderedCached.clear();
        }
        orderedCached.addAll(cachedChunks);
        _orderedCachedByGainMap.put(policy, orderedCached);
    }

    public boolean checkExistsFor(AbstractCachingModel cachingMdl) {
        return _buffersMap.keySet().contains(cachingMdl);
    }

    public Set<Chunk> cachedChunksUnmodifiable(AbstractCachingModel cachingMdl) {
        return this.getBuffer(cachingMdl).cachedChunksUnmodifiable();
    }

    public long getCacheAvailable(AbstractCachingModel cachingMdl) {
        BufferBase buffer = this.getBuffer(cachingMdl);
        return buffer.availableSpaceInBytes();
    }

    public Set<CachingUser> registeredUsersDmdPC(Chunk item, AbstractCachingModel cachingMdl) {
        return this.musCurrDmdProact(item, cachingMdl);
    }

    /**
     * @return the epcLCnoRplcInterval
     */
    public EPCLCnoRplcState getEPCLCnoRplcState() {
        return _empcLCnoRplcInterval;
    }

    public boolean updtLclDmdByStationary(boolean force) throws
            NormalSimulationEndException, CriticalFailureException {

        boolean shouldLoad; // Should new requests be loaded?
//        shouldLoad = simTime() % _loadStationaryReqsJitter == 0;
        shouldLoad = simTime() % getSmoothedHandoverDuration() < 1;

        // only for the case of stationaries:
        stationaryUsr.setLastSojournTime(getSmoothedHandoverDuration());

        if (!shouldLoad && !force) {
            return false;
        }

        if (!force) {
            try {
                stationaryUsr.forceCompleteRequests();
                getSimulation().getStatsHandle().updtPerformanceStats(stationaryUsr);
            } catch (InvalidOrUnsupportedException | StatisticException ex) {
                throw new CriticalFailureException(ex);
            }
        }

///////////////////////        
        getDmdLclForW().deregisterLclDmdForW(stationaryUsr, 1);

        stationaryUsr.clearAllRequests();

///////////////////////
//load new requests
        getSimulation().loadFromWrkloadIfNeeded(_loadStationaryReqsNum);
        Map<Double, TraceWorkloadRecord> wrkLoad = getSimulation().getWrkLoad();

        Iterator<Map.Entry<Double, TraceWorkloadRecord>> iterator
                = wrkLoad.entrySet().iterator();

        int howManyToLoad = _loadStationaryReqsNum;
        while (iterator.hasNext() && --howManyToLoad > 0) {
            TraceWorkloadRecord nxtFromWorkload = iterator.next().getValue();
            iterator.remove();

            DocumentRequest r = new DocumentRequest(nxtFromWorkload, stationaryUsr);
            r.setConsumeReady(true);

            _sim.incrWrkloadConsumed();
            stationaryUsr.addRequest(r);
        }

        return true;
    }

    public void initLclDmdStationary() throws InconsistencyException, InvalidOrUnsupportedException {
        _dmdTrcStationaryReqsRateLoadedPerSC
                = getSimulation().getScenario().intProperty(Space.SC__DMD__TRACE__STATIONARY_REQUESTS__RATE);
        _dmdTrcStationaryReqsRateLoadedPerSCStdv
                = getSimulation().getScenario().intProperty(Space.SC__DMD__TRACE__STATIONARY_REQUESTS__STDEV);

        _loadStationaryReqsNum
                = // so that not all cells get syncrhonised on loading new requests.
                (int) getSimulation().getRandomGenerator().getGaussian(_dmdTrcStationaryReqsRateLoadedPerSC, _dmdTrcStationaryReqsRateLoadedPerSCStdv);

        if (_loadStationaryReqsNum <= 0) {
            _loadStationaryReqsNum = _dmdTrcStationaryReqsRateLoadedPerSC;
        }

        if (_loadStationaryReqsNum < 1) {
            throw new exceptions.InconsistencyException(
                    "Cannot adapt negative value \""
                    + _loadStationaryReqsNum
                    + "\"for number of reloaded "
                    + "stationary requests. Please adapt different "
                    + "mean and standard deviation parameter values.");
        }

        stationaryUsr = new StationaryUser("Stationary:" + getID(), _sim, _sim.simTime(),
                this, _sim.macrocell(),
                _sim.getCachingModels());
    }

    /**
     * @return the stationaryUsr
     */
    public StationaryUser getStationaryUsr() {
        return stationaryUsr;
    }

    public void updtSmoothedResidenceDuration(double val, double weight) {
        _smoothedResidenceDuration *= 1 - weight;//exponential smothing
        _smoothedResidenceDuration += weight * val;
    }

    public void updtAvgHandoverDuration(double duration, double weight) {
        _smoothedHandoversCount *= 1 - weight;
        _smoothedHandoversCount += weight;

        _smoothedHandoverDuration *= 1 - weight;//exponential smothing
        _smoothedHandoverDuration += weight * duration;
    }

    public double getSmoothedResidenceDuration() {
        return _smoothedResidenceDuration;
    }

    public int getSmoothedHandoversCount() {
        return _smoothedHandoversCount;
    }

    public double getSmoothedHandoverDuration() {
        return _smoothedHandoverDuration;
    }

    /**
     * Useful for a dynamic interval where no replacements take place during
     * tryCacheRecentFromBH decisions with EPC-LC
     */
    private final EPCLCnoRplcState _empcLCnoRplcInterval;
    private int _dmdTrcStationaryReqsRateLoadedPerSC;
    private int _dmdTrcStationaryReqsRateLoadedPerSCStdv;
    private int _loadStationaryReqsNum;

    /**
     * Buffers _used for benchmark algorithm caching decisions. Buffers are
     * mapped to the caching policy _used.
     */
    private final Map<AbstractCachingModel, BufferBase> _buffersMap;
    private final Map<IGainRplc, PriorityQueue<Chunk>> _orderedCachedByGainMap;

    /**
     * Other small cells in range of the current small _cell.
     */
    private final Set<SmallCell> _scsInRange;
    /**
     * Maps outgoing mobile handoffs from this small _cell to the destination
     * small _cell _used as a key in the map. A SC is _used as the key to map
     * the number of outgoing handoffs to that SC.
     */
    private final Map<SmallCell, Integer> outgoingHandoffsPerCell = new HashMap();
    /**
     * A counter for the total number of outgoing handoffs from this small
     * _cell.
     */
    private int totalOutgoingHandoffs;
    /**
     * The set of the neighboring small cells of this small _cell.
     */
    private final Set<SmallCell> neighbors = new HashSet<>();

    private final Map<AbstractCachingModel, PCDemand> _proactCachingDmd;
    private StationaryUser stationaryUsr;
    private double _smoothedResidenceDuration;
    private double _smoothedHandoverDuration;
    private int _smoothedHandoversCount;

    private final Map<Chunk, Integer> eMCTTL4Cached = new HashMap();//TODO ... More efficient implementation ->  change to a priority queue based on time inserted. 
    private final Map<Chunk, Integer> naiveTTL4Cached = new HashMap();//TODO ... More efficient implementation ->  change to a priority queue based on time inserted. 
    /**
     * Time to live threshold for EMC cached chunks
     */
    public static final int TTL = 1800;

    public void updtEMCTTL4Cached(Chunk ch, int tm) {
        eMCTTL4Cached.put(ch, tm);
    }

    public int checkEMCTTL4Cached(int tm, AbstractCachingModel model) {
        int sum = 0;
        Object[] chunks =  eMCTTL4Cached.keySet().toArray();
//        for (Chunk ch : eMCTTL4Cached.keySet()) {
        for (Object ob : chunks) {
            Chunk ch = (Chunk) ob;
            Integer whenLogged = eMCTTL4Cached.get(ch);
            if (whenLogged + tm > TTL) {
                eMCTTL4Cached.remove(ch);

                sum++;
                if (bufferContains(model, null, ch)) {
                    getBuffer(model).deallocateForce(ch);
                    //getDmdPC(model).deregisterUpdtInfoPC(this, ch);
                }
            }

        }
        return sum;
    }
    public void updtNAIVETTL4Cached(Chunk ch, int tm) {
        naiveTTL4Cached.put(ch, tm);
    }

    public int checkNAIVETTL4Cached(int tm, AbstractCachingModel model) {
        int sum = 0;
        Object[] chunks =  naiveTTL4Cached.keySet().toArray();
//        for (Chunk ch : eMCTTL4Cached.keySet()) {
        for (Object ob : chunks) {
            Chunk ch = (Chunk) ob;
            Integer whenLogged = naiveTTL4Cached.get(ch);
            if (whenLogged + tm > TTL) {
                naiveTTL4Cached.remove(ch);

                sum++;
                if (bufferContains(model, null, ch)) {
                    getBuffer(model).deallocateForce(ch);
                    //getDmdPC(model).deregisterUpdtInfoPC(this, ch);
                }
            }

        }
        return sum;
    }

//    private boolean checkEMCTTL4Cached(Chunk ch, int tm) {
//        Integer whenLogged = eMCTTL4Cached.get(ch);
//        if (whenLogged + tm > TTL) {
//            eMCTTL4Cached.remove(ch);
//            return true;
//        }
//        return false;
//    }

    /**
     * Useful state for a dynamic interval where no replacements take place
     * during tryCacheRecentFromBH decisions with EPC-LC
     */
    public final class EPCLCnoRplcState {

        /**
         * @return The performance difference between a and b which indicates
         * that the golden ratio search algorithm concluded.
         */
        public double getStopE() {
            Scenario scenario = SmallCell.this.getSimulation().getScenario();
            return scenario.intProperty(app.properties.Caching.CACHING__RPLC__MINGAIN__SUM__HEURISTIC__TIME__DYNAMIC__STOPE);
        }

        /**
         * The currently used interval
         */
        private double a;
        private double c1;
        private double c2;
        private double b;

        private final Map<String, Double> gainPerPolicy;
        private boolean optimumFound;

        public EPCLCnoRplcState(int aInit, int bInit) {

            this.optimumFound = false;
            this.gainPerPolicy = new HashMap<>(5);
            resetGains();

            /*initial values, which may be readjusted if we need to restart*/
            a = aInit;
            b = bInit;
            recomputeC1C2();

        }

        public final void recomputeC1C2() {
            c1 = (PHI - 1) * a + (2 - PHI) * b;
            c2 = (2 - PHI) * a + (PHI - 1) * b;
        }

        public double getGainA() {
            return gainPerPolicy.get(EMPC_R_Tunned_a.instance().toString());
        }

        public double getGainC1() {
            return gainPerPolicy.get(EMPC_R_Tunned_c1.instance().toString());
        }

        public double getGainC2() {
            return gainPerPolicy.get(EMPC_R_Tunned_c2.instance().toString());
        }

        public double getGainB() {
            return gainPerPolicy.get(EMPC_R_Tunned_b.instance().toString());
        }

        public double getA() {
            return a;
        }

        public double getC1() {
            return c1;
        }

        public double getC2() {
            return c2;
        }

        public double getB() {
            return b;
        }

        public void setA(double _a) {
            a = _a;
        }

        public void setC1(double _c1) {
            c1 = _c1;
        }

        public void setC2(double _c2) {
            c2 = _c2;
        }

        public void setB(double _b) {
            b = _b;
        }

        public void addGainFor(String cacheMthd, double gain) {
            // you only need the sum since the same number of handoffs 
            // take place per
            gainPerPolicy.put(cacheMthd, gainPerPolicy.get(cacheMthd) + gain);
        }

        public void resetGains() {
            gainPerPolicy.clear();
            gainPerPolicy.put(EMPC_R_Tunned_a.instance().toString(), 0.0);
            gainPerPolicy.put(EMPC_R_Tunned_c1.instance().toString(), 0.0);
            gainPerPolicy.put(EMPC_R_Tunned_c2.instance().toString(), 0.0);
            gainPerPolicy.put(EMPC_R_Tunned_b.instance().toString(), 0.0);
        }

        public void markOptimumFound() {
            setA(a + b / 2);
            setC1(a + b / 2);
            setC2(a + b / 2);
            setB(a + b / 2);
            optimumFound = true;
        }

        public boolean isOptimumFound() {
            return optimumFound;
        }

        /**
         * How often (in terms of simulation time) are the time intervals
         * updated.
         *
         * @return the _readjustmenyPeriod
         */
        public int getReadjustmenyPeriod() {
            Scenario scenario = SmallCell.this.getSimulation().getScenario();
            return scenario.intProperty(CACHING__RPLC__MINGAIN__SUM__HEURISTIC__TIME__DYNAMIC__READJUSTMENT_PERIOD);
        }

    }

}
