package sim.space.cell.demand_registry;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import sim.content.Chunk;
import sim.content.request.DocumentRequest;
import sim.space.cell.AbstractCell;
import sim.space.users.CachingUser;

/**
 * Each cell has an object of this class to keep track of information regarding
 * the requests demand by mobile users.
 */
public class PCDemand {

    /**
     * @return the _demandMap
     */
    public Map<String, RegistrationInfo> getDemandMap() {
        return Collections.unmodifiableMap(_demandMap);
    }

    public void clear() {
        _demandMap.clear();
    }

    /**
     * Used for recorded request details by mobiles which got connected to this
     * cell at some point.
     */
    public class RegistrationInfo {

        /**
         * Maps hand over probabilities to requesting users for particular
         * theChunk. If this registry is used for proactive caching demand, then
         * the value of each pair is the hand over probability of the requesting
         * mobile user (who is the key in the pair).
         *
         * Note that if probabilities are irrelevant, the key set can still be
         * used to record the requesting mobile users.
         *
         */
        protected final Map<CachingUser, Double> _usersCurrRequestingProbs;
        protected final Map<CachingUser, Integer> _usersCurrRequestingTimes;

        /**
         * Keeps state about the aggregated probability from all requesting
         * mobile users that are currently registered.
         */
        private double _aggrProb;
        /**
         * How many requests for this theChunk
         */
        private int _aggrReqNum;

        private RegistrationInfo() {
            _usersCurrRequestingProbs = new HashMap<>(5);
            _usersCurrRequestingTimes = new HashMap<>(5);
            _aggrProb = 0.0;
            _aggrReqNum = 0;
        }

        /**
         * @return aggregate probability by all currently still registered
         * users.
         */
        public double sumTransProbs() {
            return _aggrProb;
        }

        public double sumReqsNum() {
            return _aggrReqNum;
        }

        public Set<CachingUser> cachingUsers() {
            return Collections.unmodifiableSet(_usersCurrRequestingProbs.keySet());
        }

    }//RegisteredInfo class

    /**
     * A map used for mapping the details of a request (either from a currently
     * connected or a past connected mobile) to the theChunk IDs that the
     * requests were made for.
     */
    private final Map<String, RegistrationInfo> _demandMap;

    final AbstractCell _cell;

    private long _currDemandNum;

    public PCDemand(AbstractCell cell) {
        _currDemandNum = 0;
        _cell = cell;
        _demandMap = new HashMap<>(100);
    }

    public AbstractCell getCell() {
        return _cell;
    }

    /**
     * @param theChunk
     * @return the currently registered information regarding the theChunk or
     * null if no registration exists.
     */
    public RegistrationInfo getRegisteredInfo(Chunk theChunk) {
        return _demandMap.get(theChunk.getID());
    }

    /**
     * @return the _currDemandNum
     */
    public long getCurrDemandNum() {
        return _currDemandNum;
    }

    public void deregisterUpdtInfoPC(CachingUser cu, DocumentRequest nxtRequest) {

        for (Chunk nxtReqChunk : nxtRequest.referredContentDocument().chunks()) {

            RegistrationInfo reqDetails = _demandMap.get(nxtReqChunk.getID());
            if (reqDetails == null) {
                // ignore. can happen
                continue;
            }

            Double probsReq = reqDetails._usersCurrRequestingProbs.remove(cu);
            if (probsReq == null) {
                // ignore. can happen
                continue;
            }

            reqDetails._aggrProb -= probsReq;
            _currDemandNum = _currDemandNum < 2 ? 0 : _currDemandNum - 1;
            if (reqDetails._aggrProb < 0) {//probs are dynamic, so small incosistencies may occur. That is normal 
//                _demandMap.remove(nxtReqChunk.getID());
                reqDetails._aggrProb = 0;
                continue;
            }

            Integer timesReq = reqDetails._usersCurrRequestingTimes.remove(cu);
            reqDetails._aggrReqNum -= timesReq;

            if (reqDetails._usersCurrRequestingTimes.isEmpty()
                    || reqDetails._aggrReqNum == 0) {
                // then no one requests this theChunk anymore
                _demandMap.remove(nxtReqChunk.getID());
            }
        }
    }

    /**
     * Updates information about a request for an theChunk and returns updated
     * number of recorded requests.
     *
     *
     * @param chunkRequested the requested theChunk
     * @param cu the mobile user who issues a request for the theChunk
     * @param prob a probability that the mobile requests the theChunk. This is
     * disregarded when localDmd is passed a true value otherwise false for
     * proactive caching
     */
    public void registerUpdtInfoPC(Chunk chunkRequested, CachingUser cu, double prob) {
        RegistrationInfo reqDetails = _demandMap.get(chunkRequested.getID());

        if (reqDetails == null) {// if  the first time
            reqDetails = new RegistrationInfo();
            _demandMap.put(chunkRequested.getID(), reqDetails);
            reqDetails._usersCurrRequestingTimes.put(cu, 1);
            reqDetails._aggrReqNum = 1;
            reqDetails._usersCurrRequestingProbs.put(cu, prob);
            reqDetails._aggrProb = prob;
            _currDemandNum++;
            return;
        }

        Integer prevTimes = reqDetails._usersCurrRequestingTimes.get(cu);
        Double prevProb = reqDetails._usersCurrRequestingProbs.get(cu);
        if (prevTimes != null) {
            // in case the same user requests the same object more than once
            // which is possible when simulating multiple mobiles with a single mobile
            reqDetails._usersCurrRequestingTimes.put(cu, prevTimes + 1);
            if (prevProb == null) {
                prevProb = 0.0;

//                throw new InconsistencyException("Requested already: " + prevTimes + " but previous probability unknown: " + prevProb);
            }
            reqDetails._usersCurrRequestingProbs.put(cu, prevProb + prob);
        } else {
//            if (prevProb != null) {
//                throw new InconsistencyException("Not previously requested: " + prevTimes + " but previous probability known: " + prevProb);
//            }
            // this is the first time (or -in cae prevprob==null or zero- legacy cached with zero prob
            reqDetails._usersCurrRequestingTimes.put(cu, 1);
            reqDetails._usersCurrRequestingProbs.put(cu, prob);
        }
        reqDetails._aggrReqNum++;
        reqDetails._aggrProb += prob;

        _currDemandNum++;
    }

}
