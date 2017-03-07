package statistics.output;

import app.properties.Registry;
import app.properties.StatsProperty;
import exceptions.InvalidOrUnsupportedException;
import exceptions.NotIntiliazedException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;
import sim.ISimulationMember;
import sim.Scenario;
import sim.space.cell.CellRegistry;
import static statistics.output.Constants.REPEAT_DETAILS_BEGIN;
import static statistics.output.Constants.REPEAT_DETAILS_END;
import static statistics.output.Constants.SCENARIO_SETUP_BEGIN;
import static statistics.output.Constants.SCENARIO_SETUP_END;
import utilities.Couple;

/**
 *
 * @author Xenofon Vasilakos xvas@aueb.gr
 */
public class Printer implements ISimulationMember {

    private final PrintStream printer;
    private final sim.run.SimulationBaseRunner _sim;

    /**
     *
     * @param simulation
     * @param setup
     * @param dirPathBase parent path of directory of results for a simulation
     * run
     * @param dirPreffix
     * @param fileSuffix
     * @throws InvalidOrUnsupportedException
     * @throws FileNotFoundException
     */
    public Printer(sim.run.SimulationBaseRunner simulation, Scenario setup, String dirPathBase, String dirPreffix, String fileSuffix)
            throws InvalidOrUnsupportedException, FileNotFoundException, IOException {
        this._sim = simulation;

        /*
         * 1. Create a directory according to the date of the experiments 
         * 2. Create a subdirectory for this run's results 
         * 3. Create a file name for the particular simulation results
         */
        String dateTimeFormated = new SimpleDateFormat("yy-MM-dd").format(Calendar.getInstance().getTime());
        File dateDirectory = new File(dirPathBase + "[" + dateTimeFormated + "]");

        SortedSet<SetupBean> simSetupBeans = simSetupBeans();

        //<editor-fold defaultstate="collapsed" desc="create output directory and scenario family">
        String dirName = "";
        String directoryStr = dirPreffix
                + (simSetupBeans.size() > 0
                        ? "-" + (dirName = createOutDirName(simSetupBeans))
                        : "");
        File directory = new File(dateDirectory, directoryStr);

        if (!directory.exists()) {// then create directory and setup.txt file
            directory.mkdirs();
            File setupTxt = new File(dateDirectory,
                    dirName
                    + (dirName.length() > 0 ? "_" : "")
                    + setup.setupSignatureHash() + ".txt");
            try (PrintStream setupTxtPrntr = new PrintStream(setupTxt)) {
                setup.print(setupTxtPrntr);
            }
        }

        //</editor-fold>
        //<editor-fold defaultstate="collapsed" desc="create output file">
        String filename = "res_" + createOutFileName(simSetupBeans) + fileSuffix;
        File outputFile = new File(directory, filename);
        outputFile.createNewFile();
        //</editor-fold>

        printer = new PrintStream(outputFile);
        //@TODO printSimSetupDetails(simSetupBeans, setup);
       // printer.append('\n');
        //@TODO take care in future printSimRepeatDetails(repeatitionDetails(simulation, setup));
    }

    private SortedSet<SetupBean> repeatitionDetails(sim.run.SimulationBaseRunner simulation, Scenario scenarioSetup) throws InvalidOrUnsupportedException {
        SortedSet<SetupBean> repeatDetails = new TreeSet<>();
        SetupBean<Double> nextPriorityBean = new SetupBean();

        //<editor-fold defaultstate="collapsed" desc="SCs_Per_Point_Ratio">
        nextPriorityBean = new SetupBean();
        nextPriorityBean.setInOutputDirName(false);
        nextPriorityBean.setTitle("SCs_Per_Point_Ratio");
        nextPriorityBean.setTitleAbbreviation("#SCsPntRt");
        double scPerPoint = simulation.getArea().coveragePerPoint_SC();
        nextPriorityBean.setValue(scPerPoint);
        repeatDetails.add(nextPriorityBean);
        //</editor-fold>

        //<editor-fold defaultstate="collapsed" desc="No_Coverage_Per_Point_Ratio">
        nextPriorityBean = new SetupBean();
        nextPriorityBean.setInOutputDirName(false);
        nextPriorityBean.setTitle("NoCover_Per_Point_Ratio");
        nextPriorityBean.setTitleAbbreviation("NoCoverPntRt");
        double no_sc_perPoint = simulation.getArea().noCoveragePerPoint_SC();
        nextPriorityBean.setValue(no_sc_perPoint);
        repeatDetails.add(nextPriorityBean);
        //</editor-fold>

        //<editor-fold defaultstate="collapsed" desc="Setup_Repeat_Seed">
        nextPriorityBean = new SetupBean();
        nextPriorityBean.setInOutputDirName(false);// otherwise files will be printed over others with same setup
        nextPriorityBean.setTitle("Seed");
        nextPriorityBean.setTitleAbbreviation("Rpt");
        double seed = scenarioSetup.seed();
        nextPriorityBean.setValue(seed);
        repeatDetails.add(nextPriorityBean);
        //</editor-fold>

        return repeatDetails;
    }

