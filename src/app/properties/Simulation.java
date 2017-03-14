package app.properties;

import app.properties.valid.Values;

/**
 * @author Xenofon Vasilakos xvas@aueb.gr
 */
public enum Simulation implements IProperty {

    RUN__CLASS("simulation.run.class", TooltipMsgs.RUN__CLASS),
    SEED("simulation.seed", TooltipMsgs.SEED),
    PROGRESS_UPDATE("simulation.progress_update", TooltipMsgs.PROGRESS_UPDATE),
    DecimalFormat("simulation.DecimalFormat", TooltipMsgs.PROGRESS_UPDATE);

    private final String _propertyName;
    private final String _tooltip;

    private Simulation(String _propTitle, String tooltip) {
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

    public enum Clock implements IProperty {

        TYPE("simulation.clock.type", TooltipMsgs.CLOCK__TYPE),
        MAX_TIME("simulation.clock.max_time", TooltipMsgs.CLOCK__MAX_TIME),
        INIT_TIME("simulation.clock.init_time", TooltipMsgs.CLOCK__INIT_TIME),
        MAX_REQ_NUM("simulation.clock.max_req_num", ""),
        GC_PERIOD("simulation.clock.gc_period", TooltipMsgs.CLOCK__GC_PERIOD);

        private final String _propertyName;
        private final String _tooltip;

        private Clock(String _propTitle, String tooltip) {
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
    }

}

class TooltipMsgs {

    public static final String RUN__CLASS = "The class to be used for running the simulation thread(s).";
    public static final String SEED
            = "Seed for producing random numbers during simulation run."
            + " Multiple values for this property lead to multiple repeatitions"
            + " of each scenario. Valid values: any interger";
    public static final String CLOCK__TYPE
            = "Type of clock to be used";
    public static final String CLOCK__MAX_TIME
            = "Simulation time at which simulation is completed.";
    public static final String CLOCK__INIT_TIME
            = "Intial value for simulation time. Useful for mobility traces using custom time.";

    public static final String INCLUDE
            = "Path to another ini file to be included. Note that the last property-value entries"
            + " read override previously read entries of the same property ";
    public static final String PROGRESS_UPDATE
            = "Percentage of simulation progress to be used through some computer-human"
            + " interaction interface (console or GUI)";
    public static final String CLOCK__GC_PERIOD
            = "Garbage Collection Period (in simulation time units)."
            + " Setting to 0 implies invocation after completing every simulation."
            + " Negative values disable the use of System.gc(), which leaves the JVM"
            + " decide upon the right gc invocation time on its own.";
}
