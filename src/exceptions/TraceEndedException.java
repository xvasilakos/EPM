
package exceptions;

/**
 *
 * @author xvas
 */
public class TraceEndedException extends Exception{

   public TraceEndedException() {
      super();
   }

   public TraceEndedException(Throwable ex) {
      super(ex);
   }
   public TraceEndedException(String str, Throwable ex) {
      super(str, ex);
   }

   public TraceEndedException(String str) {
      super(str);
   }

   public TraceEndedException(StringBuilder strBuild) {
      super(strBuild.toString());
   }
   
}
