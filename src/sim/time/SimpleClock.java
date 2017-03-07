package sim.time;

import java.util.logging.Level;
import java.util.logging.Logger;
import utils.CommonFunctions;

/**
 * Clock that simply ticks to keep the simulation time updated and does not end
 * a simulation.
 *
 * @author Xenofon Vasilakos xvas@aueb.gr
 */
public class SimpleClock extends AbstractClock {

    protected static String LOG_SEPARATOR = "* * * * * * * * *\n";

    protected int gcPeriod;

    public SimpleClock(sim.run.SimulationBaseRunner sim) {
        super(sim);
        _logger = Logger.getLogger(getClass().getCanonicalName());

        if (sim == sim.NONE) {
            gcPeriod = -1;
            return;
        }

        try {
            gcPeriod = getSim().getScenario().intProperty(app.properties.Simulation.Clock.GC_PERIOD);
            //</editor-fold>
        } catch (RuntimeException ex) {
            // ignore
            gcPeriod = Integer.MAX_VALUE;
        }
    }

    @Override
    protected void checkSimEnded() throws NormalSimulationEndException {
        tryCallGC();
    }

    protected void tryCallGC() {
//         MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        double free = Runtime.getRuntime().freeMemory(); // / Runtime.getRuntime().maxMemory();
        if (simTime() % gcPeriod == 0 || free < 100000000) {
            System.gc();
        }
    }

    @Override
    protected void reportProgressLcl() {
        if (isPeriodicLoging()) {
            logMemoryStatus();
        }
    }

    protected void logMemoryStatus() {
        _logger.log(Level.INFO,
                LOG_SEPARATOR
                + "Memory status in bytes:"
                + "\n\t- total:{0}"
                + "\n\t- max:\t{1}"
                + "\n\t- free:\t{2}%",
                new Object[]{
                    Runtime.getRuntime().totalMemory(),
                    Runtime.getRuntime().maxMemory(),
                    CommonFunctions.roundNumber(
                            100.0 * Runtime.getRuntime().freeMemory()
                            / Runtime.getRuntime().maxMemory(), 2)
                });
    }

}
