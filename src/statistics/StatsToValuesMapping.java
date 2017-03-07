package statistics;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import statistics.Statistics.ConfidenceInterval;

/**
 * Keeps track of different statistic titles (categories) by mapping recorded values to statistics titles.
 * Values of a given statistic are recorded in an Values instance, which is mapped to the title of the
 * statistic. Note that statistics and their corresponding values are kept in lexicographical order according
 * to statistics titles.
 *
 * @author xvas
 */
public class StatsToValuesMapping {

   /**
    * key: Statistic title
    *
    * value: Values aggregated
    */
   private final Map<String, Values> statTitle2Values = new TreeMap<>();
   private ConfidenceInterval confidence_interval;

   /**
    * Uses default confidence interval with 95% confidence.
    */
   public StatsToValuesMapping() {
      confidence_interval = ConfidenceInterval.Percentile_95;
   }

   /**
    * @param _confidence_interval The confidence interval percentile to use.
    */
   public StatsToValuesMapping(ConfidenceInterval _confidence_interval) {
      confidence_interval = _confidence_interval;
   }

   /**
    * @param confIntervalPercentile The confidence interval percentile to use.
    * @throws statistics.StatisticException
    */
   public StatsToValuesMapping(String confIntervalPercentile) throws StatisticException {
      this(ConfidenceInterval.find(confIntervalPercentile));
   }

   /**
    *
    * @param statName
    * @param roundDecismal
    * @param value
    * @return true if recorded this statName for the first time, otherwise false
     * @throws statistics.StatisticException
    */
   public boolean update(String statName, int roundDecismal, double... value) throws StatisticException {
      Values aggr;
      if ((aggr = statTitle2Values.get(statName)) == null) {
         statTitle2Values.put(statName, new Values(roundDecismal, value));
         return true;
      }
      aggr.updt(value);
      return false;
   }

   /**
    *
    * @param statName The title of the statistic
    *
    * @return An Values instance for the particular statistic or null if no statistic such name is
    * recorded.
    */
   public Values aggregatesFor(String statName) {
      return statTitle2Values.get(statName);
   }

   /**
    * @param statName The title of the statistic.
    *
    * @return The mean value of the statistic
    *
    * @throws StatisticException in case there is no record for the statistic
    */
   public double mean(String statName) throws StatisticException {
      Values aggr;
      if ((aggr = statTitle2Values.get(statName)) == null) {
         throw new StatisticException(statName + " is not recorded");
      }

      return aggr.mean();
   }

   /**
    * @param statName The title of the statistic.
    *
    * @return The summary of values recorder for the particular statistic
    *
    * @throws StatisticException in case there is no record for the statistic
    */
   public double sum(String statName) throws StatisticException {
      Values aggr;
      if ((aggr = statTitle2Values.get(statName)) == null) {
         throw new StatisticException(statName + " is not recorded");
      }

      return aggr.sum();
   }

   /**
    * @param statName The title of the statistic.
    *
    * @return The variance of values recorder for the particular statistic
    *
    * @throws StatisticException in case there is no record for the statistic
    */
   public double variance(String statName) throws StatisticException {
      Values aggr;
      if ((aggr = statTitle2Values.get(statName)) == null) {
         throw new StatisticException(statName + " is not recorded");
      }

      return aggr.variance();
   }

   public double stddev(String statName) throws StatisticException {
      Values aggr;
      if ((aggr = statTitle2Values.get(statName)) == null) {
         throw new StatisticException(statName + " is not recorded");
      }

      return aggr.stddev();
   }

   /**
    * @return the mapping between statistics titles and mapped Values instances as an unmodifiable map.
    */
   public Map<String, Values> names2aggregatesMapping() {
      return Collections.unmodifiableMap(statTitle2Values);
   }

