package sim.space.cell.smallcell;

import app.properties.Cost;
import app.properties.valid.Values;
import caching.base.AbstractCachingPolicy;
import caching.interfaces.rplc.IGainRplc;
import exceptions.InconsistencyException;
import java.util.NoSuchElementException;
import java.util.Set;
import sim.Scenario;
import sim.run.SimulationBaseRunner;
import sim.content.Chunk;
import sim.space.users.CachingUser;
import sim.space.users.mobile.MobileUser;
import utils.CommonFunctions;

/**
 * Type of buffer _used for caching decisions using prices.
 *
 * @author Xenofon Vasilakos xvas@aueb.gr
 */
public class PricedBuffer extends BufferBase {

    /**
     * The price of this buffer, as defined by the dynamic or fixed pricing
     * scheme in use.
     */
    protected double _price;
    private double _price4Rplc;

    /**
     * The pricing scheme in use.
     */
    private final String _pricingScheme;
    /**
     * The factor _used for updating the dynamic price of this buffer.
     */
    private final double _gamma;
    /**
     * The target utilization, which is _used for some dynamic pricing schemes
     * only.
     */
    private final double _costTrgtUtilization;

    public PricedBuffer(SimulationBaseRunner sim, SmallCell cell, long capacity) {
        super(sim, cell, capacity);

        Scenario setup = _simulation.getScenario();

        _pricingScheme = setup.stringProperty(Cost.Cache.PRICING_SCHEME, false);
        _gamma = setup.doubleProperty(Cost.EPC.GAMMA);

        _costTrgtUtilization = setup.doubleProperty(Cost.EPC.TARGET_UTILIZATION);

        _price = 0.0;
        _price4Rplc = 0.0;
    }

    /**
     *
     * @param chnk
     * @param mu
     * @param policy
     * @param sc
     * @return the list of users still requesting the cached theChnk.
     *
     * @throws NoSuchElementException
     */
    @Override
    public Set<CachingUser> deallocateTry(Chunk chnk, MobileUser mu,
            AbstractCachingPolicy policy, SmallCell sc)
            throws NoSuchElementException {

        double pricePolled = PricedBuffer.this.priceDeallocatePoll(chnk);

        Set<CachingUser> mobsStillRequesting = super.deallocateTry(chnk, mu, policy, sc);
        if (!mobsStillRequesting.isEmpty()) {// if the super class implementation did evict the theChnk
            _price = pricePolled;
        }

        return mobsStillRequesting;
    }

    public Set<CachingUser> deallocate(Chunk theChnk, MobileUser mu,
            IGainRplc policy, SmallCell sc) throws NoSuchElementException, Throwable {
        Set<CachingUser> result = super.deallocateTry(theChnk, mu, (AbstractCachingPolicy) policy, sc);
        //CAUTION, must reduce _used before re-assessing price
        priceUpdt4Rplc(policy);
        return result;
    }

    /**
     * Computes price by considering buffer utilization computed only over the
     * cached items for which the expected (assessed) gain is lower than the
     * current price. This method of price computation is suitable for cache
     * replacement decisions based on gain, which are used for AbstractEPCPop
     * descendants which use popularity and summary of transition probabilities
     * for all requestors .
     *
     * @return
     * @throws Throwable
     */
    public double priceUpdt4Rplc(IGainRplc policy) throws Throwable {
//        appendLog("Updating price.. ", _cell, (AbstractCachingPolicy) mthd);
//        appendLog("Price before update= " + getPrice4Rplc(), _cell, (AbstractCachingPolicy) mthd);

        double consideredUtil = utilization4Rplc(policy);

        setPrice4Rplc(
                getPrice4Rplc() + getGamma() * (consideredUtil - getTrgtUtililzation())
        );
        if (getPrice4Rplc() < 0) {
            setPrice4Rplc(0);
        }

//        appendLog("Price after update= " + getPrice4Rplc(), _cell, (AbstractCachingPolicy) mthd);
        return getPrice4Rplc();
    }

