package statistics.handlers.iterative.sc.cmpt6;

import statistics.handlers.BaseHandler;
import statistics.handlers.ICompute5_6;

/**
 *
 * @author Xenofon Vasilakos xvas@aueb.gr
 */
public class UnonymousCompute6 extends BaseHandler implements ICompute5_6 {

    public static class WellKnownTitle {

        public static final WellKnownTitle GOLDEN_RATIO_A = new WellKnownTitle("a",
                "Keeps track of \"a\" in golden ratio procedure for finding optimal time"
                + "\n interval t' during which no replacements of legacy cached objects <LC1, LC2, ...>"
                + "\n are allowed in every small cell SC when there is at least one mobile MU that is expected"
                + "\n to handoff in time t' to SC, and MU requests for <LC1, LC2, ...> from SC."
        ),
                UNONYMOUS = new WellKnownTitle("", ""),
                GOLDEN_RATIO_B = new WellKnownTitle("b",
                        "Keeps track of \"b\" in golden ratio procedure. See description of"
                        + " GOLDEN_RATIO_A for details."
                ),
                GOLDEN_RATIO_C1 = new WellKnownTitle("c1",
                        "This is the golden ratio algorithm result! See description of"
                        + " GOLDEN_RATIO_A for details."
                ),
                GOLDEN_RATIO_C2 = new WellKnownTitle("c2",
                        "Keeps track of \"b\" in golden ratio procedure. See description of"
                        + " GOLDEN_RATIO_A for details."
                ),
                GOLDEN_RATIO_F_A = new WellKnownTitle("f_a", "The gains of EPC-LC for the golden ration time interval \"a\""),
                GOLDEN_RATIO_F_B = new WellKnownTitle("f_b", ""),
                GOLDEN_RATIO_F_C1 = new WellKnownTitle("f_c1", ""),
                GOLDEN_RATIO_F_C2 = new WellKnownTitle("f_c2", ""),
                PC100 = new WellKnownTitle("PC<100", "proactive caching decision taken in distance < 100"),
                PC100_200 = new WellKnownTitle("100<=PC<200", "proactive caching decision taken in distance 100 <= d < 200"),
                PC200_300 = new WellKnownTitle("200<=PC<300", "proactive caching decision taken in distance 200 <= d < 300"),
                PC_DISTANCE = new WellKnownTitle("PC_DIST", "tracks the distance proactive caching decisions taken"),
                HANDOVER_TIME = new WellKnownTitle("HND_TIME", "time for handover"),
                T1_GAINSUM = new WellKnownTitle("T1_GAINSUM", ""),
                T2_GAINSUM = new WellKnownTitle("T2_GAINSUM", ""),
                T3_GAINSUM = new WellKnownTitle("T3_GAINSUM", ""),
                // repalcement time intervals for tracking gain
                G_RPLC_T_0_1 = new WellKnownTitle("G_RPL[00;1)", ""),
                G_RPLC_T_1_2 = new WellKnownTitle("G_RPL[01;2)", ""),
                G_RPLC_T_2_3 = new WellKnownTitle("G_RPL[02;3)", ""),
                G_RPLC_T_3_4 = new WellKnownTitle("G_RPL[03;4)", ""),
                G_RPLC_T_4_5 = new WellKnownTitle("G_RPL[04;5)", ""),
                G_RPLC_T_5_6 = new WellKnownTitle("G_RPL[05;6)", ""),
                G_RPLC_T_6_7 = new WellKnownTitle("G_RPL[06;7)", ""),
                G_RPLC_T_7_8 = new WellKnownTitle("G_RPL[07;8)", ""),
                G_RPLC_T_8_9 = new WellKnownTitle("G_RPL[08;9)", ""),
                G_RPLC_T_9_10 = new WellKnownTitle("G_RPL[09;10)", ""),
                G_RPLC_T_10_11 = new WellKnownTitle("G_RPL[10;11)", ""),
                G_RPLC_T_11_12 = new WellKnownTitle("G_RPL[11;12)", ""),
                G_RPLC_T_12_13 = new WellKnownTitle("G_RPL[12;13)", ""),
                G_RPLC_T_13_14 = new WellKnownTitle("G_RPL[13;14)", ""),
                G_RPLC_T_14_15 = new WellKnownTitle("G_RPL[14;15)", ""),
                G_RPLC_T_15_16 = new WellKnownTitle("G_RPL[15;16)", ""),
                G_RPLC_T_16_17 = new WellKnownTitle("G_RPL[16;17)", ""),
                G_RPLC_T_17_18 = new WellKnownTitle("G_RPL[17;18)", ""),
                G_RPLC_T_18_19 = new WellKnownTitle("G_RPL[18;19)", ""),
                G_RPLC_T_19_20 = new WellKnownTitle("G_RPL[19;20)", ""),
                G_RPLC_T_20_25 = new WellKnownTitle("G_RPL[20;25)", ""),
                G_RPLC_T_25_30 = new WellKnownTitle("G_RPL[25;30)", ""),
                G_RPLC_T_30__ = new WellKnownTitle("G_RPL[30;...)", ""),
                // repalcement time intervals for tracking num of replacements
                N_RPLC_T_0_1 = new WellKnownTitle("N_RPL[00;1)", ""),
                N_RPLC_T_1_2 = new WellKnownTitle("N_RPL[01;2)", ""),
                N_RPLC_T_2_3 = new WellKnownTitle("N_RPL[02;3)", ""),
                N_RPLC_T_3_4 = new WellKnownTitle("N_RPL[03;4)", ""),
                N_RPLC_T_4_5 = new WellKnownTitle("N_RPL[04;5)", ""),
                N_RPLC_T_5_6 = new WellKnownTitle("N_RPL[05;6)", ""),
                N_RPLC_T_6_7 = new WellKnownTitle("N_RPL[06;7)", ""),
                N_RPLC_T_7_8 = new WellKnownTitle("N_RPL[07;8)", ""),
                N_RPLC_T_8_9 = new WellKnownTitle("N_RPL[08;9)", ""),
                N_RPLC_T_9_10 = new WellKnownTitle("N_RPL[09;10)", ""),
                N_RPLC_T_10_11 = new WellKnownTitle("N_RPL[10;11)", ""),
                N_RPLC_T_11_12 = new WellKnownTitle("N_RPL[11;12)", ""),
                N_RPLC_T_12_13 = new WellKnownTitle("N_RPL[12;13)", ""),
                N_RPLC_T_13_14 = new WellKnownTitle("N_RPL[13;14)", ""),
                N_RPLC_T_14_15 = new WellKnownTitle("N_RPL[14;15)", ""),
                N_RPLC_T_15_16 = new WellKnownTitle("N_RPL[15;16)", ""),
                N_RPLC_T_16_17 = new WellKnownTitle("N_RPL[16;17)", ""),
                N_RPLC_T_17_18 = new WellKnownTitle("N_RPL[17;18)", ""),
                N_RPLC_T_18_19 = new WellKnownTitle("N_RPL[18;19)", ""),
                N_RPLC_T_19_20 = new WellKnownTitle("N_RPL[19;20)", ""),
                N_RPLC_T_20_25 = new WellKnownTitle("N_RPL[20;25)", ""),
                N_RPLC_T_25_30 = new WellKnownTitle("N_RPL[25;30)", ""),
                N_RPLC_T_30__ = new WellKnownTitle("N_RPL[30;...)", ""),
                /**
                 * Keeps track of cached requests canceled after a mobile
                 * handoff.
                 */
                CNCLD = new WellKnownTitle("Cncld", "Keeps track of the percentage of the items canceled out of those"
                        + " requested to be canceled if cached (useful if items remain cached due"
                        + " tobeing requested by other mobile requestors too)."),
                /**
                 * Keeps track of the percentage of the items canceled out of
                 * those requested to be canceled if cached = new
                 * WellKnownTitle(useful if items remain cached due to being
                 * requested by other mobile requestors too).
                 */
                CNCLD_PERCENT = new WellKnownTitle("Cncld%", "Keeps track of the percentage of the items "
                        + "canceled out of those requested to be canceled if cached (useful if items remain cached"
                        + " due tobeing requested by other mobile requestors too)."),
                /**
                 * Keeps track of the number of cache replacements regarding
                 * replacement-based methods.
                 */
                ITMS_RPLCD = new WellKnownTitle("ITMS_RPLCD", "Keeps track of the number of cache replacements regarding"
                        + " replacement-based methods."),
                /**
                 * Keeps track of the number of not cached items due to high
                 * price.
                 */
                ITMS_NOT_CACHED_BY_PRICE = new WellKnownTitle("ITMS_NOT_CACHED_BY_PRICE", "Keeps track of the number of cache replacements regarding"),
                ITMS_ALREADY_CACHED = new WellKnownTitle("ITMS_ALREADY_CACHED", "Keeps track of the number of not cached items because they were already present in the cache at time of request"),
                /**
                 * Keeps track of the percentage of cache replacements out of
                 * the cache replacements proposed by the replacement algorithm
                 * used.
                 */
                ITMS_RPLCD_PERCENT = new WellKnownTitle("ITMS_RPLCD%", "Keeps track of the percentage of cache "
                        + "replacements out of the cache replacements proposed by the replacement "
                        + "algorithm used."),
                /**
                 * Keeps track of the popularity demand factor values.
                 */
                W_POP = new WellKnownTitle("W_POP", "Keeps track of the popularity demand factor values."),
                W_POP_NO_ZERO = new WellKnownTitle("W_POP", "Keeps track of non zero the popularity demand factor values."),
                LCL_DMD = new WellKnownTitle("LCL_DMD", "Keeps track of the local demand by connected mobile users."),
                PC_DMD = new WellKnownTitle("PC_DMD", "Keeps track of the proactive caching demand."),
                F_POP = new WellKnownTitle("F_POP", "Keeps track of the popularity."),
                Q = new WellKnownTitle("Q", "Keeps track of the transition probability."),
                Q_NONZERO = new WellKnownTitle("Q_NoZero", "Keeps track of the transition probability."),
                P_G_DIFF = new WellKnownTitle("P_G_DIFF", ""),
                PRICE = new WellKnownTitle("PRICE", ""),
                ASSESS_LC = new WellKnownTitle("ASSESS_LC", ""),
                CACHE_ATTEMPT = new WellKnownTitle("SUCCESS_CACHING%", "Keeps track of the percentage of successfull caching attempts."),
                CACHE_ATTEMPT2 = new WellKnownTitle("SUCCESS2_CACHING%", "Keeps track of the percentage of successfull caching attempts, including those for requests for which items are already cached."),
                OVERLAP_MAXPOP_EPC = new WellKnownTitle("OVERLAP_MAXPOP_EPC", "."),
                OVERLAP_MAXPOP_EPCLC = new WellKnownTitle("OVERLAP_MAXPOP_EPCLC", "."),
                PROB = new WellKnownTitle("PROB", "."),
                PROB2 = new WellKnownTitle("PROB2", "."),
                EVICTNUM1 = new WellKnownTitle("EVICTNUM1", "."),
                EVICTNUM2 = new WellKnownTitle("EVICTNUM2", ".");

