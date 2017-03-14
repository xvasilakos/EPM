package app.properties;

/**
 *
 * @author Xenofon Vasilakos xvas@aueb.gr
 */
public class Networking {

   public enum Rates implements IProperty {

      SC_BACKHAUL("networking.rates.sc_backhaul", "In KB/s; 1Mbps=122KB/s or 0.1192 MB/s or 125B/s"),
      SC_WIRELESS("networking.rates.sc_wireless", "In KB/s; 1Mbps=122KB/s or 0.1192 MB/s or 125B/s"),
      MC_WIRELESS("networking.rates.mc_wireless", "In KB/s; 1Mbps=122KB/s or 0.1192 MB/s or 125B/s"),
      CHUNK_SIZE("networking.chunk_size", "The size of the minimum unit of content");

      private final String _propTitle;
      private final String _tooltip;

      private Rates(String propTitle, String tooltip) {
         _propTitle = propTitle;
         _tooltip = tooltip;
      }

      @Override
      public String toString() {
         return _propTitle + ": " + toolTip();
      }

      @Override
      public String propertyName() {
         return _propTitle;
      }

      @Override
      public String toolTip() {
         return _tooltip;
      }
   }

   
}
