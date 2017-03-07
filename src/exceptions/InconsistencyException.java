
package exceptions;

/**
 *
 * @author xvas
 */
public class InconsistencyException extends RuntimeException {

   public InconsistencyException() {
      super();
   }

   public InconsistencyException(String msg, Object... args) {
      super(String.format(msg, args));
   }

   public InconsistencyException(StringBuilder msg) {
      this(msg.toString());
   }

   public InconsistencyException(Throwable ex) {
      super(ex);
   }
   public InconsistencyException(String msg, Throwable ex) {
      super(msg, ex);
   }
}
