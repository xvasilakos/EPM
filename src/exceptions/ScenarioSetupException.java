package exceptions;

/**
 *
 * @author xvas
 */
public class ScenarioSetupException extends Exception {

   public ScenarioSetupException() {
      super();
   }

   public ScenarioSetupException(String msg) {
      super(msg);
   }

   public ScenarioSetupException(StringBuilder msg) {
      super(msg.toString());
   }

   public ScenarioSetupException(Throwable ex) {
      super(ex);
   }
   public ScenarioSetupException(String msg, Throwable ex) {
      super(ex);
   }
}
