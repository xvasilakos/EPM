package app.properties;

/**
 *
 * @author Xenofon Vasilakos xvas@aueb.gr
 */
public enum Space implements IProperty {

    AREA__X("space.area.x", "tooltip TBD"),
    AREA__Y("space.area.y", "tooltip TBD"),
    //////////////////// SMALLER CELLS  ////////////////////////////
    SC__NUM("space.sc.num", "tooltip TBD"),
    SC__RADIUS__MEAN("space.sc.radious.mean", "tooltip TBD"),
    SC__RADIUS__STDEV("space.sc.radious.stdev", "tooltip TBD"),
    SC__WARMUP_PERIOD("space.sc.warmup_period", "Warm up period for neighborhood discovery (transition probabilities, residence and transition historical times, etc)"),
    SC__BUFFER__SIZE("space.sc.buffer.size", "tooltip TBD"),
    SC__DMD__TRACE__STATIONARY_REQUESTS__RATE("space.sc.dmd.trace.stationary_requests.rate", "tooltip TBD"),
    SC__DMD__TRACE__STATIONARY_REQUESTS__STDEV("space.sc.dmd.trace.stationary_requests.stdev", "tooltip TBD"),
    SC__DMD__TRACE__STATIONARY_REQUESTS__REQ2CACHE("space.sc.dmd.trace.stationary_requests.req2cache", "tooltip TBD"),
    ITEM__SIZE("space.item.size", "tooltip TBD"),
    ITEM__RND_ID_RANGE("space.item.rnd_id_range", "tooltip TBD"),
    ITEM__POP_CMPT("space.item.pop_cmpt",
            "Defines how to compute popularity of items. "
            + "This feature is used for caching method decisions "
            + "which use the popularity either to cache or to replace a cached item."),
    SC__INIT("space.sc.init", "tooltip TBD"),
    SC__TRACE_PATH("space.sc.trace_path", "tooltip TBD"),
    SC__TRACE_METADATA_PATH("space.sc.trace_metadata_path", "tooltip TBD"),
    SC__COVERAGE("space.sc.coverage", "tooltip TBD"),
    SC__NEIGHBORHOOD("space.sc.neighborhood", "Defines how the neighborhood of each cell is build"),
    SC__NEIGHBORHOOD__ALLOW_SELF("space.sc.neighborhood.allow_self", "Allows small cells to have their own self as a neighbor, e.g. due to mobile looping back within the area."),
    SC__INIT_DURATION__HANDOVER("space.sc.init_duration.handoff",
            "Defines an initial value for the exponential smoothing "
            + "calculation of the average duration of mobile handoff "
            + "period per small cell"),
    SC__INIT_DURATION__RESIDENCE("space.sc.init_duration.residence",
            "Defines an initial value for the exponential smoothing "
            + "calculation of the average duration of mobile residence "
            + "period per small cell."
    ),
    //////////////////////  RATE  //////////////////////////
    SC__HANDOFF_PROBABILITY__STDEV("space.sc.handoff_probability.stdev", "tooltip TBD"),
    /////////////////////////// MOBILE USERS ///////////////////////
    MOBILITY_MODEL("space.sc.mobility_model", "tooltip TBD"),
    MU__TRACE("space.mu.trace", "tooltip TBD"),
    MU__TRACE__META("space.mu.trace.meta", "tooltip TBD"),
    MU__TRANSITION_PROBABILITIES__MATRIX("space.mu.transition_probabilities.matrix", "tooltip TBD"),
    MU__TRANSITION_DECISIONS("space.mu.transition_decisions", "Defines how transition probabilities are applied. "
            + "E.g, with \"per_mu\" each mobile user moves to any of the "
            + "directions according to a random choice based on "
            + "the corresponding transition probabilties per direction."),
    MU__CLONEFACTOR("space.mu.clonefactor", "How many clones for each mobile. Used for mobility traces only."),
    ///////////////////////////////////////////////////////////////////
    MU__GROUP__SIZE("space.mu.group.size", "tooltip TBD"),
    MU__SHUFFLE("space.mu.shuffle", "tooltip TBD"),
    MU__GROUP__INIT__POS("space.mu.group.init.pos", "tooltip TBD"),
    MU__GROUP__RESET__POS("space.mu.group.reset.pos", "tooltip TBD"),
    MU__GROUP__RESIDENCE_DELAY("space.mu.group.residence_delay", "tooltip TBD"),
    MU__GROUP__HANDOVER_DELAY("space.mu.group.handover_delay", "tooltip TBD"),
    MU__MOBILITYACCURACY("space.mu.mobilityaccuracy", "tooltip TBD"),
   
    //////////
    MU__GROUP__VELOCITY__MEAN("space.mu.group.velocity.mean", "tooltip TBD"),
    MU__GROUP__VELOCITY__STDEV("space.mu.group.velocity.stdev", "tooltip TBD"),
    MU__GROUP__PROHIBIT_ITEMS_RPLC("space.mu.group.prohibit_rplc_minDist", ""
            + "Replacing items is prohibited when at least one of the "
            + "requesting mobiles is closer than this limmit distance."),
    //////////
    MU__INIT__CONNECT__SC("space.mu.init.connect.sc", "tooltip TBD"),//@todo consider to delete due to possible inconsistences
    MU__ISSOFT("space.mu.isSoft", "tooltip TBD"),//@todo consider to delete due to possible inconsistences
    MU__INIT__CACHE_DECISIONS("space.mu.init.cache_decisions", "tooltip TBD"),//@todo consider to delete due to possible inconsistences
    MU__DMD__TRACE__DOCS__CDN_SERVED("space.mu.dmd.trace.docs.cdn_served", "tooltip TBD"),
    MU__DMD__TRACE__DOCS_PATH("space.mu.dmd.trace.docs_path", "The percentage of the top max popular items in a trace."),
    MU__DMD__TRACE__WORKLOAD_PATH("space.mu.dmd.trace.workload_path", "tooltip TBD"),
    MU__DMD__TRACE__FILES("space.mu.dmd.trace.files", "tooltip TBD"),
    MU__DMD__TRACE__LIMIT("space.mu.dmd.trace.limit", "tooltip TBD"),
    MU__DMD__TRACE__OVERRIDE_SIZE("space.mu.dmd.trace.override_size", "tooltip TBD"),
    MU__DMD__TRACE__REQUESTS_PER_USER("space.mu.dmd.trace.requests_per_user", "tooltip TBD"),
    MU__DMD__TRACE__SHUFFLE_WORKLOAD_TIMES("space.mu.dmd.trace.shuffle_workload_times", "tooltip TBD"),
    MU__DMD__TRACE__RAND_INIT("space.mu.dmd.trace.rand_init", "tooltip TBD"),
    //////////
    SC__CONNECTION_POLICY("space.sc.connection_policy", "tooltip TBD"),
    MC__CONNECTION_POLICY("space.mc.connection_policy", "tooltip TBD"),
    MU__INIT__ROAM_START("space.mu.group.init.roam_start", "tooltip TBD");

    private final String _propName;
    private final String _toolTip;

    private Space(String propertyName, String toolTip) {
        _propName = propertyName;
        _toolTip = toolTip;
    }

    @Override
    public String toString() {
        return _propName + ": " + toolTip();
    }

    @Override
    public String propertyName() {
        return _propName;
    }

    @Override
    public String toolTip() {
        return _propName;
    }

}
