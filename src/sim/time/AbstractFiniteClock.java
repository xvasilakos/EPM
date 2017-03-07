
package sim.time;

import exceptions.NotIntiliazedException;
import java.util.logging.Level;
import sim.ScenariosFactory;
import sim.run.SimulationBaseRunner;

/**
 * Clock used for simulations ending after a finite threshold has been reached.
 *
 * @author Xenofon Vasilakos xvas@aueb.gr
 */
public abstract class AbstractFiniteClock extends SimpleClock {

    /**
     * Percentage of simulations batch progress with two decimals.
     *
     * @return
     * @throws exceptions.NotIntiliazedException
     */
    public static double globalProgressPercent() throws NotIntiliazedException {
        double completedScenarios = (double) ScenariosFactory.completedScenariosNum() / ScenariosFactory.initialScenariosNum();
        double globalProgressPercent = ((int) (10000 * completedScenarios)) / 100.0;
        return globalProgressPercent;
    }

    public AbstractFiniteClock(SimulationBaseRunner sim) {
        super(sim);
        defineThresholds();
    }

    abstract protected void defineThresholds();

    @Override
    protected void reportProgressLcl() {
        if (isPeriodicLoging()) {
            try {
                logMemoryStatus();
                String msg = LOG_SEPARATOR
                        + "Simulation {0} time {1}:\n" + "\t- Local run time progress {2}%\n"
                        + "\t- Local real time elapsed " + realTimeElapsedStr() + "\n"
                        + "\t- Simulation expected to finish in {3}\n"
                        + (_sim.usesTraceOfRequests() ? LOG_SEPARATOR
                                + "\t- Loaded records from trace: {4} ({5}%)\n" : "")
                        + LOG_SEPARATOR + "Parallel Running simulations {6}\n"
                        + "\t- Global progress {7}%\n"
                        + "\t\t- Global simulations time elapsed {8}\n\n" + "\t\t- Simulations batch expected to finish in {9}\n\n";
                _logger.log(
                        Level.INFO, msg,
                        new Object[]{
                            /*0*/simID(),
                            /*1*/ simTime(),
                            /*2*/ progressPercent(),
                            /*3*/ realTimeStr(100.0 * realTimeElapsedL() / progressPercent() - realTimeElapsedL()),
                            /*4*/ _sim.getWrkloadConsumed(),
                            /*5*/ _sim.getWrkloadConsumedPercent(),
                            /*6*/ SimulationBaseRunner.getRunningSimulations(),
                            /*7*/ globalProgressPercent(),
                            /*8*/ realGlobalTimeElapsedStr(),
                            /*9*/ globalTimeFinishExpectation()});
            } catch (NotIntiliazedException ex) {
                _logger.log(Level.SEVERE, null, ex);
            }
        }
    }

    /**
     * Percentage of progress with respect to the threshold(s) used
     *
     * @return
     */
    abstract public double progressPercent();

    @Override
    public int tick() throws NormalSimulationEndException {
        return super.tick();
    }


    public String globalTimeFinishExpectation() throws NotIntiliazedException {
        double globalProgressPercent = globalProgressPercent();
        if (globalProgressPercent == 0.0) {
            return "[NOT ENOUGH DATA YET]";
        }
        long expectation = (long) (100.0 * realGlobalTimeElapsedL() / globalProgressPercent - realGlobalTimeElapsedL());
        return realTimeStr(expectation);
    }

}
