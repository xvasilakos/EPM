package app;

import app.arguments.MainArguments;
import app.properties.Preprocessor;
import exceptions.CriticalFailureException;
import exceptions.NotIntiliazedException;
import exceptions.WrongOrImproperArgumentException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Comparator;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import sim.Scenario;
import sim.ScenariosFactory;
import sim.run.SimulationBaseRunner;
import utilities.Couple;
import utils.CommonFunctions;
import utils.DebugTool;

/**
 *
 * @author Xenofon Vasilakos xvas@aueb.gr
 */
public final class SimulatorApp {

    private static final Logger LOG = CommonFunctions.getLoggerFor(SimulatorApp.class);
    private static String resultFilesPath;

    public static final CachedTraceDocuments CROSS_SIM_TRC_DOCS = new CachedTraceDocuments();
    public static Preprocessor _preprocessedProps;
    public static MainArguments _mainArgs;

    private final static SortedMap<Thread, SimulationBaseRunner> _simWorkersMap = new TreeMap<>(new Comparator<Thread>() {
        @Override
        public int compare(Thread t1, Thread t2) {
            return t1.getName().compareTo(t2.getName());
        }
    });

    private static void exitByFail(String msg, Exception ex, int exitVal) {
        LOG.log(Level.SEVERE, SimulatorApp.class.getSimpleName() + ": exit by failure (" + exitVal + ")" + msg, ex);
//        ex.printStackTrace();
        System.exit(exitVal);
    }

    /**
     * @return the parsed _mainArgs of method main() of this class
     */
    public static MainArguments getMainArgs() {
        return _mainArgs;
    }

