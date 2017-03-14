package exceptions;

/**
 *
 * @author xvas
 */
public class WrongOrImproperArgumentException extends Exception{

    public WrongOrImproperArgumentException() {
        super();
    }

    public WrongOrImproperArgumentException(String msg) {
        super(msg);
    }

    public WrongOrImproperArgumentException(StringBuilder msg) {
        super(msg.toString());
    }

    public WrongOrImproperArgumentException(Throwable ex) {
        super(ex);
    }
    public WrongOrImproperArgumentException(Throwable ex, String msg) {
        super(msg, ex);
    }
}
