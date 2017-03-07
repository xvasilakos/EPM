package statistics.output.aggregating;

/**
 *
 * @author xvas
 */
public class ScenarioParamBean {

   private String title_full = "";
   private String title_short = "";
   private String value = String.valueOf(Double.MIN_VALUE);
   private String lexicographicalOrder = "";
   private boolean fileNameMember = false;

   /**
    * @return the title_full
    */
   public String getTitle_full() {
      return title_full;
   }

   /**
    * @param title_full the title_full to set
    */
   public void setTitle_full(String title_full) {
      this.title_full = title_full;
   }

   /**
    * @return the title_short
    */
   public String getTitleShort() {
      return title_short;
   }

   /**
    * @param title_short the title_short to set
    */
   public void setTitle_short(String title_short) {
      this.title_short = title_short;
   }

   /**
    * @return the value
    */
   public String getValue() {
      return value;
   }

   /**
    * @param value the value to set
    */
   public void setValue(String value) {
      this.value = value;
   }

   /**
    * @return the lexicographicalOrder
    */
   public String getLexicographicalOrder() {
      return lexicographicalOrder;
   }

   /**
    * @param lexicographicalOrder the lexicographicalOrder to set
    */
   public void setLexicographicalOrder(String lexicographicalOrder) {
      this.lexicographicalOrder = lexicographicalOrder;
   }

   /**
    * @return the fileNameMember
    */
   public boolean isFileNameMember() {
      return fileNameMember;
   }

   /**
    * @param fileNameMember the fileNameMember to set
    */
   public void setFileNameMember(boolean fileNameMember) {
      this.fileNameMember = fileNameMember;
   }

   /**
    * Obj must be also a ScenarioParamBean and toString() representations must return the same string.
    * 
    * @param otherObj
    * @return
    */
   @Override
   public boolean equals(Object otherObj) {
      if (otherObj == null) {
         return false;
      }
      if (getClass() != otherObj.getClass()) {
         return false;
      } 
      
      ScenarioParamBean otherBeam = (ScenarioParamBean) otherObj;
//      ScenarioParamBean other = (ScenarioParamBean) obj;
//      if (!this.title_short.equals(other.title_short)) {
//         return false;
//      }
//
//      // if all above not true, then decide based on the value (which is also in String format)
//      return !this.value.equals(other.value);
      
//      return this.toString().equals(obj.toString());
      return 
            this.getTitle_full().equals(otherBeam.getTitle_full())
            || this.getTitleShort().equals(otherBeam.getTitleShort())
            ;
   }

   /**
    * Based on the toString() representation hash function.
    * @return 
    */
   @Override
   public int hashCode() {
      return toString().hashCode();
   }


   @Override
   public String toString() {
      return this.title_full + ", "
            + this.title_short + ", "
            + this.value;
   }
}
