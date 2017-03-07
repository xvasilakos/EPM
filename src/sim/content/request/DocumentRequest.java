package sim.content.request;

import app.properties.Space;
import caching.base.AbstractCachingPolicy;
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
import utils.DebugTool;

/**
 * @author xvas
 */
public class DocumentRequest extends TraceWorkloadRecord implements ISynopsisString, IRequest {

    /**
     * The simulation time of request. This is not the same as the time loaded
     * from the trace workload.
     */
    protected final int _issuedAtSimTime; // either a stationary small cell user or a mobile user
    protected int _uncompletedPolicies;
    protected final CachingUser _requesterUser; // either a stationary small cell user or a mobile user
    protected final Map<AbstractCachingPolicy, List<Chunk>> _unconsumedChunksInSequence;
    protected final Map<AbstractCachingPolicy, Integer> _completitionTimes;
    protected final Map<AbstractCachingPolicy, List<Chunk>> _chunksConsumedHistoryFromMCWhileConnectedToSC;
    private final Map<AbstractCachingPolicy, List<Chunk>> _consumedHistoryFromMCwSCDiscon;
    private final Map<AbstractCachingPolicy, List<Chunk>> _chunksConsumedHistoryFromMCAfterExitingSC;
    protected final Map<AbstractCachingPolicy, List<Chunk>> _chunksConsumedHistoryFromBH;
    protected final Map<AbstractCachingPolicy, List<Chunk>> _chunksHitsHistoryFromSC;