    /**
     * Prints the simulation setPropertyup in the beginning of the output file
     *
     * @param simSetupDetails
     * @throws InvalidOrUnsupportedException
     */
    private void printSimSetupDetails(SortedSet<SetupBean> simSetupDetails, Scenario scenarioSetup) throws InvalidOrUnsupportedException {
        StringBuilder prepend = new StringBuilder();
        Iterator<SetupBean> simSetupDetails_iter = simSetupDetails.iterator();

        prepend.append('"');
        prepend.append(SCENARIO_SETUP_BEGIN);
        prepend.append('\n');
        prepend.append("setup family=");
        prepend.append(scenarioSetup.setupSignatureHash());
        prepend.append('\n');
        while (simSetupDetails_iter.hasNext()) {
            SetupBean next = simSetupDetails_iter.next();
            String fullTitle = next.getTitle();
            String shortTitle = next.getTitleAbbreviation();
            String val = String.valueOf(next.getValue());
            val = val.replace(',', ';');
            val = val.replace('|', '!');

            prepend.append(fullTitle).append(',').append(shortTitle).append(',').append(val).append('\n');
        }
        prepend.append(SCENARIO_SETUP_END);
        prepend.append('"');
        print(prepend.toString());
    }

    private void printSimRepeatDetails(SortedSet<SetupBean> repeatDetails) throws InvalidOrUnsupportedException {

        StringBuilder prepend = new StringBuilder(180);
        Iterator<SetupBean> repeatDetails_iter = repeatDetails.iterator();
        prepend.append('"');
        prepend.append('\n');
        prepend.append(REPEAT_DETAILS_BEGIN);
        prepend.append('\n');

        while (repeatDetails_iter.hasNext()) {
            SetupBean next = repeatDetails_iter.next();
            String fullTitle = next.getTitle();
            String shortTitle = next.getTitleAbbreviation();
            String val = String.valueOf(next.getValue());
            prepend.append(fullTitle).append(',').append(shortTitle).append(',').append(val).append('\n');
        }
        prepend.append(REPEAT_DETAILS_END);
        prepend.append('\n');
        prepend.append('"');
        println(prepend.toString());
    }

    /**
     * Note that a hash value (in string format) is used to save length in the
     * resulting directory name in cases of long parameters values.
     *
     * @param simSetup
     * @return
     * @throws InvalidOrUnsupportedException
     */
    private String createOutDirName(SortedSet<SetupBean> simSetup)
            throws InvalidOrUnsupportedException {
        StringBuilder dirName = new StringBuilder();
        Iterator<SetupBean> simSetupIter = simSetup.iterator();

        String decimalFormat = getSim().getDecimalFormat();
        DecimalFormat formatter;
        if (decimalFormat == null) {
            formatter = new DecimalFormat("#,##0.00;(#,##0.00)");
        } else {
            formatter = new DecimalFormat(decimalFormat);
        }

        while (simSetupIter.hasNext()) {
            SetupBean next = simSetupIter.next();

            if (!next.isInOutputDirName()) {
                continue;
            }

            String abbrevTitle = next.getTitleAbbreviation();
            dirName.append(abbrevTitle).append("=");

            Object value = next.getValue();
            String val;
            if ((value instanceof Number)) {
                val = formatter.format(value);
                dirName.append(val);
            } else {
                val = value.toString();
                val = val.replaceAll("\\s+", "");
                if (val.length() > 10) {
                    dirName.append(val.hashCode());
                } else {
                    dirName.append(val);
                }
            }
            if (simSetupIter.hasNext()) {
                dirName.append("_");// will not work well if last elements are for file name.. not important, not really a bug.
            }
        }

        return dirName.toString(); // use also a hash number to avoid synonyms
    }