    public double utilization4Rplc(IGainRplc mthd) throws Throwable {
        double used4Price = 0.0;
        double price4Rplc = getPrice4Rplc();

//        if (price4Rplc > 0) {
//            DebugTool.appendLog("#util computation for non-zero price: " + price4Rplc,
//                    _cell, (AbstractCachingPolicy) mthd);
//            Set<Item> cachedChunksUnmodifiable = getCachedItems();
//            for (Chunk nxtItem : cachedChunksUnmodifiable) {
//                double assessment = mthd.assess(nxtItem, _cell) / nxtItem.sizeInMBs();
//
//                DebugTool.appendLog("#gain/size " + assessment,
//                        _cell, (AbstractCachingPolicy) mthd);
//
//                if (assessment >= price4Rplc) {
//                    used4Price += nxtItem.sizeInBytes();
//
//                    DebugTool.appendLog("#theChnk included in utilization computation",
//                            _cell, (AbstractCachingPolicy) mthd);
//                }
//
//                DebugTool.appendLog("#updated util = " + (used4Price / _capacity),
//                        _cell, (AbstractCachingPolicy) mthd);
//            }
//        } else {
        Set<Chunk> cachedItems = getCachedItems();
        for (Chunk nxtItem : cachedItems) {
            if (mthd.assess(nxtItem, _cell) / nxtItem.sizeInMBs() >= price4Rplc) {
                used4Price += nxtItem.sizeInBytes();
            }
        }
//        }

        return used4Price / getCapacityInBytes();
    }

    /**
     * Computes and returns the buffer price after polling to cache the theChnk
     * in the buffer (respectively to evict the theChnk depending on the
     * actionFlag parameter value).
     *
     * Note that the price does not reflect whether there is available space for
 caching the theChnk or not.


 deallocateTry true if this call is for evicting the items, false if it is
 for adding the items. theChnk the theChnk polled to be cached or evicted
 from items.
     *
     * @return The polled price
     */
    double priceDeallocatePoll(Chunk theChnk) {
        if (theChnk == null) {
            throw new InconsistencyException(
                    "No theChnk passed: " + theChnk);
        }
        switch (_pricingScheme) {
            case Values.DYNAMIC__TYPE_01:
                return pricingDynamic01Dealloc(theChnk);

            case Values.DYNAMIC__TYPE_02:
                return pricingDynamic02Dealloc(theChnk);
        }
        // if reached here, then ..
        throw new UnsupportedOperationException(
                "Unknown or unsupported parameter value: " + _pricingScheme);
    }

    double pricePoll() throws Throwable {

        switch (_pricingScheme) {
            case Values.DYNAMIC__TYPE_01:
                return pricingDynamic01Poll(false);

            case Values.DYNAMIC__TYPE_02:
                return pricingDynamic02Poll(false);
        }
        // if reached here, then ..
        throw new UnsupportedOperationException(
                "Unknown or unsupported parameter value: " + _pricingScheme);
    }

    /**
     * See properties description for parameter cost.cache.pricing_scheme when
     * value dynamic.type_01 is _used.
     *
     * theChnk the chunk polled to add/evict deallocateTry true for eviction,
 otherwise false for addition.
     *
     * @return
     */
    private double pricingDynamic01Dealloc(Chunk theChnk) {
        double pricePolled;
        long size
                = //theChnk == null ? 0 : 
                theChnk.sizeInBytes();
        double polledUtil = utilizationPollAndCheck(size, true);

        pricePolled = getPrice() + getGamma() * (polledUtil - getTrgtUtililzation());
        pricePolled = pricePolled < 0 ? 0 : pricePolled; // do not allow negative values

//        DebugTool.appendLn(
//                "\ntheChnk.sizeInMBs=" + theChnk.sizeInMBs()
//                + "\tpolledUtil=" + polledUtil
//                + "\tgetTrgtUtililzation()=" + getTrgtUtililzation()
//                + "\tpricePolled=" + pricePolled
//        );
//        DebugTool.appendLn(
        //                "\ntheChnk.sizeInMBs=" + theChnk.sizeInMBs()
//                polledUtil
//                + "," + getTrgtUtililzation()
//                + "," + pricePolled
//        );
        return pricePolled;
    }

