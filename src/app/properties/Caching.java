package app.properties;

/**
 *
 * @author xvas
 */
public enum Caching implements IProperty {
    CACHING__POLICIES(
            "caching.policies",
            TooltipMsgs.CACHING_POLICIES),
    CACHING__POLICIES__MAXPOP_CUTTER(
            "caching.policies.MaxPop_cutter",
            ""),
    CACHING__PRELOAD__CACHES__ALL__POLICIES(
            "caching.preload_caches_all_policies",
            TooltipMsgs.CACHING__PRELOAD__CACHES__ALL__POLICIES),
    CACHING__RPLC__MINGAIN__SUM__HEURISTIC__TIME__DYNAMIC__READJUSTMENT_PERIOD(
            "caching.rplc.mingain.sum.heuristic.time.dynamic.readjustment_period",
            TooltipMsgs.CACHING__RPLC__MINGAIN__SUM__HEURISTIC__TIME__DYNAMIC__READJUSTMENT_PERIOD),
    CACHING__RPLC__MINGAIN__SUM__HEURISTIC__TIME__DYNAMIC_MAX_BOUND(
            "caching.rplc.mingain.sum.heuristic.time.dynamic.max_bound",
            ""),
    CACHING__RPLC__MINGAIN__SUM__HEURISTIC__TIME__DYNAMIC__STOPE(
            "caching.rplc.mingain.sum.heuristic.time.dynamic.stopE",
            TooltipMsgs.CACHING__RPLC__MINGAIN__SUM__HEURISTIC__TIME__DYNAMIC__STOPE),
    CACHING_POLICIES__NAIVE__TYPE02__THREASHOLD(
            "caching.naive.Type02.threshold",
            TooltipMsgs.CACHING_POLICIES__NAIVE__TYPE02__THREASHOLD);

    private final String _propertyName;
    private final String _tooltip;

    private Caching(String _propTitle, String tooltip) {
        _propertyName = _propTitle;
        _tooltip = tooltip;
    }

    @Override
    public String toString() {
        return _propertyName + ": " + toolTip();
    }

    @Override
    public String toolTip() {
        return _tooltip;
    }

    @Override
    public String propertyName() {
        return _propertyName;
    }

    class TooltipMsgs {

        public static final String CACHING_POLICIES
                = "A list of caching methods for the running simulation, e.g. EPC.";
        public static final String CACHING__PRELOAD__CACHES__ALL__POLICIES
                = "Should the caches be preloaded with the most popular content"
                + " from the trace of requests? If this is set to false, preloading"
                + " is used only for the MaxPop caching policy, otherwise it is used"
                + " for all policies.";
        public static final String CACHING__RPLC__MINGAIN__SUM__HEURISTIC__TIME__DYNAMIC__READJUSTMENT_PERIOD
                = "The simulation time that needs to elapse in order to run the "
                + " golden ration search algorithm";
        public static final String CACHING__RPLC__MINGAIN__SUM__HEURISTIC__TIME__DYNAMIC__STOPE = "";
        public static final String CACHING_POLICIES__NAIVE__TYPE02__THREASHOLD
                = "A threashold for type 02 naive caching method. No caching takes"
                + " place is the history of handovers between two cells is less"
                + " than this theshold.";

        public static final String CACHE_REPLACEMENT
                = "Cache replacement policy. This is used only in combination to"
                + " specific caching methods.";
    }
}
