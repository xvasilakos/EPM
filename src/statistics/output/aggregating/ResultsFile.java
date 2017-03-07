package statistics.output.aggregating;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import statistics.Statistics;
import static statistics.output.Constants.REPEAT_SHORT_TITLE;
import static statistics.output.Constants.SCENARIO_SETUP_BEGIN;
import static statistics.output.Constants.SCENARIO_SETUP_END;

/**
 *
 * @author xvas
 */
public class ResultsFile {

   /**
    * Key: shortcut title for setup parameter; Value: a ScenarioParamBean containing all the necessary information for a parameter in
    * the scenario setup
    */
   private final SortedMap<String, ScenarioParamBean> scenarioSetup;
   /**
    * key: repeat variable short name; Value: the value in Double format. Only double assumed..
    */
   private final Map<String, Double> repeatDetails;
   private final Statistics statistics;
   private final String filePath;

   public ResultsFile(String _filePath, SortedMap<String, ScenarioParamBean> scenarioSetup, Map<String, Double> _repeatDetails,
         Statistics _stats) {
      this.scenarioSetup = scenarioSetup;
      this.repeatDetails = _repeatDetails;
      this.statistics = _stats;
      this.filePath = _filePath;
   }

   public boolean equalsSetupParams(Object other_obj) {
      if (other_obj == null) {
         return false;
      }
      if (getClass() != other_obj.getClass()) {
         return false;
      }

      ResultsFile other = (ResultsFile) other_obj;
      Iterator<ScenarioParamBean> scenarioSetup_iter = scenarioSetup().values().iterator();
      while (scenarioSetup_iter.hasNext()) {
         ScenarioParamBean nxt_SetupBean = scenarioSetup_iter.next();
         ScenarioParamBean other_SetupBean = other.getSetupParam(nxt_SetupBean.getTitleShort());

         if (!nxt_SetupBean.equals(other_SetupBean)) {
            return false;
         }
      }
      return true;
   }

   /**
    * @param otherObj
    * @param paramsIgnored Ignores these parameter names (either shortcut or full names) during equality check.
    * @return
    */
   public boolean equalsSetupParams(Object otherObj, Set<String> paramsIgnored) {
      if (otherObj == null) {
         return false;
      }
      if (getClass() != otherObj.getClass()) {
         return false;
      }

      ResultsFile other = (ResultsFile) otherObj;
      Iterator<ScenarioParamBean> paramsIter = scenarioSetup().values().iterator();
      while (paramsIter.hasNext()) {
         ScenarioParamBean nxtParamBean = paramsIter.next();
         if (paramsIgnored != null) {
            if (paramsIgnored.contains(nxtParamBean.getTitle_full())
                  || paramsIgnored.contains(nxtParamBean.getTitleShort())) {
               continue;
            }

         }
         ScenarioParamBean otherSetupBean = other.getSetupParam(nxtParamBean.getTitleShort());

         if (!nxtParamBean.equals(otherSetupBean)) {
            return false;
         }
      }
      return true;
   }

   /**
    * @param title_short
    * @return the ScenarioParamBean regarding the the parameter's shortcut name, or null if no such parameter exists
    */
   public ScenarioParamBean getSetupParam(String title_short) {
      return scenarioSetup().get(title_short);
   }

   /**
    * @return the scenarioSetup
    */
   public Map<String, ScenarioParamBean> scenarioSetup() {
      return Collections.unmodifiableMap(scenarioSetup);
   }

   /**
    * @return the statistics
    */
   public Statistics getStatistics() {
      return statistics;
   }

   @Override
   public String toString() {
      StringBuilder _toString = new StringBuilder(200);

      _toString.append("Parsed from: ").append(filePath).append('\n');

      _toString.append(SCENARIO_SETUP_BEGIN).append('\n');
      _toString.append('"');
      //<editor-fold defaultstate="collapsed" desc="append scenario setup">
      Iterator<ScenarioParamBean> iterator = this.scenarioSetup.values().iterator();
      while (iterator.hasNext()) {
         ScenarioParamBean setupParameter = iterator.next();
         _toString.append(setupParameter.getTitle_full()).append(',');
         _toString.append(setupParameter.getTitleShort()).append(',');
         _toString.append(setupParameter.getValue()).append('\n');
      }
      //</editor-fold>
      _toString.append('"');
      _toString.append(SCENARIO_SETUP_END);

      _toString.append(this.statistics.toString());

      return _toString.toString();
   }

   public String getFilePath() {
      return this.filePath;
   }

   public SortedSet<Integer> times() {
      return statistics.times();
   }

   /**
    * key: repeat variable short name; Value: the value in Double format. Only double assumed..
    *
    * @return the repeatDetails
    */
   public Map<String, Double> repeatDetails() {
      return Collections.unmodifiableMap(repeatDetails);
   }

   public double getRepeat() {
      return repeatDetails.get(REPEAT_SHORT_TITLE);
   }
}