        public static final WellKnownTitle[] titleArrRPLC_T_GAIN = {
            G_RPLC_T_0_1,
            G_RPLC_T_1_2,
            G_RPLC_T_2_3,
            G_RPLC_T_3_4,
            G_RPLC_T_4_5,
            G_RPLC_T_5_6,
            G_RPLC_T_6_7,
            G_RPLC_T_7_8,
            G_RPLC_T_8_9,
            G_RPLC_T_9_10,
            G_RPLC_T_10_11,
            G_RPLC_T_11_12,
            G_RPLC_T_12_13,
            G_RPLC_T_13_14,
            G_RPLC_T_14_15,
            G_RPLC_T_15_16,
            G_RPLC_T_16_17,
            G_RPLC_T_17_18,
            G_RPLC_T_18_19,
            G_RPLC_T_19_20,
            G_RPLC_T_20_25,
            G_RPLC_T_25_30,
            G_RPLC_T_30__
        };

        public static final WellKnownTitle[] titleArrRPLC_T_NUM = {
            N_RPLC_T_0_1,
            N_RPLC_T_1_2,
            N_RPLC_T_2_3,
            N_RPLC_T_3_4,
            N_RPLC_T_4_5,
            N_RPLC_T_5_6,
            N_RPLC_T_6_7,
            N_RPLC_T_7_8,
            N_RPLC_T_8_9,
            N_RPLC_T_9_10,
            N_RPLC_T_10_11,
            N_RPLC_T_11_12,
            N_RPLC_T_12_13,
            N_RPLC_T_13_14,
            N_RPLC_T_14_15,
            N_RPLC_T_15_16,
            N_RPLC_T_16_17,
            N_RPLC_T_17_18,
            N_RPLC_T_18_19,
            N_RPLC_T_19_20,
            N_RPLC_T_20_25,
            N_RPLC_T_25_30,
            N_RPLC_T_30__
        };

        private String _ttl;
        private String _description;

        public WellKnownTitle(String title, String description) {
            _ttl = "@" + title;
            _description = description;
        }

        public WellKnownTitle(String title) {
            _ttl = "__" + title;
            _description = "[NOT SET]";
        }

        public WellKnownTitle concat(String name) {
            return new WellKnownTitle(getTtl() + name, _description);
        }

        /**
         * @return the _ttl
         */
        public String getTtl() {
            return _ttl;
        }
    }

    private final WellKnownTitle _title;

    public UnonymousCompute6(WellKnownTitle title) {
        _title = title;
    }
    public UnonymousCompute6(String title) {
        _title = new UnonymousCompute6.WellKnownTitle(title);
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
        return _title.getTtl();
    }

}
