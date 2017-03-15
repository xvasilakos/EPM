package sim.content.request;

import app.properties.Space;
import caching.base.AbstractCachingModel;
import caching.incremental.Oracle;
import java.util.Collections;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import utils.ISynopsisString;
import sim.content.Chunk;
import sim.space.cell.smallcell.SmallCell;
import sim.space.users.CachingUser;
import sim.space.users.StationaryUser;
import traces.dmdtrace.TraceWorkloadRecord;

/**
 * 
 * @author Xenofon Vasilakos - xvas@aueb.gr, mm.aueb.gr/~xvas,
 * Mobile Multimedia Laboratory (mm.aueb.gr),
 * Dept. of Informatics, School of Information {@literal Sciences & Technology},
 * Athens University of Economics and Business, Greece
 */
public class DocumentRequest extends TraceWorkloadRecord implements ISynopsisString, IRequest {

    /**
     * The simulation time of request. This is not the same as the time loaded
     * from the trace workload.
     */
    protected final int _issuedAtSimTime; // either a stationary small cell user or a mobile user
    protected int _uncompletedPolicies;
    protected final CachingUser _requesterUser; // either a stationary small cell user or a mobile user
    protected final Map<AbstractCachingModel, List<Chunk>> _unconsumedChunksInSequence;
    protected final Map<AbstractCachingModel, Integer> _completitionTimes;
    protected final Map<AbstractCachingModel, List<Chunk>> _chunksConsumedHistoryFromMCWhileConnectedToSC;
    private final Map<AbstractCachingModel, List<Chunk>> _consumedHistoryFromMCwSCDiscon;
    private final Map<AbstractCachingModel, List<Chunk>> _chunksConsumedHistoryFromMCAfterExitingSC;
    protected final Map<AbstractCachingModel, List<Chunk>> _chunksConsumedHistoryFromBH;
    protected final Map<AbstractCachingModel, List<Chunk>> _chunksHitsHistoryFromSC;

    private boolean _consumeReady;

//    List<RequestedChunk> _consumedChunksInSequence = null;
    public DocumentRequest(TraceWorkloadRecord workloadRecord, CachingUser requesterUser) {

        super(workloadRecord.getSimulation(), workloadRecord.sizeInBytes(), workloadRecord.getID(), workloadRecord.getTime());
        _consumeReady = true;

        _issuedAtSimTime = simTime();

        _requesterUser = requesterUser;

        _unconsumedChunksInSequence = new HashMap<>(7);//new TreeMap(referredContentDocument().getChunksInSequence());
        _completitionTimes = new HashMap<>(50);
        _chunksConsumedHistoryFromMCWhileConnectedToSC = new HashMap<>(50);
        _consumedHistoryFromMCwSCDiscon = new HashMap<>(5);
        _chunksConsumedHistoryFromMCAfterExitingSC = new HashMap<>(25);
        _chunksHitsHistoryFromSC = new HashMap<>(25);
        _chunksConsumedHistoryFromBH = new HashMap<>(25);

        Collection<Chunk> chunksInSequence = referredContentDocument().getChunksInSequence().values();

        _uncompletedPolicies = getSimulation().getCachingStrategies().size();
        for (AbstractCachingModel model : getSimulation().getCachingStrategies()) {
            _unconsumedChunksInSequence.put(model, new ArrayList<>(chunksInSequence));
            _completitionTimes.put(model, -1);
            _chunksConsumedHistoryFromMCWhileConnectedToSC.put(model, new ArrayList<Chunk>());
            _consumedHistoryFromMCwSCDiscon.put(model, new ArrayList<Chunk>());
            _chunksConsumedHistoryFromMCAfterExitingSC.put(model, new ArrayList<Chunk>());
            _chunksHitsHistoryFromSC.put(model, new ArrayList<Chunk>());
            _chunksConsumedHistoryFromBH.put(model, new ArrayList<Chunk>());
        }
    }

    @Override
    public String toString() {
        StringBuilder bld = new StringBuilder();
        bld.append("super={").
                append(super.toString()).append("}")
                .append("-->" + "DocumentRequest").append("{")
                .append("_issuedAtSimTime=").append(_issuedAtSimTime)
                .append(", _uncompletedPolicies=").append(_uncompletedPolicies)
                .append(", _requesterUser=").append(_requesterUser.getClass().getSimpleName()).append(_requesterUser)
                .append(", _completitionTimes=").append(_completitionTimes)
                .append(", _consumeReady=").append(_consumeReady)
                .append(",  referredContentDocument synopsis:\n\t\t")
                .append(referredContentDocument().toSynopsisString())
                .append('}');

        return bld.toString();
    }

