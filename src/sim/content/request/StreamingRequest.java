package sim.content.request;

import caching.base.AbstractCachingPolicy;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import sim.content.Chunk;
import sim.space.users.CachingUser;
import traces.dmdtrace.TraceWorkloadRecord;

/**
 * @author xvas
 */
public class StreamingRequest extends DocumentRequest {

    public StreamingRequest(TraceWorkloadRecord workloadRecord, CachingUser requesterUser) {
        super(workloadRecord, requesterUser);
    }

    @Override
    public void consumeTry(
            double mcRateSlice, Map<AbstractCachingPolicy, List<Chunk>> fillInWithDownloadedFromMC,
            double scRateSlice, Map<AbstractCachingPolicy, List<Chunk>> fillInWithCacheHits,
            double minSCorBHRateSlice, Map<AbstractCachingPolicy, List<Chunk>> fillInWithDownloadedFromBH,
            Map<AbstractCachingPolicy, List<Chunk>> fillInWithMissedPerPolicy) {

        long chunkSizeInBytes = getSimulation().chunkSizeInBytes();

        boolean userConnected = getRequesterUser().isConnected();

        long maxBudget;

        if (!userConnected) {
            maxBudget = Math.round(mcRateSlice / chunkSizeInBytes);
            for (AbstractCachingPolicy policy : getSimulation().getCachingStrategies()) {
                mergeToFirstMap(fillInWithDownloadedFromMC, super.consumeFromMCwSCDiscon(policy, maxBudget));
            }
        } else {// in this case, downloads from all reasources, with this *priority*: 
            for (AbstractCachingPolicy policy : getSimulation().getCachingStrategies()) {
// CAUTION! do not change  the priority of the following invokations!    

                if (policy != caching.incremental.Oracle.instance()) {
                    // cached chunks are fully consumed at caching decision time  
                    // (See the caching method for Oracle..)
                    //since it is oracle.
                    
// First, consumeTry from the cache
                    maxBudget = Math.round(scRateSlice / chunkSizeInBytes);
                    Map<AbstractCachingPolicy, List<Chunk>> hitsInCachePerPolicy
                            = tryStreamingFromCachePerPolicy(policy, maxBudget);
                    mergeToFirstMap(fillInWithCacheHits, hitsInCachePerPolicy);

// Second, from backhaul  
                    maxBudget = Math.round(minSCorBHRateSlice / chunkSizeInBytes);
                    mergeToFirstMap(fillInWithDownloadedFromBH,
                            streamCacheMissedFromBH(
                                    policy,
                                    maxBudget,
                                    hitsInCachePerPolicy
                            ));
                }
// Third and last, consumeTry from the macro
                maxBudget = Math.round(mcRateSlice / chunkSizeInBytes);
                mergeToFirstMap(fillInWithDownloadedFromMC, super.consumeFromMCwSCCon(policy,
                        maxBudget)
                );
            }
        }

    }

