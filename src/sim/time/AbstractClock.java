package sim.time;

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
     * of the whole _sim batch
     */
    public final static long REAL_GLOBAL_TIME_BEGIN = System.currentTimeMillis();

    protected int _simTime;
    /**
     * start simTime of this instance
     */
    protected final long _realTimeStart;
    protected SimulationBaseRunner _sim;
    protected Scenario _setup;
    private long _lastPeriodicLoging;
    protected Logger _logger;

    public AbstractClock(SimulationBaseRunner sim) {
        this._lastPeriodicLoging = System.currentTimeMillis();
        this._realTimeStart = System.currentTimeMillis();
        this._simTime = 0;
        this._sim = sim;
        this._setup = sim.getScenario();

    }

    abstract protected void reportProgressLcl();

    protected boolean isPeriodicLoging() {
        int time = simTime();
        long elapsed = System.currentTimeMillis() - _lastPeriodicLoging;
        boolean shouldLog
                = elapsed > 30000
                || time % getSim().loggingSimTimePeriod() == 0;

        if (shouldLog) {
            _lastPeriodicLoging = System.currentTimeMillis();
        }

        return shouldLog;
    }

    protected abstract void checkSimEnded() throws NormalSimulationEndException;

    /**
     * @return the simTime after ticking
     * @throws NormalSimulationEndException when the _sim has ended according to
     * the SimulationEndChecker used.
     */
    public int tick() throws NormalSimulationEndException {
        checkSimEnded();
        return ++_simTime;
    }

    @Override
    public final int simTime() {
        return _simTime;
    }

    @Override
    public final int simID() {
        return getSim().getID();
    }

    @Override
    public final SimulationBaseRunner getSim() {
        return _sim;
    }

    @Override
    public String simTimeStr() {
        return String.valueOf(_simTime);
    }

    @Override
    public final CellRegistry simCellRegistry() {
        return getSim().getCellRegistry();
    }

    public long realTimeElapsedL() {
        return System.currentTimeMillis() - _realTimeStart;
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
            return this._simTime - t._simTime;
        }
        return this.getClass().getCanonicalName().compareTo(t.getClass().getCanonicalName());
    }

}
