package statistics.output;

import app.arguments.MainArguments;

/**
 *
 * @author Xenofon Vasilakos xvas@aueb.gr
 */
public class Constants {

    public final static String IGNORE = "ignore";
    public final static String TIME = "TIME";
    public final static String MODE__TRANSIENT_STATS = "mode.transient_stats";
    public final static String MODE__AGGREGATES_STATS = "mode.aggregates_stats";
    public static final int AGGR_RESULTS_FILE__DEFAULT_TIME = -1000;
    public static final String REPEAT_SHORT_TITLE = "Rpt";
    public final static String IGNORE_REPEAT = "repeat";
    //<editor-fold defaultstate="collapsed" desc="CSV File Header annotations">
    public final static String OPEN_BEGIN = "<";
    public final static String OPEN_END = "</";
    public final static String CLOSE = ">";
    //<editor-fold defaultstate="collapsed" desc="SCENARIO_SETUP">
    public final static String SCENARIO_SETUP = "Scenario Setup";
    public final static String SCENARIO_SETUP_BEGIN = OPEN_BEGIN + SCENARIO_SETUP + CLOSE;
    public final static String SCENARIO_SETUP_END = OPEN_END + SCENARIO_SETUP + CLOSE;
    //</editor-fold>
    //<editor-fold defaultstate="collapsed" desc="REPEAT_DETAILS">
    public final static String REPEAT_DETAILS = "Repeat Details";
    public final static String REPEAT_DETAILS_BEGIN = OPEN_BEGIN + REPEAT_DETAILS + CLOSE;
    public final static String REPEAT_DETAILS_END = OPEN_END + REPEAT_DETAILS + CLOSE;
    //</editor-fold>
    //</editor-fold>
    public final static String DEFAULT_STATS_PATH
            = MainArguments.Defaults.FILES_PATH + "/results";
}
