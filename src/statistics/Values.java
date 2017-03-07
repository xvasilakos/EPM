package statistics;

import exceptions.InconsistencyException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.management.RuntimeErrorException;
import utils.CommonFunctions;

/**
 * Keeps track of a series of values for a statistic category. Aggregate values
 * that can be computed include, are: Summary, Variance, Standard Deviation,
 * Mean, Confidence Intervals for mean
 *
 * @author xvas
 */
public class Values {

    public final static Values DUMMY = new Values(-1);
    /**
     * The values aggregated
     */
    private List<Double> values;
    /**
     * Summary of the values aggregated
     */
    private double sum;
    /**
     * The precision decimal after which aggregated values are cut. The Higher
     * it is, the greater the precision but also the greater the output overhead
     * in text format.
     */
    private final int roundDecimal;
    private final FinalizationBean finalizedBean;

    /**
     *
     * @param _roundDecimal The precision decimal after which aggregated values
     * are cut. The Higher it is, the greater the precision but also the greater
     * the output overhead in text format.
     */
    public Values(int _roundDecimal) {
        this.roundDecimal = _roundDecimal;
        this.sum = 0;
        values = new ArrayList<>();
        finalizedBean = new FinalizationBean();
    }

    public boolean isFinalized() {
        return finalizedBean.isStatusFinalized();
    }

    /**
     *
     * @param _roundDecimal The precision decimal after which aggregated values
     * are cut. The Higher it is, the greater the precision but also the greater
     * the output overhead in text format.
     *
     * @param values the initial values added.
     */
    public Values(int _roundDecimal, double... values) {
        this(_roundDecimal);
        updt(values);
    }

    /**
     * A (deep) copy constructor
     *
     * @param original the original aggregates instance to be copied
     */
    public Values(Values original) {
        this.values = new ArrayList();
        for (double nxt_val : original.values) {
            this.values.add(nxt_val);
        }

        this.sum = original.sum;
        this.roundDecimal = original.roundDecimal;
        this.finalizedBean = new FinalizationBean(original.finalizedBean);
    }

    private double round(double val) {
        if (roundDecimal < 0) {
            return val;
        }

        double rounder = Math.pow(10, roundDecimal);
        return ((int) (val * rounder) / rounder);
    }

    private double sumSqredfDiffsValuesMean() throws StatisticException {
        double mean = mean();//updated mean
        double sqrOfDiffsFromMean = 0.0;
        for (double nxt_val : this.values) {
            sqrOfDiffsFromMean += Math.pow(nxt_val - mean, 2);
        }
        return sqrOfDiffsFromMean;
    }

    public void updt(double... updValues) {
        if (isStatusFinalized()) {
            throw new InconsistencyException("Illegal action: trying to update fionalized aggregated statistics.");
        }
        for (double nxt : updValues) {
            if (Double.isNaN(nxt)) {
                continue;
            }
            this.values.add(nxt);
            sum += nxt;
        }
    }

    /**
     * @return the summary of values
     */
    public double sum() {
        if (isStatusFinalized()) {
            return finalizedBean.getSum();
        }
        return sum;
    }

    /**
     * @return the mean of values
     * @throws statistics.StatisticException
     */
    public double mean() throws StatisticException {
        if (isStatusFinalized()) {
            return finalizedBean.getMean();
        }

        if (sum != 0 && values.size() == 0) {
            throw new StatisticException();//xxx
        }
        double mean = sum / values.size();
        return round(mean);
    }

    /**
     * @return the variance of values recorded.
     * @throws statistics.StatisticException
     */
    public double variance() throws StatisticException {
        if (isStatusFinalized()) {
            return finalizedBean.getVariance();
        }
        double variance = values.isEmpty() ? 0 : sumSqredfDiffsValuesMean() / (values.size() - 1);
        return round(variance);
    }

    /**
     * @return the standard deviation of values recorded or -1 if no values are
     * recorded so far.
     * @throws statistics.StatisticException
     */
    public double stddev() throws StatisticException {
        if (isStatusFinalized()) {
            return finalizedBean.getStddev();
        }
        double stddev = values.isEmpty() ? 0 : Math.sqrt(variance());
        return round(stddev);
    }