    /**
     *
     *  maxBudget the max number of chunks that can be downloaded from the
     * cache
     *  fillWithMissedByPolicy for some polices, some chunks may be
     * missed. These chunks must be considered for download by the BH or the MC
     * @return the consumed chunks from the cache
     */
    private Map<AbstractCachingPolicy, List<Chunk>> tryStreamingFromCachePerPolicy(
            AbstractCachingPolicy policy,
            long maxBudget) {
        Map<AbstractCachingPolicy, List<Chunk>> currentHitsInCachePerPolicy = new HashMap<>(5);

        List<Chunk> unconsumed = _unconsumedChunksInSequence.get(policy);
        if (unconsumed.isEmpty()) {
            if (_completitionTimes.get(policy) == -1) {
                _completitionTimes.put(policy, simTime());
                _uncompletedPolicies--;
            }
            return currentHitsInCachePerPolicy;
        }

        long budgetForPolicy = maxBudget;

        ArrayList<Chunk> currentChunkHits;
        currentHitsInCachePerPolicy.put(policy, currentChunkHits = new ArrayList<>());
        List<Chunk> historyChunkHits = _chunksHitsHistoryFromSC.get(policy);

        Iterator<Chunk> unconsumedIt = unconsumed.iterator(); // in ascending order of keys
        Set<Chunk> cachedChunks = _requesterUser.getCurrentlyConnectedSC().cachedChunksUnmodifiable(policy);
        while (unconsumedIt.hasNext() && budgetForPolicy-- > 0) {
            Chunk chunkConsumed = unconsumedIt.next();
            if (!cachedChunks.contains(chunkConsumed)) {
                // bandwidth got wasted
                // if not in the cache, skip
                continue;
            }
            unconsumedIt.remove();
            /* While being connected, only in this case the chunk is not 
                     * already consumed either from the BH nor from MC */
            historyChunkHits.add(chunkConsumed);
            currentChunkHits.add(chunkConsumed);

//                if (getSim().__tmpMaxPopSetXXX2.contains(nxtChunk)) {
//                    throw new RuntimeException();
//                }
//                if (getSim().__tmpMaxPopSetXXX.contains(nxtChunk.referredContentDocument())) {
//                    throw new RuntimeException();
//                }
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

        if (unconsumed.isEmpty()) {
            if (_completitionTimes.get(policy) == -1) {
                _completitionTimes.put(policy, simTime());
                _uncompletedPolicies--;
            }
        }

        return currentHitsInCachePerPolicy;
    }

    private Map<AbstractCachingPolicy, List<Chunk>> streamCacheMissedFromBH(
            AbstractCachingPolicy policy,
            long maxbudget, Map<AbstractCachingPolicy, List<Chunk>> cacheHitsPerPolicy
    ) {

        Map<AbstractCachingPolicy, List<Chunk>> bhCurrentConsumptionPerPolicy = new HashMap<>(5);

        List<Chunk> bhCurrentConsumption;
        bhCurrentConsumptionPerPolicy.put(policy, bhCurrentConsumption = new ArrayList<>());
        List<Chunk> bhHistororyConsumption = _chunksConsumedHistoryFromBH.get(policy);

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
        List<Chunk> hitChunks = cacheHitsPerPolicy.get(policy);
        int hitsNum = hitChunks == null ? 0 : hitChunks.size();
        long budgetForPolicy = maxbudget - hitsNum;

//xxx            if (_requesterUser.getClass() == StationaryUser.class
//                    && policy.getClass() == EMPC_LC_Full.class) {
//
//                        Utils.trackUser(false,
//                        "\n\t BH: budgetForPolicy = policyBudget /*maxbudget*/  - hitsNum = "
//                        + policyBudget + "/*" + maxbudget + "*/ -" + hitsNum + "="
//                        + budgetForPolicy,
//                        getRequesterUser(), true);
//            }            // if room in HB rate slice, then consumeTry from the unconsumed chunks
//            long xxx = budgetForPolicy;
        Iterator<Chunk> unconsumedIt = getUnconsumedChunksInSequence(policy).iterator(); // in ascending order of keys
        while (unconsumedIt.hasNext() && budgetForPolicy-- > 0) {
            Chunk chunkConsumed = unconsumedIt.next();
//                xxx++;
            unconsumedIt.remove();
            bhHistororyConsumption.add(chunkConsumed);
            bhCurrentConsumption.add(chunkConsumed);
        }
//xxx            if (_requesterUser.getClass() == StationaryUser.class
//                    && policy.getClass() == EMPC_LC_Full.class) {
//                Utils.trackUser(false,
//                        "\n\t\t consumed from BH: " + (xxx - budgetForPolicy),
//                        getRequesterUser(), true);
//            }

        if (unconsumed.isEmpty()) {
            if (_completitionTimes.get(policy) == -1) {
                _completitionTimes.put(policy, simTime());
                _uncompletedPolicies--;
            }
        }
        return bhCurrentConsumptionPerPolicy;
    }

}
