package sim.space.users.mobile;

/**
 *
 * @author xvas
 */
/**
 * Class embodying enums used for connection and connectivity related
 * operations.
 */
public class Connection {

    /**
     * Supported connection statuses.
     */
    public enum Status {
        connected, disconected
    }

    /**
     * Supported connection operations.
     */
    public enum Operation {

        /**
         * Operation for connecting to a macro cell
         */
        /**
         * Operation for connecting to a macro cell
         */
        connectMC,
        /**
         * Operation for disconnecting from a macro cell
         */
        disconnectMC,
        /**
         * Operation for connecting to a small cell
         */
        connectSC,
        /**
         * Operation for disconnecting to a small cell
         */
        disconnectSC
    }
}
