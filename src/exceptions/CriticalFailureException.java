
package exceptions;

/**
 *
 * @author xvas
 */
public class CriticalFailureException extends RuntimeException {

   public CriticalFailureException() {
      super();
   }

   public CriticalFailureException(String msg) {
      super(msg);
   }

   public CriticalFailureException(StringBuilder msg) {
      this(msg.toString());
   }

   public CriticalFailureException(Throwable ex) {
      super(ex);
   }
   public CriticalFailureException(String msg, Throwable ex) {
      super(msg, ex);
   }
}
