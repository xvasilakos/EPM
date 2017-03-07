package app.properties.valid;

/**
 * Accepted values for properties
 *
 * @author Xenofon Vasilakos xvas@aueb.gr
 */
public final class Values {

    public static final String TRUE = "true";
    public static final String FALSE = "false";
    ////////////////////////////////////////////////////////////////////////////////////
    /**
     * Prefix for EPC method variations.
     */
    public static final String CACHING__EPC__ = caching.incremental.EPC.instance().getClass().getPackage().getName();
    /**
     * Standard (original) version of the Efficient Proactive Caching method.
     */
    public static final String CACHING__EPC__CNC = caching.incremental.EPC.instance().toString();
    public static final String CACHING__EPC__LGCPOP__CNC = caching.incremental.EMPC.instance().toString();

    /**
     * Type 01 of computation method for popularity of items based on temporal
     * locality of demand.
     */
    public static final String POP__TYPE01 = "pop_type01";

    /**
     * Type 02 of computation method for popularity of items based on temporal
     * locality of demand.
     */
    public static final String POP__TYPE02 = "pop_type02";
    /**
     * Combines types 2 and "3.global"
     */
    public static final String POP__TYPE23 = "pop_type23";

    /**
     * Type 03 of computation method for popularity of items based on local
     * demand. This type uses the global popularity of items which are loaded by
     * a trace of requests.
     */
    public static final String POP__TYPE03__GLOBAL = "pop_type03_global_pop";

    /**
     * This is the name prefix of the supported Naive caching variations.
     */
    public static final String CACHING__NAIVE__ = "caching.naive.";
    public static final String CACHING__NAIVE__TYPE01 = CACHING__NAIVE__ + "type01";
    public static final String CACHING__NAIVE__TYPE02 = CACHING__NAIVE__ + "type02";
    public static final String CACHING__NAIVE__TYPE03 = CACHING__NAIVE__ + "type03";
    ////////////////////////////////////////////////////////////////////////////////////
    public static final String NEVER = "never";
    public static final String ALWAYS = "always";
    public static final String ALL = "all";
    public static final String ALL_PLUS_SELF = "all+self";
    public static final String DISCOVER = "discover";
    public static final String UPON_CREATION = "upon_creation";
    ///////////////////////////////////////////////////////////////////////////////////
    public static final String NULL = "NULL";
    public static final String NONE = "NONE";
    public static final String UNDEFINED = "UNDEFINED";
    public static final String LIST_SEPARATOR = ",";
    public static final String SETUP_SEPARATOR = ";";
    ///////////////////////////////////////////////////////////////////////////////////
    public static final String RANDOM_UNIFORM = "random_uniform";
    public static final String RANDOM = "random";
    public static final String RANDOM_Y = "random_y";
    public static final String RANDOM_X = "random_x";
    public static final String LOOP_PLUS_NO_RESET = "loop+no_reset";
    public static final String RESET="reset";
    public static final String RANDOM_IN_RANGE = "random_in_range";
    public static final String INIT = "init";
    /////////////////////////////// handoff policies //////////////////////////////////
    public static final String OUT_OF_RANGE = "out_of_range";
    public static final String CLOSEST_IN_RANGE = "closest_in_range";
    public static final String MAX_CACHED_EPC_STD = "max_cached_epc_std";

    public static final String CLOSEST__RANGE = "Closest_Range";
    public static final String CR = "CR";

    public static final String CACHED_CONTENT = "Cached_Content";
    public static final String CC = "CC";

    ///////////////////////////////////////////////////////////////////////////////////
    public static final String TRACE = "trace";
    public static final String FIXED = "fixed";
    public static final String PROPAGATION_DELAY__PLUS__MC_WIRELESS = "propagation_delay+mc_wireless";
    public static final String MONETARY = "monetary";
    public static final String DYNAMIC = "dynamic";
    public static final String DYNAMIC__TYPE_01 = "dynamic.type_01";
    public static final String DYNAMIC__TYPE_02 = "dynamic.type_02";
    public static final String DEFAULT = "default";
    ////////////////////////////////////////////////////////////////////////////////////
    public static final String BY_MAX_TIME = "by_max_time";
    ////////////////////////////////////////////////////////////////////////////////////
    public static final String LOCATION = "location";
    public static final String LOCATION__PLUS__GROUP = "location+group";
    public static final String PER_MU__PLUS__CENTRIFY = "per_mu+centrify";
    public static final String PER_MU__PLUS__CENTRIFY__PLUS__CHANGE_DIRECTION = "per_mu+centrify+change_direction";
    ////////////////////////////////////////////////////////////////////////////////////
    public static final String PER_CELL_NEIGHBOURHOOD = "per_cell_neighbourhood";
    public static final String PER_MU__PLUS__RANDPROB = "per_mu+randprob";
    public static final String PER_MU = "per_mu";
    public static final String INCLUDE = "include";
    public static String SEED_RPT = "seed_rpt";

    public enum StartPosition {

        random, center, west, east, south, north, south_west, south_east, north_west, north_east;
    }

}
