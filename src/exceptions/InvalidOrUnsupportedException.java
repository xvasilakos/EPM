
package exceptions;

/**
 *
 * @author xvas
 */
public class InvalidOrUnsupportedException extends Exception{

   public InvalidOrUnsupportedException() {
      super();
   }

   public InvalidOrUnsupportedException(Throwable ex) {
      super(ex);
   }
   public InvalidOrUnsupportedException(String str, Throwable ex) {
      super(str, ex);
   }

   public InvalidOrUnsupportedException(String str) {
      super(str);
   }

   public InvalidOrUnsupportedException(StringBuilder strBuild) {
      super(strBuild.toString());
   }
   
}
