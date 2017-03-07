package statistics;

/**
 *
 * @author xvas
 */
public class StatisticException extends Exception {

   public StatisticException() {
      super();
   }

   public StatisticException(Throwable th) {
      super(th);
   }

   public StatisticException(String msg, Throwable th) {
      super(msg, th);
   }

   public StatisticException(String msg) {
      super(msg);
   }

}