    private double pricingDynamic01Poll(boolean deallocate) {
        double pricePolled;

        double polledUtil = utilizationPollAndCheck(0, deallocate);

        pricePolled = getPrice() + getGamma() * (polledUtil - getTrgtUtililzation());
        pricePolled = pricePolled < 0 ? 0 : pricePolled; // do not allow negative values

//        DebugTool.appendLn(
//                "\nPolling"
//                + "\tpolledUtil=" + polledUtil
//                + "\tgetTrgtUtililzation()=" + getTrgtUtililzation()
//                + "\tpricePolled=" + pricePolled
//        );
//        DebugTool.appendLn(
        //                "\ntheChnk.sizeInMBs=" + theChnk.sizeInMBs()
//                polledUtil
//                + "," + getTrgtUtililzation()
//                + "," + pricePolled
//        );
        return pricePolled;
    }

    /**
     * See properties description for parameter cost.cache.pricing_scheme when
     * value dynamic.type_02 is _used.
     *
     * theChnk the list of items polled to add/evict deallocateTry true for
 eviction, otherwise false for addition.
     *
     * @return
     */
    private double pricingDynamic02Dealloc(Chunk theChnk) {
        double pricePolled;
        long size = theChnk == null ? 0 : theChnk.sizeInBytes();
        double polledUtil = utilizationPollAndCheck(size, true);

        if (polledUtil > getTrgtUtililzation()) {
            pricePolled = getPrice() + getGamma() * (polledUtil - getTrgtUtililzation());
        } else {
            pricePolled = 0.0;
        }

        return pricePolled;
    }

    private double pricingDynamic02Poll(boolean deallocate) {
        double pricePolled;
        double polledUtil = utilizationPollAndCheck(0, deallocate);

        if (polledUtil > getTrgtUtililzation()) {
            pricePolled = getPrice() + getGamma() * (polledUtil - getTrgtUtililzation());
        } else {
            pricePolled = 0.0;
        }

        return pricePolled;
    }

    @Override
    public String toString() {
        StringBuilder _toString = new StringBuilder(180);
        Set<Chunk> items = _cachingUsersPerChunk.keySet();
        _toString.append("Buffer_").append(_cell.getID()).append(" buffers: ");
        for (Chunk theChnk : items) {

            _toString.append("<theChnk=").append(theChnk).append(" cached by: ")
                    .append(CommonFunctions.toString(_cachingUsersPerChunk.values()))
                    .append(">; ");
        }
        return _toString.toString();

    }

    /**
     * The factor _used for updating the dynamic price of this buffer.
     *
     * @return the _gamma
     */
    public double getGamma() {
        return _gamma;
    }

    /**
     * The target utilization, which is _used for some dynamic pricing schemes
     * only.
     *
     * @return the target utilization
     */
    public double getTrgtUtililzation() {
        return _costTrgtUtilization;
    }

    /**
     * The price of this buffer, as defined by the dynamic or fixed pricing
     * scheme in use.
     *
     * @return the price
     */
    public double getPrice() {
        return _price;
    }

    public double setPrice(double price) {
        return _price = price;
    }

    /**
     * @return the _price4Rplc
     */
    public double getPrice4Rplc() {
        return _price4Rplc;
    }

    /**
     * _price4Rplc the _price4Rplc to set
     */
    void setPrice4Rplc(double _price4Rplc) {
        this._price4Rplc = _price4Rplc;
    }

}
