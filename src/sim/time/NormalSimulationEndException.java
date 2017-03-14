package sim.time;

/**
 *
 * @author Xenofon Vasilakos xvas@aueb.gr
 */
public class NormalSimulationEndException extends Exception {

    public NormalSimulationEndException(Throwable cause) {
        super(cause);
    }

    public NormalSimulationEndException(String msg) {
        super(msg);
    }

    public NormalSimulationEndException() {
        super();
    }

}