   /**
    * Returns a string representation of the mapped aggregates in the form of a table with optionally printed
    * statistics titles combined to aggregates titles as the headers of the table. A comma separated format is
    * used to form the table output.
    *
    * @param mean
    * @param variance
    * @param stddev
    * @param confInterval
     * @param statsTitles
    * @param includeTitles should the titles be printed?
    * @return A string representation of the mapped aggregates in the form of a table.
     * @throws statistics.StatisticException
    */
   public String toCSVString(
           boolean mean, boolean variance, boolean stddev, ConfidenceInterval confInterval,
           Collection <String> statsTitles, boolean includeTitles) throws StatisticException {

      double confIntervalZ = confInterval.z();
      StringBuilder csvBuilder = new StringBuilder(180);

      //<editor-fold defaultstate="collapsed" desc="apppend the names and category of allowed stat">
      if (includeTitles) {
         for (String statName : statTitle2Values.keySet()) {
            if (mean) {
               csvBuilder.append(statName).append("").append(',');
            }
            if (variance) {
               csvBuilder.append(statName).append("(var)").append(',');
            }
            if (stddev) {
               csvBuilder.append(statName).append("(stddev)").append(',');
            }
            if (confInterval != ConfidenceInterval.NONE) {
               csvBuilder.append(statName).append('(').append(confInterval.getConfidencePercentile()).append(" confidence)").append(',');
            }
         }

         csvBuilder.append('\n');
      }//titles
//</editor-fold>
      //<editor-fold defaultstate="collapsed" desc="append the values">
      for (String statName : statsTitles) {
         Values nxtValue = statTitle2Values.get(statName);
         if (nxtValue == null) {
            nxtValue = Values.DUMMY; // in case the are no stats for this 
         }
         if (mean) {
            if (nxtValue == Values.DUMMY) {
               csvBuilder.append(",");
            } else {
               csvBuilder.append(nxtValue.mean()).append(",");
            }
         }
         if (variance) {
            if (nxtValue == Values.DUMMY) {
               csvBuilder.append(",");
            } else {
               csvBuilder.append(nxtValue.variance()).append(",");
            }
         }
         if (stddev) {
            if (nxtValue == Values.DUMMY) {
               csvBuilder.append(",");
            } else {
               csvBuilder.append(nxtValue.stddev()).append(",");
            }
         }
         if (confInterval != ConfidenceInterval.NONE) {
            if (nxtValue == Values.DUMMY) {
               csvBuilder.append(",");
            } else {
               csvBuilder.append(nxtValue.absConfInterval(confIntervalZ)).append(",");
            }
         }
      }
//</editor-fold>
      return csvBuilder.toString();
   }

   public String toString_csv_meanOnly(Collection<String> statsTitles, boolean printTitles) throws StatisticException {
      return toCSVString(true, false, false, ConfidenceInterval.NONE, statsTitles, printTitles);
   }

   /**
    * @return the usingZ
    */
   public double getConfidenceInterval_z() {
      return confidence_interval.z();
   }

   /**
    * Compresses (in the sense that values become garbage collectable) and finalizes state.
     * @throws statistics.StatisticException
    */
   public void finalizeState() throws StatisticException {
      for (Map.Entry<String, Values> entry : statTitle2Values.entrySet()) {
         Values nxt_aggregatedValues = entry.getValue();
         if (nxt_aggregatedValues != Values.DUMMY) {
            nxt_aggregatedValues.finalizeState(getConfidenceInterval_z());
         }
      }
   }

   public Set<String> getTitles() {
      return this.statTitle2Values.keySet();
   }

   /**
    * Ensures that the passed statistics titles are recorded, even if there are no values recorded for those
    * statistics.
    *
    * This is a useful method because there may be no record for some statistics with respect to different
    * simulation times. If that is the case, the values printed in results CSV output files are not aligned
    * correctly with statistics titles.
    *
    * For titles not already added to this instance, the title of the statistic is added with a null aggregate
    * instance (value) mapped to it.
    *
    * @param statisticsTitles
    */
   void ensureTitles(Set<String> statisticsTitles) {
      Set<String> knownTitles = statTitle2Values.keySet();

      for (Iterator<String> statitle_it = statisticsTitles.iterator(); statitle_it.hasNext();) {
         String nxtStatTitle = statitle_it.next();
         if (!knownTitles.contains(nxtStatTitle)) {
            statTitle2Values.put(nxtStatTitle, Values.DUMMY);
         }
      }

   }

    
    public Set<String> getStatTitles() {
        return Collections.unmodifiableSet(statTitle2Values.keySet());
    }
    public boolean containsStatTitle(String t) {
        return statTitle2Values.keySet().contains(t);
    }
}
