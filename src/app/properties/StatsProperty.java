package app.properties;

/**
 *
 * @author Xenofon Vasilakos xvas@aueb.gr
 */
public enum StatsProperty implements IProperty {

   STATS__CONF_INTERVAL_Z("stats.conf_interval_z"),
   STATS__MIN_TIME("stats.min_time"),
   STATS__PRINT__MEAN("stats.print.mean"),
   STATS__PRINT__STDDEV("stats.print.stddev"),
   STATS__PRINT__AGGREGATES("stats.print.aggregates"),
   /**
    * Set to true for printing transient (per time) statistics csv file.
    */
   STATS__PRINT__TRANSIENT("stats.print.transient"),
   STATS__PRINT__TRANSIENT__FLUSH_PERIOD("stats.print.transient.flush_period"),
   /**
    * Averaging period.
    */
   STATS__AGGREGATES__AVG_PERIOD("stats.aggregates.avg_period"),
   STATS__OUTPUTDIR("stats.outputdir"),
   //////////////
   HANDLERS__ITERATIVE__MU__CMPT1("statistics.handlers.iterative.mu.cmpt1"),
   //////////////
   PERFORMANCE__GAINS("statistics.performance.gains"),
   PERFORMANCE__OTHER("statistics.performance.other"),
   //////////////
   HANLDERS__ITERATIVE__SC__CMPT3("statistics.handlers.iterative.sc.cmpt3"),
   HANLDERS__ITERATIVE__SC__CMPT4__NO_POLICY("statistics.handlers.iterative.sc.cmpt4.no_policy"),
   HANLDERS__ITERATIVE__SC__CMPT4("statistics.handlers.iterative.sc.cmpt4"),
   HANLDERS__ITERATIVE__SC__CMPT5("statistics.handlers.iterative.sc.cmpt5"),
   HANDLERS__ITERATIVE__SC__CMPT6("statistics.handlers.iterative.sc.cmpt6"),
   //////////////
   HANDLERS__FIXED_SC__MONITOR_SCS("statistics.handlers.fixed_sc.monitor_scs"),
   HANDLERS__FIXED_SC__CMPT0("statistics.handlers.fixed_sc.cmpt0"),
   HANDLERS__FIXED_SC__CMPT0__NO_POLICIES("statistics.handlers.fixed_sc.cmpt0.no_policy"),
   //////////////

   STATS__ROUNDING_DECIMAL("stats.rounding_decimal"),;
   private String _propName;

   private StatsProperty(String _propTitle) {
      _propName = _propTitle;
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
      return "tooltip TBD"; // TODO to define per different enum value
   }
}
