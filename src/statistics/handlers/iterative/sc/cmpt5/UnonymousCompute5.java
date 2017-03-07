package statistics.handlers.iterative.sc.cmpt5;

import caching.base.AbstractCachingPolicy;
import statistics.handlers.BaseHandler;
import statistics.handlers.ICompute5_6;

/**
 *
 * @author Xenofon Vasilakos xvas@aueb.gr
 */
public class UnonymousCompute5 extends BaseHandler implements ICompute5_6 {

    private final AbstractCachingPolicy _cachingMethod;
    private final WellKnownTitle _title;

    public UnonymousCompute5(AbstractCachingPolicy cachingMethod, WellKnownTitle title) {
        _cachingMethod = cachingMethod;
        _title = title;
    }

    public UnonymousCompute5(AbstractCachingPolicy nxtMthd, String nxtHandlerTitl) {
        this(nxtMthd, WellKnownTitle.valueOf(nxtHandlerTitl));
    }

    public static enum WellKnownTitle {
        /**
         * Keeps track of cached requests canceled after a mobile handoff.
         *//**
         * Keeps track of cached requests canceled after a mobile handoff.
         */
        CNCLD("Cncld", "Keeps track of the percentage of the items canceled out of those"
                + " requested to be canceled if cached (useful if items remain cached due"
                + " tobeing requested by other mobile requestors too)."),
        /**
         * Keeps track of the percentage of the items canceled out of those
         * requested to be canceled if cached (useful if items remain cached due
         * to being requested by other mobile requestors too).
         */
        CNCLD_PERCENT("Cncld%", "Keeps track of the percentage of the items canceled out of"
                + " those requested to be canceled if cached (useful if items remain cached"
                + " due tobeing requested by other mobile requestors too)."),
        /**
         * Keeps track of the number of cache replacements regarding
         * replacement-based methods.
         */
        ITMS_RPLCD("ITMS_RPLCD", "Keeps track of the number of cache replacements regarding"
                + " replacement-based methods."),
        /**
         * Keeps track of the number of not cached items due to high price.
         */
        ITMS_NOT_CACHED_BY_PRICE("NotByPrice", "Keeps track of the number of cache replacements regarding"),
        ITMS_ALREADY_CACHED("AlreadyCached", "Keeps track of the number of not cached items because they were already present in the cache at time of request"),
        ITMS_ALREADY_CACHED2("AlreadyCached2", "Keeps track of the number of not cached items because they were already present in the cache at time of request"),
        /**
         * Keeps track of the percentage of cache replacements out of the cache
         * replacements proposed by the replacement algorithm used.
         */
        ITMS_RPLCD_PERCENT("ITMS_RPLCD%", "Keeps track of the percentage of cache "
                + "replacements out of the cache replacements proposed by the replacement "
                + "algorithm used."),
        /**
         * Keeps track of the popularity demand factor values.
         */
        W_POP("W_POP", "Keeps track of the popularity demand factor values."),
        LCL_DMD("LCL_DMD", "Keeps track of the local demand by connected mobile users."),
        PC_DMD("PC_DMD", "Keeps track of the proactive caching demand."),
       
        CACHE_ATTEMPT("SUCCESS_CACHING%", "Keeps track of the percentage of successfull caching attempts."),
        CACHE_ATTEMPT2("SUCCESS2_CACHING%", "Keeps track of the percentage of successfull caching attempts, including those for requests for which items are already cached.");

        private String _ttl;
        private String _description;

        WellKnownTitle(String title, String description) {
            _ttl = title;
            _description = description;
        }

    }

    /**
     * @param d
     * @return
     */
    @Override
    public final double compute5_6(double d) {
        return d;
    }

    @Override
    public String title() {
        return _title._ttl + "(" + getCachingMethod() + ")";
    }

    public String getCachingMethod() {
        return _cachingMethod.toString();
    }
}