    /**
     * Note that for parameters with big values, the hash of the value (in
     * string format) is used to save length in the resulting path name.
     *
     * @param simSetupDetails
     * @return
     * @throws InvalidOrUnsupportedException
     */
    private String createOutFileName(SortedSet<SetupBean> simSetupDetails) throws InvalidOrUnsupportedException {
        StringBuilder fileName = new StringBuilder();
        Iterator<SetupBean> iter = simSetupDetails.iterator();

        String decimalFormat = getSim().getDecimalFormat();
        DecimalFormat formatter;
        if (decimalFormat == null) {
            formatter = new DecimalFormat("#,##0.00;(#,##0.00)");
        } else {
            formatter = new DecimalFormat(decimalFormat);
        }

        while (iter.hasNext()) {
            SetupBean next = iter.next();

            if (!next.prntOutputFileName()) {
                continue;
            }
            String abbrevTitle = next.getTitleAbbreviation();
            fileName.append(abbrevTitle).append("=");

            Object value = next.getValue();
            String val;
            if ((value instanceof Number)) {
                val = formatter.format(value);
                fileName.append(val);
            } else {
                val = value.toString();
                val = val.replaceAll("\\s+", "");
                if (val.length() > 10) {
                    fileName.append(val.hashCode());
                } else {
                    fileName.append(val);
                }
            }

            if (iter.hasNext()) {
                fileName.append("_");
            }

        }
        return fileName.toString(); // use also a hash number to avoid synonyms
    }

    private SortedSet<SetupBean> simSetupBeans() throws InvalidOrUnsupportedException {
        Scenario scenario = _sim.getScenario();

        SetupBean nextPriorityBean;
        SortedSet<SetupBean> simSetupDetails = new TreeSet();

        int roundDecimal = scenario.intProperty(StatsProperty.STATS__ROUNDING_DECIMAL);
        roundDecimal = (roundDecimal > 1) ? roundDecimal - 1 : roundDecimal;

        Iterator<Couple<String, String>> iterator = scenario.getReplicationProperties().iterator();
        while (iterator.hasNext()) {
            Couple<String, String> nxtPropValue = iterator.next();
            String propAbbrev = Registry.getAbbreviation(nxtPropValue.getFirst());
            String propVal = nxtPropValue.getSecond();
            nextPriorityBean = new SetupBean();

            nextPriorityBean.setInOutputDirName(Registry.printInDirName(nxtPropValue.getFirst()));
            nextPriorityBean.setInOutputFileName(Registry.printInFileName(nxtPropValue.getFirst()));

            nextPriorityBean.setTitle(nxtPropValue.getFirst());
            nextPriorityBean.setTitleAbbreviation(propAbbrev);

            String typeOf = Registry.getTypeOf(nxtPropValue.getFirst());
            if (typeOf.equalsIgnoreCase(Registry.Type.DOUBLE.toString())) {
//                double roundedValue = ((int) (Math.pow(10, roundDecimal) * Double.valueOf(nxtPropValue.getSecond()))) / Math.pow(10, roundDecimal);
//                nextPriorityBean.setValue(roundedValue);
                nextPriorityBean.setValue(String.valueOf(Double.valueOf(nxtPropValue.getSecond())));
//            nextPriorityBean.setValue("test"+nxtPropValue.getSecond());
            } else if (typeOf.equalsIgnoreCase(Registry.Type.INT.toString())) {
                nextPriorityBean.setValue(propVal);
            } else// strings and all lists are hashed due to their long length
            {
                if (propVal.length() > 9) {
                    String tmp = String.valueOf(propVal.hashCode());
                    if (typeOf.equalsIgnoreCase(Registry.Type.STRING.toString())) {
                        tmp = propVal.substring(propVal.length() - 5) + "(" + tmp + ")";
                    } else {
                        tmp = tmp.substring(0, Math.min(5, tmp.length()));
                    }
                    nextPriorityBean.setValue(tmp);
                } else {
                    nextPriorityBean.setValue(propVal);
                }
            }

            simSetupDetails.add(nextPriorityBean);
        }

        return simSetupDetails;
    }

