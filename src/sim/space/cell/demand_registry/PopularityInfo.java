package sim.space.cell.demand_registry;

import exceptions.InvalidOrUnsupportedException;
import java.util.HashMap;
import java.util.Map;
import sim.content.request.DocumentRequest;
import sim.space.cell.AbstractCell;
import sim.space.cell.smallcell.BufferBase;
import sim.space.users.mobile.MobileUser;
import sim.content.ContentDocument;
import traces.dmdtrace.TraceLoader;

/**
 *
 * @author xvas
 */
public class PopularityInfo {

    public class RegistrationInfo {

        /**
         * The most recent (the latest) serial number of a particular item
         * requested.
         */
        private long _last;
        private double _lastTimeOfReq;
        /**
         * The second-last serial number of a request for a particular item
         * requested.
         */
        private long _penultimate;
        private double _penultimateTimeOfReq;

        /**
         *
         *  last last generated request number
         *  lastTimeOfContent last time of request as loaded from the
         * trace.
         */
        private RegistrationInfo(long last, double lastTimeOfContent) {
            _last = last;
            _lastTimeOfReq = lastTimeOfContent;
            _penultimate = -1;
            _penultimateTimeOfReq = -1.0;
        }


        /**
         * The most recent (the latest) serial of request.
         *
         * @return the last serial of requests
         */
        public long getLast() {
            return _last;
        }

        /**
         * The most recent (the latest) serial number of a particular item
         * requested.
         *
         *  last
         */
        protected void setLast(long last) {
            _last = last;
        }

        /**
         * The second-last serial of requests recorded about a particular item.
         *
         * @return the penultimate serial of requests recorded about a
         * particular item.
         */
        public long getPenultimate() {
            return _penultimate;
        }

        /**
         * The second-last serial number of a request for a particular item
         * requested.
         *
         *  penultimateSerial the _penultimate to set
         */
        protected void setPenultimate(long penultimateSerial) {
            _penultimate = penultimateSerial;
        }

        private double getLastTimeOfReq() {
            return _lastTimeOfReq;
        }

        /**
         * @return the _penultimateTimeOfReq
         */
        public double getPenultimateTimeOfReq() {
            return _penultimateTimeOfReq;
        }

        /**
         *  _penultimateTimeOfReq the _penultimateTimeOfReq to set
         */
        public void setPenultimateTimeOfReq(double _penultimateTimeOfReq) {
            this._penultimateTimeOfReq = _penultimateTimeOfReq;
        }

        /**
         *  _lastTimeOfReq the _lastTimeOfReq to set
         */
        public void setLastTimeOfReq(double _lastTimeOfReq) {
            this._lastTimeOfReq = _lastTimeOfReq;
        }

    }//RegistrationInfo

    /**
     * Used for generating a serial number for each request information update.
     * The current value can be used to infer how many requests information have
     * been recorded up to now.
     */
    private long _requestSerialGen = 0;

    private final Map<Long, PopularityInfo.RegistrationInfo> _demandMap;

    final AbstractCell _cell;

    public PopularityInfo(AbstractCell cell) {
        _cell = cell;
        _demandMap = new HashMap<>(100);
    }

    /**
     *  doc
     * @return the currently registered information regarding the item or null
     * if no registration exists.
     */
    public RegistrationInfo getRegisteredInfo(ContentDocument doc) {
        return _demandMap.get(doc.getID());
    }
    
  
    public AbstractCell getCell() {
        return _cell;
    }

    public void registerPopInfo(DocumentRequest r) {
        long rID = r.referredContentDocument().getID();
        RegistrationInfo reqDetails = _demandMap.get(rID);

        if (reqDetails == null) {// if this is the first time this content is registered..
            reqDetails = new RegistrationInfo(incrNxtContentSerial(), r.getTime());
            _demandMap.put(rID, reqDetails);
            return;
        }
        reqDetails._penultimate = reqDetails._last;
        reqDetails._last = incrNxtContentSerial();
    }

    /**
     * Computes the popularity based on the latest two past requests recorded
     * for this item.
     *
     *  doc
     * @return the computed popularity, or 0 if no information is available for
     * the item
     */
    public double computePopularity1(ContentDocument doc) {
        PopularityInfo.RegistrationInfo reqDetails = _demandMap.get(doc.getID());
        if (reqDetails == null) {
            return 0;
        }

        double lastSerial = reqDetails.getLast();
        double penultimateSerial = reqDetails.getPenultimate();

        if (penultimateSerial != -1) {
            return 1.0 / (lastSerial - penultimateSerial);
        }
        return 0;
    }

    /**
     * Computes the popularity based on the latest two past requests recorded
     * for this item.
     *
     *  doc
     *  buff
     * @return the computed popularity, or 0 if no information is available for
     * the item
     */
    public double computePopularity2(ContentDocument doc, BufferBase buff) {
        RegistrationInfo reqDetails = _demandMap.get(doc.getID());
        if (reqDetails == null) {
            return 0;
        }

        double lastTimeInBuff = buff.getLastRequestTime();
        double lastTimeOfReq = reqDetails.getLastTimeOfReq();

        /*the following code takes care of the request times loaded
         * from the trace. It looks complex bacause it is tricky in  a simulation.
         * Requests are iterated not in the order of request time in the trace, 
         * but in the order of mobile requests (that can mix requests and, thus, their times),
         * which makes the following checks necessary*/
        if (lastTimeInBuff <= lastTimeOfReq) {
            buff.setPenultimateRequestTime(lastTimeInBuff);
            buff.setLastRequestTime(lastTimeOfReq);
            lastTimeInBuff = lastTimeOfReq;
        } else {
            reqDetails.setLastTimeOfReq(lastTimeInBuff);
            lastTimeOfReq = lastTimeInBuff;
        }

        double penultimateInBuff = buff.getPenultimateRequestTime();
        double penultimateOfReq = reqDetails.getPenultimateTimeOfReq();

        if (penultimateInBuff < penultimateOfReq) {
            buff.setPenultimateRequestTime(penultimateOfReq);
            penultimateInBuff = penultimateOfReq;
        }

        if (penultimateOfReq != -1) {
            return (lastTimeInBuff - penultimateInBuff)
                    / lastTimeOfReq - penultimateOfReq;
        } else {
            return 0.0;
        }
    }

    /**
     *  item the item
     * @return the popularity of the item as defined in the loaded trace of
     * requests.
     */
    public double computePopularity3(ContentDocument item) {
        TraceLoader trcLoader = this.getCell().getSim().getTrcLoader();
        return trcLoader.frequency(item.getID());
    }

    /**
     * Use this method to generating a serial number for the next request
     * information update.
     *
     * @return the next serial number generated.
     */
    protected long incrNxtContentSerial() {
        return ++_requestSerialGen; // first serial is zero as in the list used
    }

    /**
     * @return the latest serial number generated.
     */
    protected long latestSerialGenerated() {
        return _requestSerialGen;
    }

}
