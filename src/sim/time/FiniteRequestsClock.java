package sim.time;

import app.properties.Simulation;
import java.awt.Toolkit;
import java.util.logging.Logger;

/**
 * Clock used for simulations ending after a finite simulation time threshold has been reached.
 *
 * @author Xenofon Vasilakos xvas@aueb.gr
 */
public class FiniteRequestsClock extends AbstractFiniteClock {

    private int _reqsThreshold;

    public FiniteRequestsClock(sim.run.SimulationBaseRunner sim) {
        super(sim);
        _logger = Logger.getLogger(getClass().getCanonicalName());

        if (sim == sim.NONE) {
            _reqsThreshold = -1;
            return;
        }
    }

    @Override
    protected void defineThresholds() {
        _reqsThreshold = setup.intProperty(Simulation.Clock.MAX_REQ_NUM);
    }

    @Override
    protected void checkSimEnded() throws NormalSimulationEndException {
        reportProgressLcl();
        super.checkSimEnded();
        if (simulation.getWrkloadConsumed() > _reqsThreshold) {
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
        return (int) (10000.0 * simulation.getWrkloadConsumed() / _reqsThreshold) / 100.0;
    }

}
