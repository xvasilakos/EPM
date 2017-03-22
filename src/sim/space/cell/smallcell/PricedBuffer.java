package sim.space.cell.smallcell;

import app.properties.Cost;
import app.properties.valid.Values;
import caching.base.AbstractCachingModel;
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
import utils.DebugTool;

/**
 * Buffer type that applies a cache congestion pricing scheme.
 *
 *
 * @author Xenofon Vasilakos (xvas{@literal @}aueb.gr - mm.aueb.gr/~xvas),
 * Mobile Multimedia Laboratory (mm.aueb.gr), Dept. of Informatics, School of
 * Information {@literal Sciences & Technology}, Athens University of Economics
 * and Business, Greece
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

        _pricingScheme = setup.stringProperty(Cost.Cache.PRICING_SCHEME);
        _gamma = setup.doubleProperty(Cost.EPC.GAMMA);

        _costTrgtUtilization = setup.doubleProperty(Cost.EPC.TARGET_UTILIZATION);

        _price = 0.0;
        _price4Rplc = 0.0;
    }

    /**
     * Extends the inherited functionality from #BufferBase by updating the
     * price of the buffer, unless the requested chunk is already cached in the
     * buffer, in which case the price remains as it is.
     *
     *
     * @param cu
     * @param chunk
     * @param sc
     * @return the BufferAllocationStatus after trying to allocate space in the
     * buffer for chunk
     */
    @Override
    BufferAllocationStatus allocateAttempt(CachingUser cu, Chunk chunk, SmallCell sc) {
        //        DebugTool.appendln("pre-utilization()=" + utilization());

        BufferAllocationStatus result = super.allocateAttempt(cu, chunk, sc);

        if (result == BufferAllocationStatus.Success 
                ) {
            double pricePoll = priceΑllocatePoll(chunk);
            setPrice(pricePoll);
        }
        return result;
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
    public Set<CachingUser> deallocateAttempt(Chunk chnk, MobileUser mu,
            AbstractCachingModel policy, SmallCell sc)
            throws NoSuchElementException {

        double pricePolled = PricedBuffer.this.priceDeallocatePoll(chnk);

        Set<CachingUser> mobsStillRequesting = super.deallocateAttempt(chnk, mu, policy, sc);

        if (mobsStillRequesting.isEmpty()) {
            _price = pricePolled;
        }

        return mobsStillRequesting;
    }

    /**
     * Computes price by considering buffer utilization computed only over the
     * cached items for which the expected (assessed) gain is lower than the
     * current price. This method of price computation is suitable for cache
     * replacement decisions based on gain, which are used for #AbstractEPCP
     * descendants which use popularity and summary of transition probabilities
     * for all requestors .
     *
     * @return
     * @throws Throwable
     */
    public double priceUpdt4Rplc(IGainRplc policy) throws Throwable {
//        appendLog("Updating price.. ", _cell, (AbstractCachingModel) mthd);
//        appendLog("Price before update= " + getPrice4Rplc(), _cell, (AbstractCachingModel) mthd);

        double consideredUtil = utilization4Rplc(policy);

        setPrice4Rplc(
                getPrice4Rplc() + getGamma() * (consideredUtil - getTrgtUtililzation())
        );
        if (getPrice4Rplc() < 0) {
            setPrice4Rplc(0);
        }

//        appendLog("Price after update= " + getPrice4Rplc(), _cell, (AbstractCachingModel) mthd);
        return getPrice4Rplc();
    }

    public double utilization4Rplc(IGainRplc mthd) throws Throwable {
        double used4Price = 0.0;
        double price4Rplc = getPrice4Rplc();

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
     * caching the theChnk or not.
     *
     *
     * deallocateAttempt true if this call is for evicting the items, false if
     * it is for adding the items. theChnk the theChnk polled to be cached or
     * evicted from items.
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

    double priceΑllocatePoll(Chunk theChnk) {
        if (theChnk == null) {
            throw new InconsistencyException(
                    "No theChnk passed: " + theChnk);
        }
        switch (_pricingScheme) {
            case Values.DYNAMIC__TYPE_01:
                return pricingDynamic01Αlloc(theChnk);

            case Values.DYNAMIC__TYPE_02:
                return pricingDynamic02Αlloc(theChnk);
        }
        // if reached here, then ..
        throw new UnsupportedOperationException(
                "Unknown or unsupported parameter value: " + _pricingScheme);
    }

    double pricePoll() throws Throwable {

        switch (_pricingScheme) {
            case Values.DYNAMIC__TYPE_01:
                return pricingDynamic01Poll();

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
     * theChnk the chunk polled to add/evict deallocateAttempt true for
     * eviction, otherwise false for addition.
     *
     * @return
     */
    private double pricingDynamic01Dealloc(Chunk theChnk) {
        double pricePolled;
        long size = theChnk.sizeInBytes();
        double polledUtil = utilizationPollAndCheck(size, true);

        pricePolled = getPrice() + getGamma() * (polledUtil - getTrgtUtililzation());
        pricePolled = pricePolled < 0 ? 0 : pricePolled; // do not allow negative values

        return pricePolled;
    }

    private double pricingDynamic01Αlloc(Chunk theChnk) {
        double pricePolled;
        long size = theChnk.sizeInBytes();
        double polledUtil = utilizationPollAndCheck(size, false);

        pricePolled = getPrice() + getGamma() * (polledUtil - getTrgtUtililzation());
        pricePolled = pricePolled < 0 ? 0 : pricePolled; // do not allow negative values

//        DebugTool.appendln(
//                "getPrice() + getGamma() * (polledUtil - getTrgtUtililzation() = "
//                + getPrice()
//                + " + " + getGamma()
//                + " * "
//                + " + "
//                + "("
//                + polledUtil + "-" + getTrgtUtililzation()
//                + ")"
//        );

        return pricePolled;
    }

    private double pricingDynamic01Poll() {
        double pricePolled;

        double polledUtil = _used / _capacityInBytes;

        pricePolled = getPrice() + getGamma() * (polledUtil - getTrgtUtililzation());
        pricePolled = pricePolled < 0 ? 0 : pricePolled; // do not allow negative values

        return pricePolled;
    }

    /**
     * See properties description for parameter cost.cache.pricing_scheme when
     * value dynamic.type_02 is _used.
     *
     * theChnk the list of items polled to add/evict deallocateAttempt true for
     * eviction, otherwise false for addition.
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

    private double pricingDynamic02Αlloc(Chunk theChnk) {
        double pricePolled;
        long size = theChnk == null ? 0 : theChnk.sizeInBytes();
        double polledUtil = utilizationPollAndCheck(size, false);

        if (polledUtil > getTrgtUtililzation()) {
            pricePolled = getPrice() + getGamma() * (polledUtil - getTrgtUtililzation());
        } else {
            pricePolled = 0.0;
        }

        return pricePolled;
    }

    /**
     *
     * @param deallocate if false, then it computes the price based on
     * allocating added buffer space; other based on deallocating space from the
     * allocated part of the buffer.
     * @return
     */
    private double pricingDynamic02Poll(boolean deallocate) {
        double pricePolled;
        double polledUtil = utilizationPollAndCheck(0, deallocate);

//        DebugTool.appendln(
//                "\n\t"
//                + "post-getUsed()=" + getUsed()
//                + "\n\t"
//                + "post-getPrice()=" + getPrice()
//        );

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