    @Override
    public final int simTime() {
        return getSim().simTime();
    }

    @Override
    public String simTimeStr() {
        return "[" + simTime() + "]";
    }

    @Override
    public final int simID() {
        return getSim().getID();
    }

    @Override
    public final sim.run.SimulationBaseRunner getSim() {
        return _sim;
    }

    @Override
    public final CellRegistry simCellRegistry() {
        return getSim().getCellRegistry();
    }

    public synchronized void print(String msg) {
        printer.print(msg);
    }

    public synchronized void print(String msg, Object... frmtMsgContent) {
        String formattedString = String.format(msg, frmtMsgContent);
        printer.print(formattedString);
    }

    public synchronized void println(String msg, Object... frmtMsgContent) {
        String formattedString = String.format(msg, frmtMsgContent);
        printer.println(formattedString);
    }

    public synchronized void close() {
        this.printer.close();
    }

    public synchronized void close(String lastPrintMsg, Object... msgArg) {
        print(lastPrintMsg, msgArg);
        this.printer.close();
    }

    private static class SetupBean<V> implements Comparable {

        private static final String UNDEFINED = "Undefined";
        private String _title;
        private String _titleAbbreviation;
        private V value;

        /**
         * True if it is part of the output directory name.
         */
        private boolean inOutputDirName;
        /**
         * True if it is part of the output file name.
         */
        private boolean inOutputFileName;

        public SetupBean() {
            this._title = UNDEFINED;
            this._titleAbbreviation = UNDEFINED;
            this.inOutputDirName = false;
            this.inOutputFileName = false;
        }

        /**
         * Compares based on priority.
         *
         * @param o
         * @return
         */
        @Override
        public int compareTo(Object o) {
            if (!(o instanceof SetupBean)) {
                throw new RuntimeException(
                        "Trying to compare a " + getClass().getSimpleName()
                        + " instance to a " + o.getClass().getSimpleName()
                        + " instance"
                );
            }

            SetupBean otherBean = (SetupBean) o;
            if (this._titleAbbreviation == null) {
                throw new RuntimeException(
                        new NotIntiliazedException(
                                "There is no title abbreviation specified in app.properties.resources.abbreviations.ini for " + this._title
                        )
                );
            }
            if (otherBean._titleAbbreviation == null) {
                throw new RuntimeException(
                        new NotIntiliazedException(
                                "There is no title abbreviation specified in app.properties.resources.abbreviations.ini for " + otherBean._title
                        )
                );
            }
            return this._titleAbbreviation.compareTo(otherBean._titleAbbreviation);
        }

        /**
         * @return the _title
         */
        public String getTitle() {
            return _title;
        }

        /**
         * @param title the title to set
         */
        public void setTitle(String title) {
            this._title = title;
        }

        /**
         * @return the title abbreviation
         */
        public String getTitleAbbreviation() {
            return _titleAbbreviation;
        }

     
        public void setTitleAbbreviation(String titleAbbrev) {
            this._titleAbbreviation = titleAbbrev;
        }

        /**
         * @return the value
         */
        public V getValue() {
            return value;
        }

        /**
         * @param value the value to setProperty
         */
        public void setValue(V value) {
            this.value = value;
        }

        /**
         * Is this statistic's name part of the output directory name?
         *
         * @return the outputFileNameMember
         */
        public boolean isInOutputDirName() {
            return inOutputDirName;
        }

        /**
         * Is this statistic's name part of the output file name?
         *
         * @return the outputFileNameMember
         */
        public boolean prntOutputFileName() {
            return inOutputFileName;
        }

        /**
         * Set this statistic's name as part of the output directory name.
         *
         * @param flag
         */
        public void setInOutputDirName(boolean flag) {
            this.inOutputDirName = flag;
        }

        /**
         * Set this statistic's name as part of the output file name.
         *
         * @param flag
         */
        public void setInOutputFileName(boolean flag) {
            this.inOutputFileName = flag;
        }
    }
}
