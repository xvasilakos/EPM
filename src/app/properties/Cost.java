package app.properties;

/**
 *
 * @author Xenofon Vasilakos xvas@aueb.gr
 */
public class Cost {

   public enum Cache implements IProperty {

      PRICING_SCHEME("cost.cache.pricing_scheme", ""),
      FIXED_PRICE__SC("cost.cache.fixed_price.sc", "");

      private final String _propTitle;
      private final String _tooltip;

      private Cache(String propTitle, String tooltip) {
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

   public enum EPC implements IProperty {

      GAMMA("cost.epc.gamma", ""),
      TARGET_UTILIZATION("cost.epc.target_utilization", "");

      private final String _propTitle;
      private final String _tooltip;

      private EPC(String propTitle, String tooltip) {
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

   public enum Transfer implements IProperty {

      TRANSFER_COST_ON_MISS__TYPE("cost.transfer_cost_on_miss.type", ""),
      MC__MDU("cost.mc.MDU", "Macrocell Monetary cost per Data Unit (MDU)"),
      COST__TRANSFER__WIRELESS_HOP_COST__SC("cost.transfer_delay.wireless_hop_cost.sc",
            "Wireless transfer cost for data from a smallcell"),
      COST__TRANSFER__WIRELESS_HOP_COST__MC("cost.transfer_delay.wireless_hop_cost.mc",
            "Wireless transfer cost for data from a macrocell"),
      TRANSFER__HOP_COST("cost.transfer_delay.hop_cost",  
            "Cost per hop in the remote path to the provider ISP"),
      TRANSFER__PROPAGATION__HOP_COUNT__MEAN("cost.transfer_delay.propagation.hop_count.mean",
            "Mean hop count remote distance up to the provider ISP"),
      TRANSFER__PROPAGATION__HOP_COUNT__STD("cost.transfer_delay.propagation.hop_count.std",
            "Standard deviation of the hop count in the remote path up to the provider ISP");
      

      private final String _propTitle;
      private final String _tooltip;

      private Transfer(String propTitle, String tooltip) {
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