    private boolean _consumeReady;

//    List<RequestedChunk> _consumedChunksInSequence = null;
    public DocumentRequest(TraceWorkloadRecord workloadRecord, CachingUser requesterUser) {

        super(workloadRecord.getSim(), workloadRecord.sizeInBytes(), workloadRecord.getID(), workloadRecord.getTime());
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

        _uncompletedPolicies = getSim().getCachingPolicies().size();
        for (AbstractCachingPolicy policy : getSim().getCachingPolicies()) {
            _unconsumedChunksInSequence.put(policy, new ArrayList<>(chunksInSequence));
            _completitionTimes.put(policy, -1);
            _chunksConsumedHistoryFromMCWhileConnectedToSC.put(policy, new ArrayList<Chunk>());
            _consumedHistoryFromMCwSCDiscon.put(policy, new ArrayList<Chunk>());
            _chunksConsumedHistoryFromMCAfterExitingSC.put(policy, new ArrayList<Chunk>());
            _chunksHitsHistoryFromSC.put(policy, new ArrayList<Chunk>());
            _chunksConsumedHistoryFromBH.put(policy, new ArrayList<Chunk>());
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
            AbstractCachingPolicy policy, double handoverProb,
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
        List<Chunk> unconsumed = _unconsumedChunksInSequence.get(policy);

        if (unconsumed.isEmpty()) {//early skip if possible
            return new ArrayList<>();//return empty in this case
        }

        double chunkSizeInBytes = getSim().chunkSizeInBytes();

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
            if (chunk == null) {
                throw new RuntimeException();//xxx
            }
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
            AbstractCachingPolicy policy, double handoverProb,
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
        List<Chunk> unconsumed = _unconsumedChunksInSequence.get(policy);

        DebugTool.append(deb, unconsumed.size() + ",");

        if (unconsumed.isEmpty()) {//early skip if possible
            DebugTool.append(deb, "-0\n");

            return new ArrayList<>();//return empty in this case
        }

        double chunkSizeInBytes = getSim().chunkSizeInBytes();

        // Expected values based on parameter values.
        // Needed to compute which chunks wil be requested.
        long consumedFromMCDuringHandoff/*
                 * if not consume-ready, then it will start consuming only after
                 * entering the first cell
                 */
                = _consumeReady
                        ? Math.round(expectedHandoffDuration * mcRateSliceBytes / chunkSizeInBytes)
                        : 0;

        DebugTool.append(deb, expectedHandoffDuration + ",");
        DebugTool.append(deb, mcRateSliceBytes + ",");
        DebugTool.append(deb, consumedFromMCDuringHandoff + ",");

        long consumedFromMCDuringHandoffConf
                = Math.round(conf95HandoffDur * mcRateSliceBytes / chunkSizeInBytes);

        long cachableChunksDuringHandoff
                = Math.round((expectedHandoffDuration + 2 * conf95HandoffDur) * bhRateSliceBytes / chunkSizeInBytes);

        DebugTool.append(deb, cachableChunksDuringHandoff + ",");

        long consumableChunksDuringSCConnection
                = Math.round((expectedResidenceDuration + 2 * conf95ResidenceDur) * scRateSliceBytes / chunkSizeInBytes);

        DebugTool.append(deb, (expectedResidenceDuration + 2 * conf95ResidenceDur) + ",");
        DebugTool.append(deb, scRateSliceBytes + ",");
        DebugTool.append(deb, consumableChunksDuringSCConnection + ",");

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
            DebugTool.append(deb, "-1\n");
            return new ArrayList<>();//return empty in this case

        }

        DebugTool.append(deb, firstChunkSequenceNum + ",");

        long cachableChunks = Math.min(
                cachableChunksDuringHandoff,
                consumableChunksDuringSCConnection
        );

        DebugTool.append(deb, cachableChunks + ",");

        long lastValidSeq = Math.min(
                chunksInSequence.lastKey(),
                firstChunkSequenceNum + cachableChunks
        );
        for (long seqNum = firstChunkSequenceNum;
                seqNum <= lastValidSeq;
                seqNum++) {
            Chunk chunk = chunksInSequence.get(seqNum);
            if (chunk == null) {
                throw new RuntimeException();//xxx
            }
            chunks.add(chunk);
        }

        DebugTool.append(deb, lastValidSeq + "\n");

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
    public void consumeChunksRemainderFromMC(AbstractCachingPolicy policy, double mcRateSlice,
            Map<AbstractCachingPolicy, List<Chunk>> fillInWithDownloadedFromMC) {
        Map<AbstractCachingPolicy, List<Chunk>> consumeRemainderFromMC = consumeRemainderFromMC(policy);
        mergeAndSeparatePerpolicy(fillInWithDownloadedFromMC, consumeRemainderFromMC);
    }

    @Override
    public void consumeTryAllAtOnceFromSC(
            Map<AbstractCachingPolicy, List<Chunk>> fillInWithCacheHits,
            Map<AbstractCachingPolicy, List<Chunk>> fillInWithDownloadedFromBH,
            Map<AbstractCachingPolicy, List<Chunk>> fillInWithMissedPerPolicy) {

        if (!_consumeReady) {
            return;
        }

        boolean userConnected = getRequesterUser().isConnected();

        if (!userConnected) {
            return;
//                maxBudget = Math.round(mcRateSlice / chunkSizeInBytes);
//                for (AbstractCachingPolicy policy : getSim().getCachingPolicies()) {
//                    Map<AbstractCachingPolicy, List<Chunk>> consumedMCwSCDiscon
//                            = consumeFromMCwSCDiscon(policy, maxBudget);
//                    mergeToFirstMap(fillInWithDownloadedFromMC, consumedMCwSCDiscon);
//                }

        } else {// in this case, downloads from all reasources, with this *priority*: 

            Map<AbstractCachingPolicy, Set<Chunk>> hitsNowInCachePerPolicy
                    = new HashMap<>(5);

            for (AbstractCachingPolicy policy : getSim().getCachingPolicies()) {

                long maxBudget = (long) Math.ceil(sizeInChunks());

                /**
                 * ***************************
                 * First, consumeTry from the cache
                 *
                 */
                Map<AbstractCachingPolicy, Set<Chunk>> hits
                        = tryHitsFromCachePerPolicy(policy, maxBudget);
                hitsNowInCachePerPolicy.putAll(hits);
                merge2(fillInWithCacheHits, hitsNowInCachePerPolicy);

                /**
                 * ***************************
                 * Second, from backhaul whatever not hit
                 *
                 */
                Map<AbstractCachingPolicy, Set<Chunk>> consumedNowFromBH
                        = consumeCacheMissedFromBH(policy, maxBudget, hitsNowInCachePerPolicy);
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
     */
    @Override
    public void consumeTry(
            double mcRateSlice, Map<AbstractCachingPolicy, List<Chunk>> fillInWithDownloadedFromMC,
            double scRateSlice, Map<AbstractCachingPolicy, List<Chunk>> fillInWithCacheHits,
            double minSCorBHRateSlice, Map<AbstractCachingPolicy, List<Chunk>> fillInWithDownloadedFromBH,
            Map<AbstractCachingPolicy, List<Chunk>> fillInWithMissedPerPolicy) {

        Boolean isSoft = Boolean.parseBoolean(getSim().getScenario().stringProperty(Space.MU__ISSOFT, false));
        // if soft, then the macro IS never used

        if (!_consumeReady) {
            return;
        }

        long chunkSizeInBytes = getSim().chunkSizeInBytes();

        boolean userConnected = getRequesterUser().isConnected();

        long maxBudget;

        if (!userConnected) {
            if (!isSoft) {
                maxBudget = Math.round(mcRateSlice / chunkSizeInBytes);
                for (AbstractCachingPolicy policy : getSim().getCachingPolicies()) {
                    Map<AbstractCachingPolicy, List<Chunk>> consumedMCwSCDiscon
                            = consumeFromMCwSCDiscon(policy, maxBudget);
                    mergeToFirstMap(fillInWithDownloadedFromMC, consumedMCwSCDiscon);
                }
            }
        } else {// in this case, downloads from all reasources, with this *priority*: 

            Map<AbstractCachingPolicy, Set<Chunk>> hitsNowInCachePerPolicy
                    = new HashMap<>(5);

            maxBudget = Math.round(scRateSlice / chunkSizeInBytes);
            for (AbstractCachingPolicy policy : getSim().getCachingPolicies()) {

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
                Map<AbstractCachingPolicy, Set<Chunk>> hits
                        = tryHitsFromCachePerPolicy(policy, maxBudget);
                hitsNowInCachePerPolicy.putAll(hits);
                merge2(fillInWithCacheHits, hitsNowInCachePerPolicy);

                /**
                 * ***************************
                 * Second, from backhaul
                 *
                 */
                maxBudget = Math.round(minSCorBHRateSlice / chunkSizeInBytes);
                Map<AbstractCachingPolicy, Set<Chunk>> consumedNowFromBH
                        = consumeCacheMissedFromBH(policy, maxBudget, hitsNowInCachePerPolicy);
                merge2(fillInWithDownloadedFromBH, consumedNowFromBH);

                /**
                 * ******************************
                 * Third and last, consumeTry from the macro
                 *
                 */
                if (!(_requesterUser instanceof StationaryUser) && !isSoft) {
                    // stationaries do not consume from the macrocell
                    maxBudget = Math.round(mcRateSlice / chunkSizeInBytes);
                    mergeToFirstMap(fillInWithDownloadedFromMC, consumeFromMCwSCCon(policy, maxBudget));
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
     * fillWithMissedByPolicy for some polices, some chunks may be missed. These
     * chunks must be considered for download by the BH or the MC
     *
     * @return the consumed chunks from the cache
     */
    private Map<AbstractCachingPolicy, Set<Chunk>> tryHitsFromCachePerPolicy(
            AbstractCachingPolicy policy, long maxBudget) {
        Map<AbstractCachingPolicy, Set<Chunk>> currentHitsInCachePerPolicy = new HashMap<>(5);

        List<Chunk> unconsumed = _unconsumedChunksInSequence.get(policy);
        if (unconsumed.isEmpty()) {
            if (_completitionTimes.get(policy) == -1) {
                _completitionTimes.put(policy, simTime());
                _uncompletedPolicies--;
            }
            return currentHitsInCachePerPolicy;
        }

        long budgetForPolicy = maxBudget;

        Set<Chunk> currentChunkHits;
        currentHitsInCachePerPolicy.put(policy, currentChunkHits = new HashSet<>());
        List<Chunk> historyChunkHits = _chunksHitsHistoryFromSC.get(policy);

        Iterator<Chunk> unconsumedIt = unconsumed.iterator(); // in ascending order of keys

        SmallCell hostSC = _requesterUser.getCurrentlyConnectedSC();
        Set<Chunk> cachedChunks = hostSC.cachedChunksUnmodifiable(policy);
        while (unconsumedIt.hasNext() && budgetForPolicy > 0) {
            Chunk chunkConsumed = unconsumedIt.next();

            if (policy instanceof Oracle) {
                // if so, try to cache before consuming
                if (cacheForOracle(
                        hostSC,// in this case the target sc is the hosting
                        chunkConsumed)) {
                    unconsumedIt.remove();
                    /* While being connected, only in this case the chunk is not 
                     * already consumed either from the BH nor from MC */
                    budgetForPolicy--;
                    historyChunkHits.add(chunkConsumed);
                    currentChunkHits.add(chunkConsumed);
                }
                continue;
            }

            if (cachedChunks.contains(chunkConsumed)) {
                unconsumedIt.remove();
                /* While being connected, only in this case the chunk is not 
                     * already consumed either from the BH nor from MC */
                budgetForPolicy--;
                historyChunkHits.add(chunkConsumed);
                currentChunkHits.add(chunkConsumed);
            }
        }

        if (unconsumed.isEmpty()) {
            if (_completitionTimes.get(policy) == -1) {
                _completitionTimes.put(policy, simTime());
                _uncompletedPolicies--;
            }
        }

//            if (_requesterUser.getClass() == StationaryUser.class
//                    && policy.getClass() == EMPC_LC_Full.class) {
//xxx
//                Utils.trackUser(false,
//                        "\n\t\t budget remained: " + policyBudget
//                        + " out of a maxBudget: " + maxBudget
//                        + "\n\t\t hits now : " + nowChunkHits.size()
//                        + "\n\t\t hits history : " + hitsByPolicyHistory.size(),
//                        getRequesterUser(), true);
//            }
        return currentHitsInCachePerPolicy;
    }

    private Map<AbstractCachingPolicy, Set<Chunk>> consumeCacheMissedFromBH(
            AbstractCachingPolicy policy,
            long maxbudget,
            Map<AbstractCachingPolicy, Set<Chunk>> hitsNumMap) {

        Map<AbstractCachingPolicy, Set<Chunk>> bhCurrentConsumptionPerPolicy = new HashMap<>(5);

        Set<Chunk> bhCurrentConsumption;
        bhCurrentConsumptionPerPolicy.put(policy, bhCurrentConsumption = new HashSet<>());
        List<Chunk> bhHistoryConsumption = _chunksConsumedHistoryFromBH.get(policy);

        List<Chunk> unconsumed = _unconsumedChunksInSequence.get(policy);
        if (unconsumed.isEmpty()) {
            if (_completitionTimes.get(policy) == -1) {
                _completitionTimes.put(policy, simTime());
                _uncompletedPolicies--;
//                    DebugTool.printer.print("\n****Consumed Fully " + getID());
            }
            return bhCurrentConsumptionPerPolicy;
        }

        // each policy has its own budget, i.e. if you have 10 slots in the
        // sc wireless and you have 4 hits in the cache, then you have
        // consumed 4 slots in the wireless. Thus, now you can use only 
        // six slots for this policy.
        int hitsNum = hitsNumMap.get(policy).size();
        long budgetForPolicy = maxbudget - hitsNum;

        Iterator<Chunk> unconsumedIt = unconsumed.iterator(); // in ascending order of keys

        Set<Chunk> cachedChunks = _requesterUser.getCurrentlyConnectedSC().cachedChunksUnmodifiable(policy);
        while (unconsumedIt.hasNext() && budgetForPolicy > 0) {
            Chunk chunkConsumed = unconsumedIt.next();
            if (cachedChunks.contains(chunkConsumed)) {
                // if already in the cache, let be consumed in the future from there
                continue;
            }

            unconsumedIt.remove();
            budgetForPolicy--;
            bhHistoryConsumption.add(chunkConsumed);
            bhCurrentConsumption.add(chunkConsumed);
        }

        if (unconsumed.isEmpty()) {
            if (_completitionTimes.get(policy) == -1) {
                _completitionTimes.put(policy, simTime());
                _uncompletedPolicies--;
            }
        }

        return bhCurrentConsumptionPerPolicy;
    }

    protected Map<AbstractCachingPolicy, List<Chunk>> consumeFromMCwSCCon(
            AbstractCachingPolicy policy, long policyBudget) {

        Map<AbstractCachingPolicy, List<Chunk>> mcCurrentConsumptionPerPolicy = new HashMap<>(5);

        long budgetForPolicy = policyBudget;

        List<Chunk> mcCurrentConsumption;
        mcCurrentConsumptionPerPolicy.put(policy, mcCurrentConsumption = new ArrayList<>());
        List<Chunk> mcHistoryConsumption = _chunksConsumedHistoryFromMCWhileConnectedToSC.get(policy);

//xxx            int xxx = 0;
//            if (_requesterUser.getClass() == StationaryUser.class
//                    && policy.getClass() == EMPC_LC_Full.class) {
//                Utils.trackUser(false,
//                        "\t #MC policyBudget " + policyBudget,
//                        getRequesterUser(), true);
//            }
        List<Chunk> unconsumed = _unconsumedChunksInSequence.get(policy);
        if (unconsumed.isEmpty()) {
            if (_completitionTimes.get(policy) == -1) {
                _completitionTimes.put(policy, simTime());
                _uncompletedPolicies--;
            }
            return mcCurrentConsumptionPerPolicy;

        }
        Iterator<Chunk> unconsumedIt = unconsumed.iterator(); // in ascending order of keys

        Set<Chunk> cachedChunks = _requesterUser.getCurrentlyConnectedSC().cachedChunksUnmodifiable(policy);
        while (unconsumedIt.hasNext() && budgetForPolicy > 0) {
            Chunk chunkConsumed = unconsumedIt.next();
            if (cachedChunks.contains(chunkConsumed)) {
                // if already in the cache, let be consumed in the future from there
                continue;
            }
//                xxx++;

            unconsumedIt.remove();
            budgetForPolicy--;
            mcHistoryConsumption.add(chunkConsumed);
            mcCurrentConsumption.add(chunkConsumed);
        }

        if (unconsumed.isEmpty()) {
            if (_completitionTimes.get(policy) == -1) {
                _completitionTimes.put(policy, simTime());
                _uncompletedPolicies--;
            }
        }

//xxx
//            if (_requesterUser.getClass() == StationaryUser.class
//                    && policy.getClass() == EMPC_LC_Full.class) {
//                Utils.trackUser(false,
//                        "\t #MC from rest " + xxx,
//                        getRequesterUser(), true);
//            }
        return mcCurrentConsumptionPerPolicy;
    }

    protected Map<AbstractCachingPolicy, List<Chunk>> consumeFromMCwSCDiscon(
            AbstractCachingPolicy policy, long budget) {

        Map<AbstractCachingPolicy, List<Chunk>> toReturn = new HashMap<>(5);

        long policyBudget = budget;

        List<Chunk> nowDownloadedChunks;
        toReturn.put(policy, nowDownloadedChunks = new ArrayList<>());
        List<Chunk> consumedHistoryByPolicy = _consumedHistoryFromMCwSCDiscon.get(policy);

        List<Chunk> unconsumed = _unconsumedChunksInSequence.get(policy);
        if (unconsumed.isEmpty()) {
            if (_completitionTimes.get(policy) == -1) {
                _completitionTimes.put(policy, simTime());
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
            if (_completitionTimes.get(policy) == -1) {
                _completitionTimes.put(policy, simTime());
                _uncompletedPolicies--;
            }
        }

        return toReturn;
    }

    protected Map<AbstractCachingPolicy, List<Chunk>>
            consumeRemainderFromMC(AbstractCachingPolicy policy) {

        Map<AbstractCachingPolicy, List<Chunk>> toReturn = new HashMap<>(5);

        long policyBudget = Long.MAX_VALUE;

        List<Chunk> nowDownloadedChunks = new ArrayList<>();
        List<Chunk> consumedHistoryByPolicy = _chunksConsumedHistoryFromMCAfterExitingSC.get(policy);

        List<Chunk> unconsumed = _unconsumedChunksInSequence.get(policy);
        if (unconsumed.isEmpty()) {
            if (_completitionTimes.get(policy) == -1) {
                _completitionTimes.put(policy, simTime());
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

        toReturn.put(policy, nowDownloadedChunks);

        if (unconsumed.isEmpty()) {
            if (_completitionTimes.get(policy) == -1) {
                _completitionTimes.put(policy, simTime());
                _uncompletedPolicies--;
            }
        }

        return toReturn;
    }

    /**
     * @return the _chunksConsumedHistoryFromMCWhileConnectedToSC
     */
    @Override
    public Map<AbstractCachingPolicy, List<Chunk>> getChunksConsumedHistoryFromMCWhileConnectedToSC() {
        return Collections.unmodifiableMap(_chunksConsumedHistoryFromMCWhileConnectedToSC);
    }

    public Map<AbstractCachingPolicy, List<Chunk>> getConsumedHistoryFromMCwSCDiscon() {
        return Collections.unmodifiableMap(_consumedHistoryFromMCwSCDiscon);
    }

    public Map<AbstractCachingPolicy, List<Chunk>> getChunksConsumedHistoryFromMCAfterExitingSC() {
        return Collections.unmodifiableMap(_chunksConsumedHistoryFromMCAfterExitingSC);
    }

    @Override
    public List<Chunk> getChunksConsumedHistoryFromMCwSCConn(AbstractCachingPolicy policy) {
        return Collections.unmodifiableList(_chunksConsumedHistoryFromMCWhileConnectedToSC.get(policy));
    }

    @Override
    public List<Chunk> getChunksConsumedHistoryFromMCBeforeEnteringSC(AbstractCachingPolicy policy) {
        return Collections.unmodifiableList(_consumedHistoryFromMCwSCDiscon.get(policy));
    }

    @Override
    public List<Chunk> getChunksConsumedHistoryFromMCAfterExitingSC(AbstractCachingPolicy policy) {
        return Collections.unmodifiableList(_chunksConsumedHistoryFromMCAfterExitingSC.get(policy));
    }

    /**
     * policy
     *
     * @return the _consumedChunksFromSC
     */
    @Override
    public List<Chunk> getChunksCacheHitsHistory(AbstractCachingPolicy policy) {
        return Collections.unmodifiableList(_chunksHitsHistoryFromSC.get(policy));
    }

    /**
     * @return the _chunksConsumedHistoryFromBH
     */
    @Override
    public Map<AbstractCachingPolicy, List<Chunk>> getChunksConsumedHistoryFromBH() {
        return Collections.unmodifiableMap(_chunksConsumedHistoryFromBH);
    }

    @Override
    public List<Chunk> getChunksConsumedHistoryFromBH(AbstractCachingPolicy policy) {
        return Collections.unmodifiableList(_chunksConsumedHistoryFromBH.get(policy));
    }

    /**
     * Merges the value entries in the second to the value entries in the first
     *
     * firstMap secondMap
     */
    protected final void mergeToFirstMap(
            Map<AbstractCachingPolicy, List<Chunk>> firstMap,
            Map<AbstractCachingPolicy, List<Chunk>> secondMap) {
        for (Map.Entry<AbstractCachingPolicy, List<Chunk>> entry : secondMap.entrySet()) {
            AbstractCachingPolicy policy = entry.getKey();
            Collection<Chunk> chunksInSecond = entry.getValue();

            List<Chunk> chunksInFirst = firstMap.get(policy);
            if (chunksInFirst == null) {
                firstMap.put(policy, (chunksInFirst = new ArrayList<>()));
            }
            chunksInFirst.addAll(chunksInSecond);
        }
    }

    protected final void mergeAndSeparatePerpolicy(
            Map<AbstractCachingPolicy, List<Chunk>> firstMap,
            Map<AbstractCachingPolicy, List<Chunk>> secondMap) {
        for (Map.Entry<AbstractCachingPolicy, List<Chunk>> entry : secondMap.entrySet()) {
            AbstractCachingPolicy policy = entry.getKey();
            Collection<Chunk> chunksInSecond = entry.getValue();

            List<Chunk> chunksInFirst = firstMap.get(policy);
            if (chunksInFirst == null) {
                firstMap.put(policy, (chunksInFirst = new ArrayList<>()));
            }
            chunksInFirst.addAll(chunksInSecond);

        }
    }

    protected final void merge2(Map<AbstractCachingPolicy, List<Chunk>> firstMap,
            Map<AbstractCachingPolicy, Set<Chunk>> secondMap) {
        for (Map.Entry<AbstractCachingPolicy, Set<Chunk>> entry : secondMap.entrySet()) {
            AbstractCachingPolicy policy = entry.getKey();
            Collection<Chunk> chunksInSecond = entry.getValue();

            List<Chunk> chunksInFirst = firstMap.get(policy);
            if (chunksInFirst == null) {
                firstMap.put(policy, (chunksInFirst = new ArrayList<>()));
            }
            chunksInFirst.addAll(chunksInSecond);
        }
    }

    public boolean isFullyConsumed() {
        return _uncompletedPolicies == 0;
    }

    public int getCompetionSimTime(AbstractCachingPolicy policy) {
        return _completitionTimes.get(policy);
    }

    public void forceComplete(int time) {
        _uncompletedPolicies = 0;
        for (AbstractCachingPolicy policy : getSim().getCachingPolicies()) {
            _completitionTimes.put(policy, time);
        }
    }

    /**
     * policy
     *
     * @return
     */
    public List<Chunk> getUnconsumedChunksInSequence(AbstractCachingPolicy policy) {
        return _unconsumedChunksInSequence.get(policy);
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

    public List<Chunk> getChunksConsumedOverall(AbstractCachingPolicy policy) {
        List<Chunk> tmp = new ArrayList(referredContentDocument().chunks());
        tmp.removeAll(getUnconsumedChunksInSequence(policy));

        return tmp;
    }

}
