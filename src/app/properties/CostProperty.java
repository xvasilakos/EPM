
package app.properties;

/**
 *
 * @author Xenofon Vasilakos xvas@aueb.gr
 */
   public enum CostProperty implements IProperty {

      //////////////////////////  COSTS ///////////////////////////
     
      
      COST__CACHE__PRICING_SCHEME("cost.cache.pricing_scheme"),
      COST__EPC__GAMMA("cost.epc.gamma"),
      COST__EPC__TARGET_UTILIZATION("cost.epc.target_utilization"),
      COST__CACHE__FIXED_PRICE__SC("cost.cache.fixed_price.sc"),
      COST__TRANSMISSION__TYPE("cost.transmission.type"),
      COST__TRANSMISSION__FIXED__SC("cost.transmission.fixed.sc"),
      COST__TRANSMISSION__FIXED__MC("cost.transmission.fixed.mc"),
      // DYNAMIC TRANSMISSION COSTS  
      SC__COST_PER_BIT("sc.cost_per_bit"),
      MC__COST_PER_BIT("mc.cost_per_bit");
      private String propTitle;

      private CostProperty(String _propTitle) {
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