package app.properties;

/**
 *
 * @author Xenofon Vasilakos xvas@aueb.gr
 */

   public enum LoggingProperty implements IProperty {

      LOGGING__PATH("logging.path"),
      LOGGING__GENERAL__LEVEL("logging.general.level"),
      LOGGING__GENERAL__ENABLE_UNIVERSAL("logging.general.enable_universal"),
      LOGGING__PROPERTIES__LEVEL("logging.properties.level"),
      LOGGING__CONSOLE__LEVEL("logging.console.level"),
      LOGGING__MUS__LEVEL("logging.mus.level"),
      LOGGING__CELLS__LEVEL("logging.cells.level"),
      LOGGING__BUFF__LEVEL("logging.buff.level");
      private String propTitle;

      private LoggingProperty(String _propTitle) {
         propTitle = _propTitle;
      }

         @Override
      public String toString() {
         return propTitle + ": " +toolTip();
      }

      @Override
      public String propertyName() {
         return propTitle;
      }

      @Override
      public String toolTip() {
         return "tooltip TBD"; // TODO to define per different enum value
      }
   }
  