    /**
     * @param args the command line main_mainArguments
     * @throws java.lang.InterruptedException
     */
    public static void main(String[] args) throws InterruptedException {

        /**
         * If manually stopped by user with "Kill -15"
         */
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                for (Thread nxtSimThrd : _simWorkersMap.keySet()) {
                    if (nxtSimThrd.isAlive()) {
                        _simWorkersMap.get(nxtSimThrd).runFinish();
                        nxtSimThrd.interrupt();
                    }
                }

                DebugTool.close();
            }
        });

        Logger.getGlobal().setLevel(Level.INFO);

        try {
            _mainArgs = MainArguments.load(args);
        } catch (WrongOrImproperArgumentException ex) {
            exitByFail("Failed to load arguments propertly..\n", ex, -10);
        }

        //<editor-fold defaultstate="collapsed" desc="defaultPreprocessor properties">
        try {
            String path = _mainArgs.getPropertiesPath();

            LOG.log(Level.INFO, "Parsing properties file from path \"{0}\"\n", path);
            _preprocessedProps = Preprocessor.process(path);
            LOG.log(Level.INFO, "Simulation properties file parsed successfully from path \"{0}\"\n", path);
        } catch (CriticalFailureException ex) {
            exitByFail("Loading properties failed: " + ex.getMessage(), ex, -20);
        }
        //</editor-fold>

        //<editor-fold defaultstate="collapsed" desc="init ScenariosFactory">
        try {
            ScenariosFactory.init(_preprocessedProps, _mainArgs);
            int num = ScenariosFactory.unconsumedScenariosNum();
            LOG.log(
                    Level.INFO, "{0} simulation scenario{1} initialized\n",
                    new Object[]{num, num > 1 ? "s" : ""}
            );
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ScenariosFactory.printScenarios(new PrintStream(baos));
            LOG.log(Level.CONFIG, "{0}\n", baos.toString());
        } catch (CriticalFailureException | NotIntiliazedException ex) {
            exitByFail("\"ScenariosFactory FAILED!", ex, -30);
        }
        //</editor-fold>

        initStatsDir();
        DebugTool.init();

        //<editor-fold defaultstate="collapsed" desc="spawn simulation thread for each scenario">
        Scenario nxtScenario;
        try {
            while ((nxtScenario = ScenariosFactory.consumeNextSetup()) != null) {
                try {
                    Couple<Thread, SimulationBaseRunner> launched = SimulationBaseRunner.launchSimulationThread(nxtScenario);
                    _simWorkersMap.put(launched.getFirst(), launched.getSecond());

                    LOG.log(
                            Level.INFO, launched.getFirst().getName() + " thread for scenario {0} launched.\n",
                            nxtScenario.getIDStr()
                    );

                    LOG.log(
                            Level.FINEST, "Details:\n{0}\n", nxtScenario.toString()
                    );

                } catch (CriticalFailureException crex) {
                    LOG.log(
                            Level.SEVERE, "Simulation failed for scenario setup:\n"
                            + nxtScenario.toString(), crex
                    );
                }
            }//while
        } catch (NotIntiliazedException ex) {
            exitByFail("Simulator application  terminates unsuccessfully.\n", ex, -40);
        }
        //</editor-fold>

        LOG.exiting(SimulatorApp.class.getCanonicalName(), "main");
    }

    public static String getResultFilesPath() {
        return resultFilesPath;
    }

    private static void initStatsDir() {
        try {
            resultFilesPath
                    = MainArguments.replaceAllTags(
                            _preprocessedProps.getValues(
                                    app.properties.StatsProperty.STATS__OUTPUTDIR
                            ).getFirst().get(0)
                    );

            System.err.println("resultFilesPath=" + resultFilesPath);

            //AggregatorApp.STATS_DIR_PATH;//@TODO temporary solution
            File parent = new File(resultFilesPath);
            parent.mkdirs();

            String runsRecorded = "runs_recorded.txt";
            File file = new File(parent, runsRecorded);
            int statusRecorded = 3;
            try {
                statusRecorded = file.createNewFile() ? 1 : 0;
            } catch (IOException ex) {
                statusRecorded = 3; //in case of error!
            }
            //<editor-fold defaultstate="collapsed" desc="case: error with creating recorded runs">
            if (statusRecorded == 3) {
                int nxtRunNum = (int) (1000 * Math.random());
                resultFilesPath = resultFilesPath + "/rand_stat_id-" + nxtRunNum;
                String msg = "Cannot create file " + runsRecorded + "in path \"" + parent.getCanonicalPath() + "\". Results will be saved in directory:\n"
                        + " \"" + resultFilesPath + " \"";
                LOG.log(Level.WARNING, msg);
                System.out.println("Press enter to continue ..");
                System.in.read();
            } //</editor-fold>
            //<editor-fold defaultstate="collapsed" desc="case: keeping stats for the first simTime">
            if (statusRecorded == 1) {
                try (BufferedWriter bout = new BufferedWriter(new FileWriter(file));) {
                    bout.append("0");
                }
                resultFilesPath += "/000";
            } //</editor-fold>
            try (BufferedReader bin = new BufferedReader(new FileReader(file));) {
                //<editor-fold defaultstate="collapsed" desc="case: keeping stats at consequent times">
                if (statusRecorded == 0) {
                    int nxtRunNum = -1;
                    try {
                        String nxtRunNum_str = bin.readLine().trim();
                        bin.close();
                        nxtRunNum = Integer.parseInt(nxtRunNum_str) + 1;

                        if (nxtRunNum < 100 && nxtRunNum > 9) {
                            resultFilesPath = resultFilesPath + "/0" + nxtRunNum;
                        } else if (nxtRunNum < 10) {
                            resultFilesPath = resultFilesPath + "/00" + nxtRunNum;
                        } else {
                            resultFilesPath = resultFilesPath + "/" + nxtRunNum;
                        }
                    } catch (IOException | NumberFormatException ex) {
                        //<editor-fold defaultstate="collapsed" desc="case: error with reading recorded runs">
                        nxtRunNum = (int) (1000 * Math.random());
                        resultFilesPath = resultFilesPath + "/rand_stat_id-" + nxtRunNum;
                        String msg = "Cannot read from file " + runsRecorded + ". Results will be saved in directory:\n"
                                + " \"" + resultFilesPath + " \"";
                        LOG.log(Level.WARNING, msg, ex);
                        System.out.println("Press enter to continue ..");
                        System.in.read();
                        //</editor-fold>
                    }
                    try (BufferedWriter bout = new BufferedWriter(new FileWriter(file));) {
                        bout.append(String.valueOf(nxtRunNum));
                    } catch (IOException ex) {
                        //<editor-fold defaultstate="collapsed" desc="case: error with writting to recorded runs">
                        nxtRunNum = (int) (1000 * Math.random());
                        resultFilesPath = resultFilesPath + "/rand_stat_id-" + nxtRunNum;
                        String msg = "Cannot write to file " + runsRecorded + ". Results will be saved in directory:\n"
                                + " \"" + resultFilesPath + " \"";
                        LOG.log(Level.WARNING, msg, ex);
                        System.out.println("Press enter to continue ..");
                        System.in.read();
                        //</editor-fold>
                    }
                }
                //</editor-fold>
            } catch (IOException ex) {
                throw new Exception(ex);
            }
        } catch (Exception ex) {
            try {
                int nxtRunNum = (int) (1000 * Math.random());
                resultFilesPath = resultFilesPath + "/rand_stat_id-" + nxtRunNum;
                String msg = "Problem with intiliazing path to output dir for statistics results.";
                LOG.log(Level.WARNING, msg, ex);
                System.out.println("Results will be logged in: " + resultFilesPath);
                System.out.println("Press enter to continue ..");
                System.in.read();
            } catch (IOException ex1) {
                //ignore
            }
        }
    }
}//class
