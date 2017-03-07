package sim.time;

import app.properties.Simulation;
import java.awt.Toolkit;
import java.util.logging.Logger;

/**
 * Clock used for simulations ending after a finite simulation time threshold
 * has been reached.
 *
 * @author Xenofon Vasilakos xvas@aueb.gr
 */
public class FiniteTimeClock extends AbstractFiniteClock {

    private int _timeThreshold;

    public FiniteTimeClock(sim.run.SimulationBaseRunner sim) {
        super(sim);
        _logger = Logger.getLogger(getClass().getCanonicalName());

        if (sim == sim.NONE) {
            _timeThreshold = -1;
            return;
        }

    }

    @Override
    protected void defineThresholds() {
        _timeThreshold = _setup.intProperty(Simulation.Clock.MAX_TIME);
    }

    @Override
    protected void checkSimEnded() throws NormalSimulationEndException {
        reportProgressLcl();
        super.checkSimEnded();
        if (simTime() > _timeThreshold) {
            try {
                Toolkit.getDefaultToolkit().beep();
            } catch (Exception e) {
                //ignore
            } finally {
                throw new NormalSimulationEndException("Simulation time expired.");
            }
        }
    }

    @Override
    /**
     * Percentage of progress with respect to the time threshold
     *
     * @return
     */
    public double progressPercent() {
        return (int) (10000.0 * simTime() / _timeThreshold) / 100.0;
    }

}
