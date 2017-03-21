package sim.time;

import static app.properties.Simulation.Clock.INIT_TIME;
import exceptions.InconsistencyException;
import java.util.logging.Logger;
import sim.ISimulationMember;
import sim.Scenario;
import sim.run.SimulationBaseRunner;
import sim.space.cell.CellRegistry;

/**
 * @author Xenofon Vasilakos xvas@aueb.gr
 */
public abstract class AbstractClock implements ISimulationMember, Comparable<AbstractClock> {

    /**
     * this is the simTime since the class was loaded, i.e., from the beginning
     * of the whole simulation batch
     */
    public final static long REAL_GLOBAL_TIME_BEGIN = System.currentTimeMillis();

    protected int simTime;
    /**
     * start simTime of this instance
     */
    protected final long realTimeStart;
    protected SimulationBaseRunner simulation;
    protected Scenario setup;
    private long lastPeriodicLoging;
    protected Logger _logger;

    public AbstractClock(SimulationBaseRunner sim) {
        this.lastPeriodicLoging = System.currentTimeMillis();
        this.realTimeStart = System.currentTimeMillis();
        this.simulation = sim;
        this.setup = sim.getScenario();

        this.simTime = setup.intProperty(INIT_TIME);
    }

    abstract protected void reportProgressLcl();

    protected boolean isPeriodicLoging() {
        int time = simTime();
        long elapsed = System.currentTimeMillis() - lastPeriodicLoging;
        boolean shouldLog
                = elapsed > 30000
                || time % getSimulation().loggingSimTimePeriod() == 0;

        if (shouldLog) {
            lastPeriodicLoging = System.currentTimeMillis();
        }

        return shouldLog;
    }

    protected abstract void checkSimEnded() throws NormalSimulationEndException;

    /**
     * @return the simTime after ticking
     * @throws NormalSimulationEndException when the simulation has ended
     * according to the SimulationEndChecker used.
     */
    public int tick() throws NormalSimulationEndException {
        checkSimEnded();
        return ++simTime;
    }

    /**
     * Proceeds the time to a given time value. This is a useful option for some
     * mobility traces.
     *
     * @param t
     * @return
     * @throws NormalSimulationEndException
     * @throws InconsistencyException In case there is an effort to set the time
     * to a value lower than the current clock's time.
     */
    public int tick(int t) throws NormalSimulationEndException, InconsistencyException {
        if (t < simTime) {
            throw new InconsistencyException(
                    "Effort to set simulation time to value "
                    + "\""
                    + t
                    + "\""
                    + ", which is less than current time "
                    + "\""
                    + simTime
                    + "\""
            );
        }
        checkSimEnded();
        return simTime = t;
    }

    /**
     * Proceeds the time to a given time value. This is a useful option for some
     * mobility traces. Allows to set time to a value lower than the current clock's
     * time.
     *
     * @param t
     * @return
     * @throws NormalSimulationEndException
     */
    public int tickAllowBackwardsTime(int t) throws NormalSimulationEndException {
        checkSimEnded();
        return simTime = t;
    }

    @Override
    public final int simTime() {
        return simTime;
    }

    @Override
    public final int simID() {
        return getSimulation().getID();
    }

    @Override
    public final SimulationBaseRunner getSimulation() {
        return simulation;
    }

    @Override
    public String simTimeStr() {
        return String.valueOf(simTime);
    }

    @Override
    public final CellRegistry simCellRegistry() {
        return getSimulation().getCellRegistry();
    }

    public long realTimeElapsedL() {
        return System.currentTimeMillis() - realTimeStart;
    }

    public String realTimeElapsedStr() {
        return AbstractClock.realTimeStr(realTimeElapsedL());
    }

    public static long realGlobalTimeElapsedL() {
        return System.currentTimeMillis() - REAL_GLOBAL_TIME_BEGIN;
    }

    public static String realGlobalTimeElapsedStr() {
        return AbstractClock.realTimeStr(realGlobalTimeElapsedL());
    }

    public static String realTimeStr(double elapsedMillis) {
        return AbstractClock.realTimeStr((long) elapsedMillis);
    }

    public static String realTimeStr(long elapsedMillis) {
        if (elapsedMillis > 19 * 24 * 60 * 60 * 1000) {
            return " > 19 days!";
        }

        long second = (elapsedMillis / 1000) % 60;
        long minute = (elapsedMillis / (60 * 1000)) % 60;
        long hour = (elapsedMillis / (60 * 60 * 1000)) % 24;
        long days = (elapsedMillis / (24 * 60 * 60 * 1000)) % 30;
        long months = (elapsedMillis / (30 * 24 * 60 * 60 * 1000));

        StringBuilder timeFormatted = new StringBuilder(15);

        if (days != 0) {
            timeFormatted.append(String.format("%d days and ", days));
        }
        if (months != 0) {
            timeFormatted.append(String.format("%.2 months and ", months));
        }
        timeFormatted.append(String.format("%02d:%02d:%02d", hour, minute, second));
        return timeFormatted.toString();
    }

    @Override
    public int compareTo(AbstractClock t) {
        if (this.getClass().equals(t.getClass())) {
            return this.simTime - t.simTime;
        }
        return this.getClass().getCanonicalName().compareTo(t.getClass().getCanonicalName());
    }

}