    /**
     *
     * @param z parameter for computing confidence interval
     * @return The absolute value of confidence
     * @throws statistics.StatisticException
     */
    public double absConfInterval(double z) throws StatisticException {
        if (isStatusFinalized()) {
            return finalizedBean.getAbsConfInterval();
        }
        return round(z * stddev() / Math.sqrt(values.size()));
    }

    /**
     * @param z parameter for computing confidence interval
     *
     * @return the lower bound value of confidence interval
     */
    public double confIntervLowerBound(double z) throws StatisticException {
        double mean = -1;
        double stddev = -1;
        int valuesNum = -1;
        if (isStatusFinalized()) {
            mean = finalizedBean.getMean();
            stddev = finalizedBean.getStddev();
            valuesNum = finalizedBean.getValuesNum();
        } else {
            mean = mean();
            stddev = stddev();
            valuesNum = values.size();
        }
        double low = mean - z * stddev / Math.sqrt(valuesNum);
        return round(low);
    }

    /**
     * @param z parameter for computing confidence interval
     *
     * @return the upper bound value of confidence interval
     */
    public double confIntervUpperBound(double z) throws StatisticException {
        double mean = -1;
        double stddev = -1;
        int valuesNum = -1;
        if (isStatusFinalized()) {
            mean = finalizedBean.getMean();
            stddev = finalizedBean.getStddev();
            valuesNum = finalizedBean.getValuesNum();
        } else {
            mean = mean();
            stddev = stddev();
            valuesNum = values.size();
        }
        double hi = mean + z * stddev / Math.sqrt(valuesNum);
        return round(hi);
    }

    /**
     * Returns the list of recorded values. The order of values is defined by
     * the order they have been recoded.
     *
     * @return The list of recorded values.
     */
    public List<Double> values() {
        return Collections.unmodifiableList(values);
    }

    /**
     * @return the precision decimal after which values are cut.
     */
    public int getRoundDecimal() {
        return roundDecimal;
    }

    /**
     * Compresses (in the sense that values become garbage collectable) and
     * finalizes state.
     *
     * @param z
     * @throws statistics.StatisticException
     */
    public void finalizeState(double z) throws StatisticException {
        finalizedBean.finalizeStatus(z);
        values = null;
    }

    public boolean isStatusFinalized() {
        return finalizedBean.isStatusFinalized();
//      return values == null;
    }

    private class FinalizationBean {

        private boolean finalized;
        private double absConfInterval;
        private double mean;
        private double stddev;
        private double variance;
        private double sum;
        private int valuesNum;

        private FinalizationBean(FinalizationBean finalizedBean) {
            this.finalized = finalizedBean.finalized;
            this.absConfInterval = finalizedBean.absConfInterval;
            this.mean = finalizedBean.mean;
            this.stddev = finalizedBean.stddev;
            this.variance = finalizedBean.variance;
            this.sum = finalizedBean.sum;
            this.valuesNum = finalizedBean.valuesNum;
        }

        private FinalizationBean() {
            this.variance = -1000;
            this.stddev = -1000;
            this.mean = -1000;
            this.absConfInterval = -1000;
            this.finalized = false;
        }

        private void finalizeStatus(double z) throws StatisticException {
            sum = Values.this.sum;
            absConfInterval = Values.this.absConfInterval(z);
            mean = Values.this.mean();
            variance = Values.this.variance();
            stddev = Values.this.stddev();
            valuesNum = Values.this.values.size();

            finalized = true; // must be the lat instuction of this method.. otherwise inconsistent status.. 
        }

        /**
         * @return the finalized
         */
        private boolean isStatusFinalized() {
            return finalized;
        }

        /**
         * @return the absConfInterval
         */
        private double getAbsConfInterval() {
            return absConfInterval;
        }

        /**
         * @return the mean
         */
        private double getMean() {
            return mean;
        }

        /**
         * @return the stddev
         */
        private double getStddev() {
            return stddev;
        }

        /**
         * @return the variance
         */
        private double getVariance() {
            return variance;
        }

        /**
         * @return the sum
         */
        private double getSum() {
            return sum;
        }

        private int getValuesNum() {
            return valuesNum;
        }
    }
}
