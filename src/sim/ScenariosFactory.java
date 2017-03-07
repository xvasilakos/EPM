package sim;

import app.arguments.MainArguments;
import app.properties.Preprocessor;
import exceptions.CriticalFailureException;
import exceptions.InconsistencyException;
import exceptions.InvalidOrUnsupportedException;
import exceptions.NotIntiliazedException;
import exceptions.ScenarioSetupException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import static logging.LoggersRegistry.CONSOLE_LOGGER;
import utilities.Couple;

/**
 * Each properties value combination gives another scenario setPropertyup
 *
 * @author Xenofon Vasilakos xvas@aueb.gr
 */
public final class ScenariosFactory {

    private static SortedSet<Scenario> _setups;
    private static boolean initialized = false;
    private static int maxAllowedConcurrentWorkers;
    private static final Object concurentAccessLock = new Object();
    private static Preprocessor preprocessedProperties;
    private static int _initialSetupsSize;
    private static int _completedSetupsNum = 0;

    /**
     * @return the preprocessedProperties
     */
    public static Preprocessor getPreprocessedProperties() {
        return preprocessedProperties;
    }

    private ScenariosFactory() {
    }

    public static void checkInitStatus() {
        synchronized (concurentAccessLock) {
            if (!initialized) {
                throw new RuntimeException("Method init() of class "
                        + ScenariosFactory.class.getName()
                        + " was not called first to initilize scenarios.");
            }
        }
    }

    /**
     * @param preprocessed
     * @param argsBean
     * @return true iff initialized; false if it was already initialized. In
     * this case the invocation has no effect. Note there is no resetProperty()
     * method, nor any equivalent.
     *
     */
    public static boolean init(Preprocessor preprocessed, MainArguments argsBean)
            throws CriticalFailureException {
        synchronized (concurentAccessLock) {
            if (initialized) {
                CONSOLE_LOGGER.log(Level.WARNING,
                        "{0}.init() invoked but ignored because {0} is already intilized.\n",
                        ScenariosFactory.class.getCanonicalName()
                );
                return false;
            }

            ScenariosFactory.preprocessedProperties = preprocessed;
            maxAllowedConcurrentWorkers = argsBean.getMaxConcurrentWorkers();

            //<editor-fold defaultstate="collapsed" desc="different scenario setups">
            try {
                List<Scenario> listOfSetups = new ArrayList<>(); // will contain result setups
                List<Scenario> tmpList = new ArrayList<>(); // temporary list for computational reasons
                listOfSetups.add(new Scenario());
                Set<String> propertyNames = preprocessed.getPropertyNames();

                for (String nxtPropName : propertyNames) {
                    Couple<List<String>, String> valuesOfNxtProp = preprocessed.getValues(nxtPropName);
                    List<String> valuesList = valuesOfNxtProp.getFirst();
                    while (!listOfSetups.isEmpty()) {
                        Scenario headScenario = listOfSetups.remove(0);// take the head of the list

                        switch (valuesList.size()) {
                            case 0:
                                throw new ScenarioSetupException("No value for property: " + nxtPropName);
                            case 1: // case of only one value: does not spawn sub-scenarios
                                String tokTrimed = valuesList.get(0).trim();
                                headScenario.setProperty(nxtPropName, tokTrimed);
                                tmpList.add(headScenario);
                                break;
                            default: // if multiple values provided, then replacate scenario for each value
                                List<Scenario> replicaSetups = Scenario.replicate(headScenario, valuesList, nxtPropName);
                                tmpList.addAll(replicaSetups);
                        }
                    }
                    listOfSetups.addAll(tmpList);
                    tmpList.clear();
                }
                _setups = new TreeSet(listOfSetups);
            } catch (ScenarioSetupException | InvalidOrUnsupportedException | InconsistencyException ex) {
                throw new CriticalFailureException(ex);
            }
            //</editor-fold>

            _initialSetupsSize = _setups.size();
            return (initialized = true);
        }
    }

    /**
     * Thread-safe
     *
     * @return
     * @throws exceptions.NotIntiliazedException
     */
    public static Scenario consumeNextSetup() throws NotIntiliazedException {
        synchronized (concurentAccessLock) {
            checkInitStatus();
            if (!_setups.isEmpty()) {
                Scenario first = _setups.first();
                _setups.remove(first);
                return first;
            } else {
                return null;
            }
        }
    }

    public static SortedSet<Scenario> unconsumedScenarios() throws NotIntiliazedException {
        synchronized (concurentAccessLock) {
            checkInitStatus();
            return Collections.unmodifiableSortedSet(_setups);
        }
    }

    public static int initialScenariosNum() throws NotIntiliazedException {
        synchronized (concurentAccessLock) {
            checkInitStatus();
            return _initialSetupsSize;
        }
    }

    public static int consumedScenariosNum() throws NotIntiliazedException {
        synchronized (concurentAccessLock) {
            checkInitStatus();
            return _initialSetupsSize - _setups.size();
        }
    }

    public static int incCompletedScenarios() throws NotIntiliazedException {
        synchronized (concurentAccessLock) {
            checkInitStatus();
            return _completedSetupsNum++;
        }
    }

    public static int completedScenariosNum() throws NotIntiliazedException {
        synchronized (concurentAccessLock) {
            checkInitStatus();
            return _completedSetupsNum;
        }
    }

    public static int unconsumedScenariosNum() throws NotIntiliazedException {
        synchronized (concurentAccessLock) {
            checkInitStatus();
            return _setups.size();
        }
    }

    public static int getMaxAllowedConcurrentWorkers() throws NotIntiliazedException {
        synchronized (concurentAccessLock) {
            checkInitStatus();
            return maxAllowedConcurrentWorkers;
        }
    }

    public static void printRawProperties(PrintStream printStream) {
        synchronized (concurentAccessLock) {
            checkInitStatus();
            getPreprocessedProperties().print(printStream);
        }
    }

    public static void printScenarios(PrintStream pout) {
        synchronized (concurentAccessLock) {
            pout.printf("The following %d pscenario setups are concidered:\n", _setups.size());
            int count = 0;
            for (Iterator<Scenario> it = _setups.iterator(); it.hasNext();) {
                Scenario simSetup = it.next();
                pout.println(
                        String.format(
                                "%d out of  %d; family id= %d; setup id= %s  ",
                                ++count, _setups.size(), simSetup.setupSignatureHash(), simSetup.getIDStr()
                        )
                );
                simSetup.print(pout);
            }
        }
    }
}