    @Override
    public String toSynopsisString() {
        StringBuilder bld = new StringBuilder();
        bld.append("DocumentRequest").append("{")
                .append("_issuedAtSimTime=").append(_issuedAtSimTime)
                .append(", _uncompletedPolicies=").append(_uncompletedPolicies)
                .append(", _requesterUser=").append(_requesterUser.getClass().getSimpleName()).append(_requesterUser)
                .append(", _completitionTimes=").append(_completitionTimes)
                .append(", _consumeReady=").append(_consumeReady)
                .append(",  referredContentDocument id:\n\t\t")
                .append(referredContentDocument().getID())
                .append('}');

        return bld.toString();
    }

    /**
     * Takes into account the simulation issue time and the hashcode of the
     * superclass. Avoids conflicts with same ID between different records
     * referring to the same item.
     *
     * @return
     */
    @Override
    public int hashCode() {
        int hash = super.hashCode();
        hash = 29 * hash + (int) (Double.doubleToLongBits(getIssuedAtSimTime())
                ^ (Double.doubleToLongBits(getIssuedAtSimTime()) >>> 32));
        return hash;
    }

    /**
     *
     * obj
     *
     * @return
     */
    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) {
            return false;
        }
        // super checks for getClass() != obj.getClass(), so it can be casted
        final DocumentRequest other = (DocumentRequest) obj;
        return getIssuedAtSimTime() == other.getIssuedAtSimTime();
    }

    @Override
    public long requesterUserID() {
        return getRequesterUser().getID();
    }

    @Override
    public int getIssuedAtSimTime() {
        return _issuedAtSimTime;
    }

    @Override
    public List<Chunk> predictChunks2Request(
            AbstractCachingModel model, double handoverProb,
            boolean isSoftMU, double expectedHandoffDuration,
            double conf95HandoffDur, double expectedResidenceDuration,
            double conf95ResidenceDur, int mcRateSliceBytes,
            int bhRateSliceBytes, int scRateSliceBytes) {

        if (isSoftMU) {
            return new ArrayList<>(referredContentDocument().getChunksInSequence().values());
        }

        List<Chunk> chunks = new ArrayList<>();// keep insertion order in returned 

//////////////////////        
//        if (predictChunksZeroProb(chunks, handoverProb)) {
//            return chunks;//skip fast
//        }
//////////////////////   
        List<Chunk> unconsumed = _unconsumedChunksInSequence.get(model);

        if (unconsumed.isEmpty()) {//early skip if possible
            return new ArrayList<>();//return empty in this case
        }

        double chunkSizeInBytes = getSimulation().chunkSizeInBytes();

        // Expected values based on parameter values.
        // Needed to compute which chunks wil be requested.
        long consumedFromMCDuringHandoff, consumedFromMCDuringHandoffConf;
        consumedFromMCDuringHandoff = consumedFromMCDuringHandoffConf = 0;

        if (!isSoftMU && _consumeReady) {
            /* If soft ussr, never consume from macro cell.
             * If not consume-ready yet, then it will start consuming only after
             * entering the first cell
             */
            consumedFromMCDuringHandoff
                    = Math.round(expectedHandoffDuration * mcRateSliceBytes / chunkSizeInBytes);
            consumedFromMCDuringHandoffConf
                    = Math.round(conf95HandoffDur * mcRateSliceBytes / chunkSizeInBytes);
        }

        long cachableChunksDuringHandoff
                = Math.round((expectedHandoffDuration + 2 * conf95HandoffDur) * bhRateSliceBytes / chunkSizeInBytes);

        long consumableChunksDuringSCConnection
                = Math.round((expectedResidenceDuration + 2 * conf95ResidenceDur) * scRateSliceBytes / chunkSizeInBytes);

        long firstChunkSequenceNum
                = unconsumed.get(0).getSequenceNum()// first unconsumed chunk in sequence
                + consumedFromMCDuringHandoff
                - consumedFromMCDuringHandoffConf;// conf interval
        firstChunkSequenceNum = Math.max(1, firstChunkSequenceNum);// to prohibit meaningless negative sequences

        SortedMap<Long, Chunk> chunksInSequence = referredContentDocument().getChunksInSequence();

//////////////////////                
//        if (predictChunksContentTooSmall(chunksInSequence.lastKey(), 
//                firstChunkSequenceNum, chunksInSequence, handoverProb, chunks)) {
//            return chunks;
//        }
        if (chunksInSequence.lastKey() <= firstChunkSequenceNum) {
            return new ArrayList<>();//return empty in this case

        }

        long cachableChunks = Math.min(
                cachableChunksDuringHandoff,
                consumableChunksDuringSCConnection
        );

        long lastValidSeq = Math.min(
                chunksInSequence.lastKey(),
                firstChunkSequenceNum + cachableChunks
        );
        for (long seqNum = firstChunkSequenceNum;
                seqNum <= lastValidSeq;
                seqNum++) {
            Chunk chunk = chunksInSequence.get(seqNum);
            
            chunks.add(chunk);
        }

//        //yyy
//        getSim().getStatsHandle().updtSCCmpt6(consumedFromMCDuringHandoff,
//                new UnonymousCompute6(
//                        new UnonymousCompute6.WellKnownTitle("consumedMCHandoff"))
//        );
//        //yyy
//        getSim().getStatsHandle().updtSCCmpt6(consumedFromMCDuringHandoffConf,
//                new UnonymousCompute6(
//                        new UnonymousCompute6.WellKnownTitle("consumedMCHandoffConf"))
//        );
//
//        //yyy
//        getSim().getStatsHandle().updtSCCmpt6(cachableChunks,
//                new UnonymousCompute6(
//                        new UnonymousCompute6.WellKnownTitle("cachableChunks"))
//        );
//
//        //yyy
//        getSim().getStatsHandle().updtSCCmpt6(firstChunkSequenceNum,
//                new UnonymousCompute6(
//                        new UnonymousCompute6.WellKnownTitle("firstChunkSequenceNum"))
//        );
        return chunks;
    }

    public List<Chunk> predictChunks2Request(boolean deb,
            AbstractCachingModel model, double handoverProb,
            double expectedHandoffDuration,
            double conf95HandoffDur, double expectedResidenceDuration,
            double conf95ResidenceDur, int mcRateSliceBytes,
            int bhRateSliceBytes, int scRateSliceBytes) {

        //DebugUtils.printer.print("\n\n****\n\n");
        List<Chunk> chunks = new ArrayList<>();// keep insertion order in returned 

//////////////////////        
//        if (predictChunksZeroProb(chunks, handoverProb)) {
//            return chunks;//skip fast
//        }
//////////////////////   
        List<Chunk> unconsumed = _unconsumedChunksInSequence.get(model);

        if (unconsumed.isEmpty()) {//early skip if possible
            return new ArrayList<>();//return empty in this case
        }

        double chunkSizeInBytes = getSimulation().chunkSizeInBytes();

        // Expected values based on parameter values.
        // Needed to compute which chunks wil be requested.
        long consumedFromMCDuringHandoff/*
                 * if not consume-ready, then it will start consuming only after
                 * entering the first cell
                 */
                = _consumeReady
                        ? Math.round(expectedHandoffDuration * mcRateSliceBytes / chunkSizeInBytes)
                        : 0;

        long consumedFromMCDuringHandoffConf
                = Math.round(conf95HandoffDur * mcRateSliceBytes / chunkSizeInBytes);

        long cachableChunksDuringHandoff
                = Math.round((expectedHandoffDuration + 2 * conf95HandoffDur) * bhRateSliceBytes / chunkSizeInBytes);

        long consumableChunksDuringSCConnection
                = Math.round((expectedResidenceDuration + 2 * conf95ResidenceDur) * scRateSliceBytes / chunkSizeInBytes);

        long firstChunkSequenceNum
                = unconsumed.get(0).getSequenceNum()// first unconsumed chunk in sequence
                + consumedFromMCDuringHandoff
                - consumedFromMCDuringHandoffConf;// conf interval
        firstChunkSequenceNum = Math.max(1, firstChunkSequenceNum);// to prohibit meaningless negative sequences

        SortedMap<Long, Chunk> chunksInSequence = referredContentDocument().getChunksInSequence();

//////////////////////                
//        if (predictChunksContentTooSmall(chunksInSequence.lastKey(), 
//                firstChunkSequenceNum, chunksInSequence, handoverProb, chunks)) {
//            return chunks;
//        }
        if (chunksInSequence.lastKey() <= firstChunkSequenceNum) {
            return new ArrayList<>();//return empty in this case
        }


        long cachableChunks = Math.min(
                cachableChunksDuringHandoff,
                consumableChunksDuringSCConnection
        );


        long lastValidSeq = Math.min(
                chunksInSequence.lastKey(),
                firstChunkSequenceNum + cachableChunks
        );
        for (long seqNum = firstChunkSequenceNum;
                seqNum <= lastValidSeq;
                seqNum++) {
            Chunk chunk = chunksInSequence.get(seqNum);
            chunks.add(chunk);
        }


//        //yyy
//        getSim().getStatsHandle().updtSCCmpt6(consumedFromMCDuringHandoff,
//                new UnonymousCompute6(
//                        new UnonymousCompute6.WellKnownTitle("consumedMCHandoff"))
//        );
//        //yyy
//        getSim().getStatsHandle().updtSCCmpt6(consumedFromMCDuringHandoffConf,
//                new UnonymousCompute6(
//                        new UnonymousCompute6.WellKnownTitle("consumedMCHandoffConf"))
//        );
//
//        //yyy
//        getSim().getStatsHandle().updtSCCmpt6(cachableChunks,
//                new UnonymousCompute6(
//                        new UnonymousCompute6.WellKnownTitle("cachableChunks"))
//        );
//
//        //yyy
//        getSim().getStatsHandle().updtSCCmpt6(firstChunkSequenceNum,
//                new UnonymousCompute6(
//                        new UnonymousCompute6.WellKnownTitle("firstChunkSequenceNum"))
//        );
        return chunks;
    }

    /**
     * *
     * If too small, cache it all
     *
     * lastValidSeq firstChunkSequenceNum chunksInSequence handoverProb chunks
     *
     * @return
     */
    protected boolean predictChunksContentTooSmall(long lastValidSeq,
            long firstChunkSequenceNum, SortedMap<Long, Chunk> chunksInSequence,
            double handoverProb, List<Chunk> chunks) {

//        //DebugUtils.printer.print("\npredictChunksContentTooSmall");
//        //DebugUtils.printer.print("\tlastSeq = " + lastSeq);
//        //DebugUtils.printer.print("\tfirstChunkSequenceNum = " + firstChunkSequenceNum);
        if (lastValidSeq <= firstChunkSequenceNum) {
            // the content is too small and will be consumed entirelly during the handover

            for (long seqNum = 1;
                    seqNum <= lastValidSeq;
                    seqNum++) {
                Chunk chunk = chunksInSequence.get(seqNum);

                chunks.add(chunk);
            }
            return true;
        }
        return false;
    }

    protected boolean predictChunksZeroProb(Map<Chunk, Double> chunks, double handoverProb) {
        if (handoverProb > 0) {
            return false;
        }
        // considering a non-neighbor, which in some scenario setups may be allowed to happen.
        SortedMap<Long, Chunk> chunksInSequence = referredContentDocument().getChunksInSequence();
        long lastSeq = chunksInSequence.lastKey();
        for (long seqNum = 1;
                seqNum <= lastSeq;
                seqNum++) {

            Chunk chunk = chunksInSequence.get(seqNum);
            chunks.put(chunk, 0.0);
        }

        return true;
    }

    /**
     * @return the _requesterUser
     */
    @Override
    public CachingUser getRequesterUser() {
        return _requesterUser;
    }

    @Override
    public void consumeChunksRemainderFromMC(AbstractCachingModel model, double mcRateSlice,
            Map<AbstractCachingModel, List<Chunk>> fillInWithDownloadedFromMC) {
        Map<AbstractCachingModel, List<Chunk>> consumeRemainderFromMC = consumeRemainderFromMC(model);
        mergeAndSeparatePerpolicy(fillInWithDownloadedFromMC, consumeRemainderFromMC);
    }

    @Override
    public void consumeTryAllAtOnceFromSC(
            Map<AbstractCachingModel, List<Chunk>> fillInWithCacheHits,
            Map<AbstractCachingModel, List<Chunk>> fillInWithDownloadedFromBH,
            Map<AbstractCachingModel, List<Chunk>> fillInWithMissedPerModel) {

        if (!_consumeReady) {
            return;
        }

        boolean userConnected = getRequesterUser().isConnected();

        if (!userConnected) {
            return;
//                maxBudget = Math.round(mcRateSlice / chunkSizeInBytes);
//                for (AbstractCachingModel model : getSim().getCachingPolicies()) {
//                    Map<AbstractCachingModel, List<Chunk>> consumedMCwSCDiscon
//                            = consumeFromMCwSCDiscon(model, maxBudget);
//                    mergeToFirstMap(fillInWithDownloadedFromMC, consumedMCwSCDiscon);
//                }

        } else {// in this case, downloads from all reasources, with this *priority*: 

            Map<AbstractCachingModel, Set<Chunk>> hitsNowInCachePerModel
                    = new HashMap<>(5);

            for (AbstractCachingModel model : getSimulation().getCachingStrategies()) {

                long maxBudget = (long) Math.ceil(sizeInChunks());

                /**
                 * ***************************
                 * First, consumeTry from the cache
                 *
                 */
                Map<AbstractCachingModel, Set<Chunk>> hits
                        = tryHitsFromCachePerModel(model, maxBudget);
                hitsNowInCachePerModel.putAll(hits);
                merge2(fillInWithCacheHits, hitsNowInCachePerModel);

                /**
                 * ***************************
                 * Second, from backhaul whatever not hit
                 *
                 */
                Map<AbstractCachingModel, Set<Chunk>> consumedNowFromBH
                        = consumeCacheMissedFromBH(model, maxBudget, hitsNowInCachePerModel);
                merge2(fillInWithDownloadedFromBH, consumedNowFromBH);

            }
        }

    }

    /**
     *
     * mcRateSlice the assumed slice per request of the macro-cellular rate
     * fillInWithDownloadedFromMC scRateSlice the assumed slice per request of
     * the small-cellular rate fillInWithCacheHits minSCorBHRateSlice the
     * assumed slice per request of the rate through the backhaul and from the
     * small cell fillInWithDownloadedFromBH fillInWithMissedPerPolicy
     *
     * @param mcRateSlice
     * @param fillInWithDownloadedFromMC
     * @param scRateSlice
     * @param fillInWithCacheHits
     * @param minSCorBHRateSlice
     * @param fillInWithDownloadedFromBH
     * @param fillInWithMissedPerPolicy
     */
    @Override
    public void consumeTry(
            double mcRateSlice, Map<AbstractCachingModel, List<Chunk>> fillInWithDownloadedFromMC,
            double scRateSlice, Map<AbstractCachingModel, List<Chunk>> fillInWithCacheHits,
            double minSCorBHRateSlice, Map<AbstractCachingModel, List<Chunk>> fillInWithDownloadedFromBH,
            Map<AbstractCachingModel, List<Chunk>> fillInWithMissedPerPolicy) {

        Boolean isSoft = Boolean.parseBoolean(getSimulation().getScenario().stringProperty(Space.MU__ISSOFT, false));
        // if soft, then the macro IS never used

        if (!_consumeReady) {
            return;
        }

        long chunkSizeInBytes = getSimulation().chunkSizeInBytes();

        boolean userConnected = getRequesterUser().isConnected();

        long maxBudget;

        if (!userConnected) {
            if (!isSoft) {
                maxBudget = Math.round(mcRateSlice / chunkSizeInBytes);
                for (AbstractCachingModel model : getSimulation().getCachingStrategies()) {
                    Map<AbstractCachingModel, List<Chunk>> consumedMCwSCDiscon
                            = consumeFromMCwSCDiscon(model, maxBudget);
                    mergeToFirstMap(fillInWithDownloadedFromMC, consumedMCwSCDiscon);
                }
            }
        } else {// in this case, downloads from all reasources, with this *priority*: 

            Map<AbstractCachingModel, Set<Chunk>> hitsNowInCachePerPolicy
                    = new HashMap<>(5);

            maxBudget = Math.round(scRateSlice / chunkSizeInBytes);
            for (AbstractCachingModel model : getSimulation().getCachingStrategies()) {

//
//
//// CAUTION! do not change  the priority of the following invokations!                
//
//
                /**
                 * ***************************
                 * First, consumeTry from the cache
                 *
                 */
                Map<AbstractCachingModel, Set<Chunk>> hits
                        = tryHitsFromCachePerModel(model, maxBudget);
                hitsNowInCachePerPolicy.putAll(hits);
                merge2(fillInWithCacheHits, hitsNowInCachePerPolicy);

                /**
                 * ***************************
                 * Second, from backhaul
                 *
                 */
                maxBudget = Math.round(minSCorBHRateSlice / chunkSizeInBytes);
                Map<AbstractCachingModel, Set<Chunk>> consumedNowFromBH
                        = consumeCacheMissedFromBH(model, maxBudget, hitsNowInCachePerPolicy);
                merge2(fillInWithDownloadedFromBH, consumedNowFromBH);

                /**
                 * ******************************
                 * Third and last, consumeTry from the macro
                 *
                 */
                if (!(_requesterUser instanceof StationaryUser) && !isSoft) {
                    // stationaries do not consume from the macrocell
                    maxBudget = Math.round(mcRateSlice / chunkSizeInBytes);
                    mergeToFirstMap(fillInWithDownloadedFromMC, consumeFromMCwSCCon(model, maxBudget));
                }
            }
        }

    }

    public boolean cacheForOracle(SmallCell targetSC, Chunk theChunk) {
        return Oracle.instance().cacheDecision(
                getRequesterUser(),
                targetSC,
                theChunk) > 0;
    }

    /**
     *
     * maxBudget the max number of chunks that can be downloaded from the cache
     * fillWithMissedByModel for some polices, some chunks may be missed. These
     * chunks must be considered for download by the BH or the MC
     *
     * @return the consumed chunks from the cache
     */
    private Map<AbstractCachingModel, Set<Chunk>> tryHitsFromCachePerModel(
            AbstractCachingModel model, long maxBudget) {
        Map<AbstractCachingModel, Set<Chunk>> currentHitsInCachePerModel = new HashMap<>(5);

        List<Chunk> unconsumed = _unconsumedChunksInSequence.get(model);
        if (unconsumed.isEmpty()) {
            if (_completitionTimes.get(model) == -1) {
                _completitionTimes.put(model, simTime());
                _uncompletedPolicies--;
            }
            return currentHitsInCachePerModel;
        }

        long budgetForModel = maxBudget;

        Set<Chunk> currentChunkHits;
        currentHitsInCachePerModel.put(model, currentChunkHits = new HashSet<>());
        List<Chunk> historyChunkHits = _chunksHitsHistoryFromSC.get(model);

        Iterator<Chunk> unconsumedIt = unconsumed.iterator(); // in ascending order of keys

        SmallCell hostSC = _requesterUser.getCurrentlyConnectedSC();
        Set<Chunk> cachedChunks = hostSC.cachedChunksUnmodifiable(model);
        while (unconsumedIt.hasNext() && budgetForModel > 0) {
            Chunk chunkConsumed = unconsumedIt.next();

            if (model instanceof Oracle) {
                // if so, try to cache before consuming
                if (cacheForOracle(
                        hostSC,// in this case the target sc is the hosting
                        chunkConsumed)) {
                    unconsumedIt.remove();
                    /* While being connected, only in this case the chunk is not 
                     * already consumed either from the BH nor from MC */
                    budgetForModel--;
                    historyChunkHits.add(chunkConsumed);
                    currentChunkHits.add(chunkConsumed);
                }
                continue;
            }

            if (cachedChunks.contains(chunkConsumed)) {
                unconsumedIt.remove();
                /* While being connected, only in this case the chunk is not 
                     * already consumed either from the BH nor from MC */
                budgetForModel--;
                historyChunkHits.add(chunkConsumed);
                currentChunkHits.add(chunkConsumed);
            }
        }

        if (unconsumed.isEmpty()) {
            if (_completitionTimes.get(model) == -1) {
                _completitionTimes.put(model, simTime());
                _uncompletedPolicies--;
            }
        }

        return currentHitsInCachePerModel;
    }

    private Map<AbstractCachingModel, Set<Chunk>> consumeCacheMissedFromBH(
            AbstractCachingModel model,
            long maxbudget,
            Map<AbstractCachingModel, Set<Chunk>> hitsNumMap) {

        Map<AbstractCachingModel, Set<Chunk>> bhCurrentConsumptionPerModel = new HashMap<>(5);

        Set<Chunk> bhCurrentConsumption;
        bhCurrentConsumptionPerModel.put(model, bhCurrentConsumption = new HashSet<>());
        List<Chunk> bhHistoryConsumption = _chunksConsumedHistoryFromBH.get(model);

        List<Chunk> unconsumed = _unconsumedChunksInSequence.get(model);
        if (unconsumed.isEmpty()) {
            if (_completitionTimes.get(model) == -1) {
                _completitionTimes.put(model, simTime());
                _uncompletedPolicies--;
            }
            return bhCurrentConsumptionPerModel;
        }

        // each model has its own budget, i.e. if you have 10 slots in the
        // sc wireless and you have 4 hits in the cache, then you have
        // consumed 4 slots in the wireless. Thus, now you can use only 
        // six slots for this model.
        int hitsNum = hitsNumMap.get(model).size();
        long budgetForModel = maxbudget - hitsNum;

        Iterator<Chunk> unconsumedIt = unconsumed.iterator(); // in ascending order of keys

        Set<Chunk> cachedChunks = _requesterUser.getCurrentlyConnectedSC().cachedChunksUnmodifiable(model);
        while (unconsumedIt.hasNext() && budgetForModel > 0) {
            Chunk chunkConsumed = unconsumedIt.next();
            if (cachedChunks.contains(chunkConsumed)) {
                // if already in the cache, let be consumed in the future from there
                continue;
            }

            unconsumedIt.remove();
            budgetForModel--;
            bhHistoryConsumption.add(chunkConsumed);
            bhCurrentConsumption.add(chunkConsumed);
        }

        if (unconsumed.isEmpty()) {
            if (_completitionTimes.get(model) == -1) {
                _completitionTimes.put(model, simTime());
                _uncompletedPolicies--;
            }
        }

        return bhCurrentConsumptionPerModel;
    }

    protected Map<AbstractCachingModel, List<Chunk>> consumeFromMCwSCCon(
            AbstractCachingModel model, long policyBudget) {

        Map<AbstractCachingModel, List<Chunk>> mcCurrentConsumptionPerPolicy = new HashMap<>(5);

        long budgetForPolicy = policyBudget;

        List<Chunk> mcCurrentConsumption;
        mcCurrentConsumptionPerPolicy.put(model, mcCurrentConsumption = new ArrayList<>());
        List<Chunk> mcHistoryConsumption = _chunksConsumedHistoryFromMCWhileConnectedToSC.get(model);

        List<Chunk> unconsumed = _unconsumedChunksInSequence.get(model);
        if (unconsumed.isEmpty()) {
            if (_completitionTimes.get(model) == -1) {
                _completitionTimes.put(model, simTime());
                _uncompletedPolicies--;
            }
            return mcCurrentConsumptionPerPolicy;

        }
        Iterator<Chunk> unconsumedIt = unconsumed.iterator(); // in ascending order of keys

        Set<Chunk> cachedChunks = _requesterUser.getCurrentlyConnectedSC().cachedChunksUnmodifiable(model);
        while (unconsumedIt.hasNext() && budgetForPolicy > 0) {
            Chunk chunkConsumed = unconsumedIt.next();
            if (cachedChunks.contains(chunkConsumed)) {
                // if already in the cache, let be consumed in the future from there
                continue;
            }

            unconsumedIt.remove();
            budgetForPolicy--;
            mcHistoryConsumption.add(chunkConsumed);
            mcCurrentConsumption.add(chunkConsumed);
        }

        if (unconsumed.isEmpty()) {
            if (_completitionTimes.get(model) == -1) {
                _completitionTimes.put(model, simTime());
                _uncompletedPolicies--;
            }
        }

        return mcCurrentConsumptionPerPolicy;
    }

    protected Map<AbstractCachingModel, List<Chunk>> consumeFromMCwSCDiscon(
            AbstractCachingModel model, long budget) {

        Map<AbstractCachingModel, List<Chunk>> toReturn = new HashMap<>(5);

        long policyBudget = budget;

        List<Chunk> nowDownloadedChunks;
        toReturn.put(model, nowDownloadedChunks = new ArrayList<>());
        List<Chunk> consumedHistoryByPolicy = _consumedHistoryFromMCwSCDiscon.get(model);

        List<Chunk> unconsumed = _unconsumedChunksInSequence.get(model);
        if (unconsumed.isEmpty()) {
            if (_completitionTimes.get(model) == -1) {
                _completitionTimes.put(model, simTime());
                _uncompletedPolicies--;
            }
            return toReturn;
        }
        Iterator<Chunk> unconsumedIt = unconsumed.iterator(); // in ascending order of keys

        while (unconsumedIt.hasNext() && policyBudget-- > 0) {
            Chunk chunkConsumed = unconsumedIt.next();
            unconsumedIt.remove();

            consumedHistoryByPolicy.add(chunkConsumed);
            nowDownloadedChunks.add(chunkConsumed);
        }

        if (unconsumed.isEmpty()) {
            if (_completitionTimes.get(model) == -1) {
                _completitionTimes.put(model, simTime());
                _uncompletedPolicies--;
            }
        }

        return toReturn;
    }

    protected Map<AbstractCachingModel, List<Chunk>>
            consumeRemainderFromMC(AbstractCachingModel model) {

        Map<AbstractCachingModel, List<Chunk>> toReturn = new HashMap<>(5);

        long policyBudget = Long.MAX_VALUE;

        List<Chunk> nowDownloadedChunks = new ArrayList<>();
        List<Chunk> consumedHistoryByPolicy = _chunksConsumedHistoryFromMCAfterExitingSC.get(model);

        List<Chunk> unconsumed = _unconsumedChunksInSequence.get(model);
        if (unconsumed.isEmpty()) {
            if (_completitionTimes.get(model) == -1) {
                _completitionTimes.put(model, simTime());
                _uncompletedPolicies--;
            }
            return toReturn;
        }
        Iterator<Chunk> unconsumedIt = unconsumed.iterator(); // in ascending order of keys

        while (unconsumedIt.hasNext() && policyBudget-- > 0) {

            Chunk chunkConsumed = unconsumedIt.next();
            unconsumedIt.remove();

            consumedHistoryByPolicy.add(chunkConsumed);
            nowDownloadedChunks.add(chunkConsumed);
        }

        toReturn.put(model, nowDownloadedChunks);

        if (unconsumed.isEmpty()) {
            if (_completitionTimes.get(model) == -1) {
                _completitionTimes.put(model, simTime());
                _uncompletedPolicies--;
            }
        }

        return toReturn;
    }

    /**
     * @return the _chunksConsumedHistoryFromMCWhileConnectedToSC
     */
    @Override
    public Map<AbstractCachingModel, List<Chunk>> getChunksConsumedHistoryFromMCWhileConnectedToSC() {
        return Collections.unmodifiableMap(_chunksConsumedHistoryFromMCWhileConnectedToSC);
    }

    public Map<AbstractCachingModel, List<Chunk>> getConsumedHistoryFromMCwSCDiscon() {
        return Collections.unmodifiableMap(_consumedHistoryFromMCwSCDiscon);
    }

    public Map<AbstractCachingModel, List<Chunk>> getChunksConsumedHistoryFromMCAfterExitingSC() {
        return Collections.unmodifiableMap(_chunksConsumedHistoryFromMCAfterExitingSC);
    }

    @Override
    public List<Chunk> getChunksConsumedHistoryFromMCwSCConn(AbstractCachingModel model) {
        return Collections.unmodifiableList(_chunksConsumedHistoryFromMCWhileConnectedToSC.get(model));
    }

    @Override
    public List<Chunk> getChunksConsumedHistoryFromMCBeforeEnteringSC(AbstractCachingModel model) {
        return Collections.unmodifiableList(_consumedHistoryFromMCwSCDiscon.get(model));
    }

    @Override
    public List<Chunk> getChunksConsumedHistoryFromMCAfterExitingSC(AbstractCachingModel model) {
        return Collections.unmodifiableList(_chunksConsumedHistoryFromMCAfterExitingSC.get(model));
    }

    /**
     * model
     *
     * @return the _consumedChunksFromSC
     */
    @Override
    public List<Chunk> getChunksCacheHitsHistory(AbstractCachingModel model) {
        return Collections.unmodifiableList(_chunksHitsHistoryFromSC.get(model));
    }

    /**
     * @return the _chunksConsumedHistoryFromBH
     */
    @Override
    public Map<AbstractCachingModel, List<Chunk>> getChunksConsumedHistoryFromBH() {
        return Collections.unmodifiableMap(_chunksConsumedHistoryFromBH);
    }

    @Override
    public List<Chunk> getChunksConsumedHistoryFromBH(AbstractCachingModel model) {
        return Collections.unmodifiableList(_chunksConsumedHistoryFromBH.get(model));
    }

    /**
     * Merges the value entries in the second to the value entries in the first
     *
     * firstMap secondMap
     */
    protected final void mergeToFirstMap(
            Map<AbstractCachingModel, List<Chunk>> firstMap,
            Map<AbstractCachingModel, List<Chunk>> secondMap) {
        for (Map.Entry<AbstractCachingModel, List<Chunk>> entry : secondMap.entrySet()) {
            AbstractCachingModel model = entry.getKey();
            Collection<Chunk> chunksInSecond = entry.getValue();

            List<Chunk> chunksInFirst = firstMap.get(model);
            if (chunksInFirst == null) {
                firstMap.put(model, (chunksInFirst = new ArrayList<>()));
            }
            chunksInFirst.addAll(chunksInSecond);
        }
    }

    protected final void mergeAndSeparatePerpolicy(
            Map<AbstractCachingModel, List<Chunk>> firstMap,
            Map<AbstractCachingModel, List<Chunk>> secondMap) {
        for (Map.Entry<AbstractCachingModel, List<Chunk>> entry : secondMap.entrySet()) {
            AbstractCachingModel model = entry.getKey();
            Collection<Chunk> chunksInSecond = entry.getValue();

            List<Chunk> chunksInFirst = firstMap.get(model);
            if (chunksInFirst == null) {
                firstMap.put(model, (chunksInFirst = new ArrayList<>()));
            }
            chunksInFirst.addAll(chunksInSecond);

        }
    }

    protected final void merge2(Map<AbstractCachingModel, List<Chunk>> firstMap,
            Map<AbstractCachingModel, Set<Chunk>> secondMap) {
        for (Map.Entry<AbstractCachingModel, Set<Chunk>> entry : secondMap.entrySet()) {
            AbstractCachingModel model = entry.getKey();
            Collection<Chunk> chunksInSecond = entry.getValue();

            List<Chunk> chunksInFirst = firstMap.get(model);
            if (chunksInFirst == null) {
                firstMap.put(model, (chunksInFirst = new ArrayList<>()));
            }
            chunksInFirst.addAll(chunksInSecond);
        }
    }

    public boolean isFullyConsumed() {
        return _uncompletedPolicies == 0;
    }

    public int getCompetionSimTime(AbstractCachingModel model) {
        return _completitionTimes.get(model);
    }

    public void forceComplete(int time) {
        _uncompletedPolicies = 0;
        for (AbstractCachingModel model : getSimulation().getCachingStrategies()) {
            _completitionTimes.put(model, time);
        }
    }

    /**
     * model
     *
     * @return
     */
    public List<Chunk> getUnconsumedChunksInSequence(AbstractCachingModel model) {
        return _unconsumedChunksInSequence.get(model);
    }

    /**
     * @return the _consumeReady
     */
    public boolean isConsumeReady() {
        return _consumeReady;
    }

    /**
     * cnsmr the _consumeReady to set
     */
    public void setConsumeReady(boolean cnsmr) {
        this._consumeReady = cnsmr;
    }

    public List<Chunk> getChunksConsumedOverall(AbstractCachingModel model) {
        List<Chunk> tmp = new ArrayList(referredContentDocument().chunks());
        tmp.removeAll(getUnconsumedChunksInSequence(model));

        return tmp;
    }

}
