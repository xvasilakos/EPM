package sim.space.cell.demand_registry;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import sim.space.cell.AbstractCell;
import sim.space.users.User;
import sim.content.ContentDocument;
import sim.content.request.DocumentRequest;
import sim.space.users.CachingUser;

/**
 * Each cell has an object of this class to keep track of information regarding
 * the requests demand by mobile users.
 */
public class LocalDemand {

    /**
     * Used for recorded request details by mobiles which got connected to this
     * cell at some point.
     */
    public class RegistrationInfo {

        private int _overallNumOfReqs = 0;
        /**
         * Mobiles requesting the content. The number of times a mobile requests
         * this particular content is mapped to the requesting mobile.
         */
        private final Map<User, Integer> _numOfReqsByUser;

        private RegistrationInfo() {
            _numOfReqsByUser = new HashMap<>(535, 1);
        }

        public void reset() {
            this._numOfReqsByUser.clear();
            this._overallNumOfReqs = 0;
        }

        private void addCU(User mu) {
            Integer num = _numOfReqsByUser.get(mu);
            if (num == null) {
                _numOfReqsByUser.put(mu, 1);
            } else {
                _numOfReqsByUser.put(mu, num + 1);
            }

            _overallNumOfReqs++;
        }

        private boolean removeUser(User mu) {
            Integer num = _numOfReqsByUser.remove(mu);

            if (num == null) {
                return false;
            }

            _overallNumOfReqs -= num;
            return true;
        }

        /**
         * @return the _overallNumOfReqs
         */
        public int getOverallNumOfReqs() {
            return _overallNumOfReqs;
        }

    }

    private double _currDemandNumForW;

    /**
     * A map used for mapping details (either from a currently connected or a
     * past connected mobile) to the requested content's ID.
     */
    private final Map<String, RegistrationInfo> _demandMap;

    final AbstractCell _cell;

    public LocalDemand(AbstractCell cell) {
        this._currDemandNumForW = -1.0;
        _cell = cell;
        _demandMap = new HashMap<>(100);
    }

    public void resetCurrDemand() {
        this._currDemandNumForW = -1.0;
        _demandMap.clear();
    }

    /**
     * @return the _demandMap
     */
    public Set<String> getRegistertedItemIDs() {
        return Collections.unmodifiableSet(_demandMap.keySet());
    }

    public int registeredItemIdsCount() {
        return _demandMap.size();
    }

    /**
     * @param item
     * @return the currently registered information regarding the item or null
     * if no registration exists.
     */
    public RegistrationInfo getRegisteredInfo(ContentDocument item) {
        return _demandMap.get(item.getID());
    }

    public AbstractCell getCell() {
        return _cell;
    }

    public void registerLclDmdForW(CachingUser cu, double weight) {
        String trcDocumentID;
        for (DocumentRequest nxt : cu.getRequests()) {
            trcDocumentID = nxt.referredContentDocument().getID();

            LocalDemand.RegistrationInfo reqDetails = _demandMap.get(trcDocumentID);
            if (reqDetails == null) {// if this is the first time this content is registered..
                reqDetails = new LocalDemand.RegistrationInfo();
                _demandMap.put(trcDocumentID, reqDetails);
            }

            reqDetails.addCU(cu);
        }
        updtCurrDemandNumForW(weight);
    }

    public void deregisterLclDmdForW(CachingUser cu, double weight) {
        for (DocumentRequest req : cu.getRequests()) {
            LocalDemand.RegistrationInfo reqDetails = _demandMap.get(req.getID());
            if (reqDetails != null) {
                reqDetails.removeUser(cu);
            }
        }
        updtCurrDemandNumForW(weight);
    }

    /**
     */
    private void updtCurrDemandNumForW(double weight) {
        double prevDmd = _currDemandNumForW;

        _currDemandNumForW = 0;
        for (RegistrationInfo nxt : _demandMap.values()) {
            for (Integer nxtMUReqsNum : nxt._numOfReqsByUser.values()) {
                _currDemandNumForW += nxtMUReqsNum;
            }
        }

        _currDemandNumForW = weight * _currDemandNumForW + (1.0 - weight) * prevDmd;
    }

    /**
     * @return the _currDemandNumForW
     */
    public double getCurrDemandNumForW() {
        return _currDemandNumForW;
    }

    public int getCurrDemandNumFor(long contentID) {
        RegistrationInfo nfo = _demandMap.get(contentID);
        return nfo == null ? 0 : nfo.getOverallNumOfReqs();
    }

    public double computeAvgW() {
        return (_demandMap.size() > 0) ? _currDemandNumForW / _demandMap.size() : -1;
    }
}
