package sim;

import app.SimulatorApp;
import app.arguments.MainArguments;
import app.properties.IProperty;
import app.properties.Preprocessor;
import app.properties.Registry;
import app.properties.Simulation;
import app.properties.Space;
import app.properties.valid.Values;
import static app.properties.valid.Values.CLOSEST_IN_RANGE;
import static app.properties.valid.Values.MAX_CACHED_EPC_STD;
import static app.properties.valid.Values.OUT_OF_RANGE;
import static app.properties.valid.Values.RANDOM_IN_RANGE;
import caching.CachingPoliciesFactory;
import caching.base.AbstractCachingPolicy;
import exceptions.CriticalFailureException;
import exceptions.InconsistencyException;
import exceptions.InvalidOrUnsupportedException;
import exceptions.ScenarioSetupException;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SortedSet;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import sim.space.Area;
import sim.space.cell.MacroCell;
import sim.time.AbstractClock;
import utils.CommonFunctions;
import utilities.Couple;
import utils.random.RandomGeneratorWrapper;

/**
 *
 * @author xvas
 */
public class Scenario implements Comparable<Scenario> {

    public static final Object NONE = new Object();

    /*
    * The following maps contain the name of property as a key and its value.
    * There can be different types of values, therefore there is on map for each 
    * type of value(s).
     */
    /**
     * Map of properties with string values. Key: the name of the property;
     * value: the string value of the property.
     */
    private final Map<String, String> _strProps;
    private final Map<String, String> _customProps;
    /**
     * Map of properties with a list of string values. Key: the name of the
     * property; value: the list of string values of the property.
     */
    private final Map<String, List< String>> _strListProps;
    /**
     * Map of properties with integer values. Key: the name of the property;
     * value: the integer value of the property.
     */
    private final Map<String, Integer> _intProps;
    /**
     * Map of properties with a list of integer values. Key: the name of the
     * property; value: the list of integer values of the property.
     */
    private final Map<String, List< Integer>> _intListProps;
    /**
     * Map of properties with double values. Key: the name of the property;
     * value: the double value of the property.
     */
    private final Map<String, Double> _doubleProps;
    /**
     * Map of properties with a list of double values. Key: the name of the
     * property; value: the list of double values of the property.
     */
    private final Map<String, List<Double>> _doubleListProps;
    private final int _level;
    /**
     * transition probabilities as defined for the current scenario
     */
    private List<double[][]> transProbabilities;
    /**
     * A list of property name and value couples that keeps track of all the
     * different property values in a single _setup used to create different
     * scenarios running in parallel.
     */
    private final SortedSet<Couple<String, String>> _replicationProperties;
    private final Logger _logger;

    {// initilization block shared by all constructors
        _customProps = new HashMap<>(10, 0.25f);
        _strProps = new HashMap<>(35, 0.25f);
        _strListProps = new HashMap<>(35, 0.25f);
        _intProps = new HashMap<>(50, 0.25f);
        _intListProps = new HashMap<>(5, 0.25f);
        _doubleProps = new HashMap<>(25, 0.25f);
        _doubleListProps = new HashMap<>(25, 0.25f);
        _replicationCount = 0;
    }
    /**
     * Number of known replicate of this scenario. Each simTime a replicate of
     * this scenario is created, this counter is incremented.
     */
    private int _replicationCount;
    private final int _id;
    private final String _idStr;
    private static int idGen = 0;
    private RandomGeneratorWrapper randGen;

    public static final Scenario replicate(Scenario original) throws ScenarioSetupException {
        ++original._replicationCount;
        return new Scenario(original);
    }

    public static List<Scenario> replicate(Scenario original, List<String> valuesList, String propertyName)
            throws ScenarioSetupException, InvalidOrUnsupportedException {
        List<Scenario> reps = new ArrayList<>(valuesList.size());
        for (String nxt_value : valuesList) {
            Scenario replica = replicate(original);

            replica.setProperty(propertyName, nxt_value.trim());
            replica.addReplicationProperty(propertyName, nxt_value.trim());
            reps.add(replica);
        }
        return reps;
    }

    /**
     * Constructor for zero level scenarios, i.e. scenarios that do not stem
     * from replicating an other scenario.
     *
     * replicate()
     */
    protected Scenario() {
        this._replicationProperties = new TreeSet<>(new Comparator<Couple>() {

            @Override
            public int compare(Couple t1, Couple t2) {
                return t1.compareToFirst(t2);
            }
        });
        _replicationCount = 0;
        _level = 0;

        _id = ++idGen;
        _logger = CommonFunctions.getLoggerFor(Scenario.class, "scenarioID=" + _id);
        _idStr = _id + "";
    }

    private Scenario(Scenario original) throws ScenarioSetupException {
        this._replicationProperties = new TreeSet<>(new Comparator<Couple>() {
            @Override
            public int compare(Couple t1, Couple t2) {
                return t1.compareToFirst(t2);
            }
        });

        this._strProps.putAll(original._strProps);
        this._customProps.putAll(original._customProps);
        this._strListProps.putAll(original._strListProps);

        this._doubleProps.putAll(original._doubleProps);
        this._doubleListProps.putAll(original._doubleListProps);

        this._intProps.putAll(original._intProps);
        this._intListProps.putAll(original._intListProps);

        this._replicationProperties.addAll(original._replicationProperties);

        _replicationCount = 0;
        _level = original._level + 1;

        _id = ++idGen;
        _logger = CommonFunctions.getLoggerFor(Scenario.class, "scenarioID=" + _id);
        _idStr = original._idStr + (_level < 10 ? ".0" : ".") + _level;
    }

    private RuntimeException onPropertyFail(String propertyName, String typeSearched) {
        ScenariosFactory.checkInitStatus();

        StringBuilder msg = new StringBuilder(180);
        msg.append("No such ").append(typeSearched).append(" property loaded with name \"");
        msg.append(propertyName).append("\"");
        String loadedType = Registry.getTypeOf(propertyName);
        if (loadedType != null) {
            msg.append("; the property has ").append(loadedType).append(" type");
        }

        //  msg.append("\n\n").append(this.toString());
        return new RuntimeException(msg.toString());
    }

    /**
     * The setPropertyup family number. A setPropertyup family number is a
     * unique id number computed after the property values of this scenario,
     * excluding the repeat number. Therefore, repeats of the same setPropertyup
     * have the same setPropertyupFamily number.
     *
     * @return the unique number of this setPropertyup's family.
     */
    public int setupSignatureHash() {
        int hash = 5;
        hash = 7 * hash + this._strProps.hashCode();
        hash = 17 * hash + this._customProps.hashCode();
        hash = 21 * hash + this._strListProps.hashCode();

        //<editor-fold defaultstate="collapsed" desc="exclude repeat number (i.e. the seed) from hash computation">
        Iterator<Map.Entry<String, Integer>> iterator = this._intProps.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Integer> next = iterator.next();
            hash = 23 * hash + next.getValue().hashCode();
        }
        //</editor-fold>

        hash = 3 * hash + this._intListProps.hashCode();

        hash = 17 * hash + this._doubleProps.hashCode();
        hash = 25 * hash + this._doubleListProps.hashCode();
        return hash;
    }

    /**
     * The hash code is computed after all the property values.
     *
     * @return
     */
    @Override
    public int hashCode() {
        int hash = 5;
        hash = 7 * hash + this._strProps.hashCode();
        hash = 17 * hash + this._customProps.hashCode();
        hash = 21 * hash + this._strListProps.hashCode();
        hash = 23 * hash + this._intProps.hashCode();
        hash = 3 * hash + this._intListProps.hashCode();
        hash = 17 * hash + this._doubleProps.hashCode();
        hash = 25 * hash + this._doubleListProps.hashCode();
        return hash;
    }

    /**
     * Two scenarios are equal if all properties have the same values (including
     * the repeat number.
     *
     * other
     *
     * @return
     */
    @Override
    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }
        if (getClass() != other.getClass()) {
            return false;
        }
        final Scenario otherScenario = (Scenario) other;
        if (!Objects.equals(this._strProps, otherScenario._strProps)) {
            return false;
        }
        if (!Objects.equals(this._customProps, otherScenario._customProps)) {
            return false;
        }
        if (!Objects.equals(this._strListProps, otherScenario._strListProps)) {
            return false;
        }
        if (!Objects.equals(this._intProps, otherScenario._intProps)) {
            return false;
        }
        if (!Objects.equals(this._intListProps, otherScenario._intListProps)) {
            return false;
        }
        if (!Objects.equals(this._doubleProps, otherScenario._doubleProps)) {
            return false;
        }
        if (!Objects.equals(this._doubleListProps, otherScenario._doubleListProps)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder _toString = new StringBuilder();
        _toString.append("#Printing scenario setup with setup id: ").append(_id).append("\n");
        _toString.append("#Properties loaded from file: ").append(
                Preprocessor.defaultPreprocessor().getUSER_PASSED_PROPS_PATH()).append("\n");

        //<editor-fold defaultstate="collapsed" desc="int - int list">
        _toString.append("\n#int properties:\n");
        for (Map.Entry<String, Integer> entry : this._intProps.entrySet()) {
            String propName = entry.getKey();
            Integer propValue = entry.getValue();
            _toString.append("\t").append(propName).append("=").append(propValue).append("\n");
        }

        _toString.append("\n#int list properties:\n");
        for (Map.Entry<String, List<Integer>> entry : this._intListProps.entrySet()) {
            String propName = entry.getKey();
            List<Integer> propValue = entry.getValue();
            _toString.append("\t");
            _toString.append(propName + "=");
            for (Iterator<Integer> it = propValue.iterator(); it.hasNext();) {
                int nxtval = it.next();
                _toString.append(nxtval).append(", ");
            }
            _toString.append("\n");

        }
//</editor-fold>

        //<editor-fold defaultstate="collapsed" desc="double - double list">
        _toString.append("\n#double properties:\n");
        for (Map.Entry<String, Double> entry : this._doubleProps.entrySet()) {
            String propName = entry.getKey();
            Double propValue = entry.getValue();
            _toString.append("\t").append(propName).append("=").append(propValue).append("\n");
        }

        _toString.append("\n#double list properties:\n");
        for (Map.Entry<String, List<Double>> entry : this._doubleListProps.entrySet()) {
            String propName = entry.getKey();
            List<Double> propValue = entry.getValue();
            _toString.append("\t");
            _toString.append(propName + "=");
            for (Iterator<Double> it = propValue.iterator(); it.hasNext();) {
                double nxtval = it.next();
                _toString.append(nxtval).append(", ");
            }
            _toString.append("\n");
        }
//</editor-fold>

        //<editor-fold defaultstate="collapsed" desc="String - String list">
        _toString.append("\n#String properties:\n");
        for (Map.Entry<String, String> entry : this._strProps.entrySet()) {
            String propName = entry.getKey();
            String propValue = entry.getValue();
            _toString.append("\t").append(propName).append("=").append(propValue).append("\n");
        }

        _toString.append("\n#Custom properties:\n");
        for (Map.Entry<String, String> entry : this._customProps.entrySet()) {
            String propName = entry.getKey();
            String propValue = entry.getValue();
            _toString.append("\t").append(propName).append("=").append(propValue).append("\n");
        }

        _toString.append("\n#String list properties:\n");
        for (Map.Entry<String, List<String>> entry : this._strListProps.entrySet()) {
            String propName = entry.getKey();
            List<String> propValue = entry.getValue();
            _toString.append("\t");
            _toString.append(propName).append("=");
            Iterator<String> iter_values = propValue.iterator();
            while (iter_values.hasNext()) {
                String nxtval = iter_values.next();
                _toString.append(nxtval).append(", ");
            }
            _toString.append("\n");
        }
//</editor-fold>

        return _toString.toString();
    }

    public void print(PrintStream out) {
        out.print(toString());
    }

    //<editor-fold defaultstate="collapsed" desc=" = = = = = = methods for handling property values = = = = = = = ">
    public void addReplicationProperty(String propName, String val) {
        _replicationProperties.add(new Couple<>(propName, val));
    }

    /**
     * @return the properties that yield multiple scenarios due to having
     * multiple values in the same properties _setup file, coupled by their
     * values in the current scenario.
     */
    public SortedSet<Couple<String, String>> getReplicationProperties() {
        return Collections.unmodifiableSortedSet(_replicationProperties);
    }

    public void setProperty(String propName, String val) throws InvalidOrUnsupportedException, ScenarioSetupException {
        try {
            String type = Registry.getTypeOf(propName);
            if (type == null) {
                throw new InvalidOrUnsupportedException(
                        "Type not known for property: \"" + propName
                        + "\" Please specify type of property @property.types.ini."
                );
            }

            StringTokenizer toks;

            if (type.equals(Registry.Type.CUSTOM.toString())) {
                _customProps.put(propName, val);
            } else if (type.equals(Registry.Type.INT.toString())) {
                _intProps.put(propName, (int) Double.parseDouble(val));

            } else if (type.equals(Registry.Type.LIST_INT.toString())) {
                toks = new StringTokenizer(val, Values.LIST_SEPARATOR);
                List<Integer> intlist = new ArrayList<>();
                while (toks.hasMoreElements()) {
                    String tokTrimed = toks.nextToken().trim();
                    if (tokTrimed.equals(Values.NONE)
                            || tokTrimed.equals(Values.UNDEFINED)) {
                        continue;
                    }
                    intlist.add((int) Double.parseDouble(tokTrimed));
                }
                _intListProps.put(propName, intlist);

            } else if (type.equals(Registry.Type.DOUBLE.toString())) {
                _doubleProps.put(propName, Double.valueOf(val));

            } else if (type.equals(Registry.Type.LIST_DOUBLE.toString())) {
                toks = new StringTokenizer(val, Values.LIST_SEPARATOR);
                List<Double> dbllist = new ArrayList<>();
                while (toks.hasMoreElements()) {
                    dbllist.add(Double.parseDouble(toks.nextToken().trim()));
                }
                _doubleListProps.put(propName, dbllist);

            } else if (type.equals(Registry.Type.STRING.toString())) {
                _strProps.put(propName, relativePathCheck(val));

            } else if (type.equals(Registry.Type.CUSTOM.toString())) {
                _customProps.put(propName, relativePathCheck(val));

            } else if (type.equals(Registry.Type.LIST_STRING.toString())) {
                toks = new StringTokenizer(val, Values.LIST_SEPARATOR);
                List strlist = new ArrayList<>();
                while (toks.hasMoreElements()) {
                    String value = toks.nextToken().trim();
                    strlist.add(relativePathCheck(value));
                }
                _strListProps.put(propName, strlist);

            }
        } catch (IOException ex) {
            throw new InvalidOrUnsupportedException(ex);
        } catch (java.lang.NumberFormatException nf) {
            throw new exceptions.ScenarioSetupException("For property: \"" + propName + "\" with value \"" + val + "\"", nf);
        }
    }

    /**
     * If relativeChecked is a relative path string, then the relative part is
     * replaced by the parent directly path of the properties file as the latter
     * is defined in the main method's arguments in class SimulatorApp.
     *
     * str
     *
     * @return
     * @throws java.io.IOException if the path does not exist or any other IO
     * problem happens.
     */
    public String relativePathCheck(String str) throws IOException {
        String relativeChecked = str;
        if (relativeChecked.startsWith("./")
                || relativeChecked.startsWith(".\\")) {
            relativeChecked = (new File(SimulatorApp.getMainArgs().getPropertiesParent() + "/" + relativeChecked)).getCanonicalPath();
        } else if (relativeChecked.startsWith("./")
                || relativeChecked.startsWith(".\\")) {
            relativeChecked = (new File(SimulatorApp.getMainArgs().getPropertiesParent() + "/" + relativeChecked.substring(1))).getCanonicalPath();
        }
        return relativeChecked;
    }

    public int intProperty(IProperty property) {
        return intProperty(property.propertyName());
    }

    public int intProperty(String propertyName) {
        Integer toreturn = _intProps.get(propertyName);
//        if (toreturn == null) {
//            throw onPropertyFail(propertyName, Registry.Type.INT.toString());
//        }
        return toreturn;
    }

    public List<Integer> listOfIntegersProperty(IProperty property) {
        return listOfIntProperty(property.propertyName());
    }

    public List<Integer> listOfIntProperty(String propertyName) {
        List<Integer> toreturn = _intListProps.get(propertyName);
//        if (toreturn == null) {
//            throw onPropertyFail(propertyName, Registry.Type.LIST_INT.toString());
//        }
        return toreturn;
    }

    public double doubleProperty(IProperty property) {
        return doubleProperty(property.propertyName());
    }

    public double doubleProperty(String propertyName) {
        Double toreturn = _doubleProps.get(propertyName);
//        if (toreturn == null) {
//            throw onPropertyFail(propertyName, Registry.Type.DOUBLE.toString());
//        }
        return toreturn;
    }

    public List<Double> listOfDoublesProperty(IProperty property) {
        return listOfDoublesProperty(property.propertyName());
    }

    public List<Double> listOfDoublesProperty(String propertyName) {
        List<Double> toreturn = _doubleListProps.get(propertyName);
//        if (toreturn == null) {
//            throw onPropertyFail(propertyName, Registry.Type.LIST_DOUBLE.toString());
//        }
        return toreturn;
    }

    public String stringProperty(IProperty property, boolean isPath) {
        return stringProperty(property.propertyName(), isPath);
    }

    public String stringProperty(String propertyName, boolean isPath) {
        String toreturn = _strProps.get(propertyName);
//        if (toreturn == null) {
//            throw onPropertyFail(propertyName, Registry.Type.STRING.toString());
//        }
        toreturn = removePathTags(isPath, toreturn);

        return toreturn;
    }

    private String removePathTags(boolean isPath, String toreturn) {
        if (isPath) {
            if (toreturn.startsWith(MainArguments.Defaults.DEFAULT_PROPS_TAG)) {
                toreturn = toreturn.replace(MainArguments.Defaults.DEFAULT_PROPS_TAG, MainArguments.Defaults.PROPERTIES_DIR_PATH);
            } else if (toreturn.startsWith(MainArguments.Defaults.DEFAULT_FILES_TAG)) {
                toreturn = toreturn.replace(MainArguments.Defaults.DEFAULT_FILES_TAG, MainArguments.Defaults.FILES_PATH);
            } else if (toreturn.startsWith(MainArguments.Defaults.HOME_TAG)) {
                toreturn = toreturn.replace(MainArguments.Defaults.HOME_TAG, MainArguments.Defaults.HOME);
            }
        } else if (toreturn.startsWith(MainArguments.Defaults.DEFAULT_PROPS_TAG) || toreturn.startsWith(MainArguments.Defaults.DEFAULT_FILES_TAG) || toreturn.startsWith(MainArguments.Defaults.HOME_TAG)) {
            throw new RuntimeException("xxx");
        }
        return toreturn;
    }

    public List<String> listOfStringsProperty(IProperty property, boolean containsPaths) {
        return listOfStringsProperty(property.propertyName(), containsPaths);
    }

    public List<String> listOfStringsProperty(String propertyName, boolean constainsPaths) {
        List<String> toreturn = _strListProps.get(propertyName);
//        if (toreturn == null) {
//            throw onPropertyFail(propertyName, Registry.Type.LIST_STRING.toString());
//        }

        if (constainsPaths) {
            List<String> tmp = new ArrayList<>();
            for (int i = 0; i < toreturn.size(); i++) {
                String nxt = toreturn.get(i);
                nxt = removePathTags(true, nxt);
                tmp.add(nxt);
            }
            toreturn = tmp;
        }

        return toreturn;
    }

    /**
     * property must have string type.
     *
     * @return
     * 
     */
    public boolean isFalse(String property) throws InvalidOrUnsupportedException {
        return !isTrue(property);
    }

    public boolean isFalse(IProperty property) throws InvalidOrUnsupportedException {
        return isFalse(property.propertyName());
    }

    /**
     * property must have string type.
     *
     * @return
     * 
     */
    public boolean isTrue(String property) throws InvalidOrUnsupportedException {
        String value = stringProperty(property, false);
        if (value.equalsIgnoreCase(Values.TRUE)) {
            return true;
        } else if (value.equalsIgnoreCase(Values.FALSE)) {
            return false;
        } else {
            throw new InvalidOrUnsupportedException("The value of property " + property
                    + " is not valid for boolean check: " + value);
        }
    }

    public boolean isTrue(IProperty propertyName) throws InvalidOrUnsupportedException {
        return isTrue(propertyName.propertyName());
    }
    //</editor-fold>

    ////// custom properties /////
    public int seed() {
        String propertyName = Simulation.SEED.propertyName();
        String str = _customProps.get(propertyName);

        if (str == null || str.equalsIgnoreCase("TIME")) {
            return (int) (Math.random() * System.currentTimeMillis() / 1000000);
        } else {
            return Integer.parseInt(str);
        }
    }

    /**
     * Parses the mobile transition probabilities for each group of MUs.
     *
     * The probabilities are parsed only the first time this method is invoked.
     * For consequent calls, the already parsed probabilities are returned.
     *
     *
     *
     ***************************** Different groups of mobiles
     * ***************************
     *
     *
     * Each element in the list is an array of doubles that defines the mobility
     * probabilities of each different group of mobiles. In other words, the
     * different elements in the list, i.e. the different double [][] arrays in
     * the list, correspond to the different probabilities between the different
     * groups. The different groups are separated by a "|" in the parsed
     * property string value. *
     *
     *
     *
     ************ Different subset of mobiles within groups of mobiles
     * *****************
     *
     *
     * Different rows within the same element (double [][] array) correspond to
     * the different probabilities of each subset of MUs within the same group
     * of MUs.
     *
     * Within each subset's probabilities, the first double is the percentage of
     * MUs that conform to the following nine probability values, each for each
     * moving direction. For instance, the first 10 double values are i) the
     * percentage of MUs that conform to ii) the following 9 transition
     * probabilities, then the next 10 doubles refer to the following subset of
     * MUs within the same group etc.
     *
     *
     *
     * @return a list of mobile transition probabilities per group of mobiles.
     *
     *  In case there is something wrong
     * with the parsed percentages of probabilities
     */
    public List<double[][]> parseMobileTransProbs() throws InvalidOrUnsupportedException {
        if (transProbabilities != null) { // in this case it is already loaded; just return it
            return Collections.unmodifiableList(transProbabilities);
        }

        transProbabilities = new ArrayList<>();
        String propertyName = Space.MU__TRANSITION_PROBABILITIES__MATRIX.propertyName();
        String probsStr = _customProps.get(propertyName).trim();

        // find the probs sets for each group of mobiles
        StringTokenizer tok = new StringTokenizer(probsStr, "|");
        while (tok.hasMoreElements()) {// for the next group of mobiles. 
            String nxtProbSet_str = tok.nextToken();
            String exceptionMsg = "During parsing subset of mobile probabilities: "
                    + "\n \""
                    + nxtProbSet_str
                    + "\""
                    + "\nThere must be a \"$\" followed by 10 "
                    + "double numbers for each subset of MUs. The double numbers denote are "
                    + " i) the percentage of mobiles that "
                    + "conform to ii) 9 mobile transition probabilities. ";

            StringTokenizer tokProbs = new StringTokenizer(nxtProbSet_str, ",");
            if (tokProbs.countTokens() % 10 != 0) {
                throw new InvalidOrUnsupportedException(
                        exceptionMsg + "\nThe number of parsed tokens in property "
                        + Space.MU__TRANSITION_PROBABILITIES__MATRIX.name()
                        + " is " + tokProbs.countTokens()
                        + ": \"" + nxtProbSet_str + "\""
                );
            }

            //<editor-fold defaultstate="collapsed" desc="read 9 probabilities">
            double[][] subsetProbs = new double[tokProbs.countTokens() / 10][10];
            /* Each row refers to a subset of MUs, 
          * including the percentage of MUs at 
          * column zero, followed by the 9 probabilities 
          * of the transition matix
             */

            int count = 0;
            double checkSum_probs = 0;
            double checkSum_subsetPercect = 0;

            while (tokProbs.hasMoreElements()) {
                int row = count / 10;
                int column = count % 10;

                if (column == 0) { // start with the % of mus in the form of "$x%"
                    String musPercent_str = tokProbs.nextToken().trim();

                    if (!musPercent_str.startsWith("$")) {
                        throw new InvalidOrUnsupportedException(
                                "Missing \"$\" before "
                                + "\"" + musPercent_str + "\" "
                                + " in the beggining of mobile's set probabilities "
                                + ": \"" + nxtProbSet_str + "\""
                                + "\n"
                                + exceptionMsg);

                    }

                    subsetProbs[row][column] = Double.parseDouble(
                            musPercent_str.substring(1)// get rid of "$"
                    );

                    checkSum_probs = 0; // restart checking if probs are getting equal to 1
                    checkSum_subsetPercect += subsetProbs[row][column];
                } else {// summarize probs from columns 1 to 9
                    subsetProbs[row][column] = Double.parseDouble(tokProbs.nextToken());

                    checkSum_probs += subsetProbs[row][column];
                    if (column == 9 && checkSum_probs > 1.01) {
                        throw new InconsistencyException("Transition matrix does not summarize to 1.0 " //confusing with groups if printed: for subset " + (row + 1)
                                + " that refers to percentage of MUs " + subsetProbs[row][0] + ". "
                                + " The sum of probabiliies is " + checkSum_probs);
                    }
                }

                count++;
            }//while

            if (checkSum_subsetPercect < 1.0 || checkSum_subsetPercect > 1.01) {
                throw new InconsistencyException("The percentages of MUs subsets summarizes to " + checkSum_subsetPercect + " instead of 1.0.");
            }
            //</editor-fold>

            transProbabilities.add(subsetProbs);
        }

        String muTrace = stringProperty(Space.MU__TRACE, true);
        if (muTrace.equalsIgnoreCase(Values.NONE)) {
            StringBuilder sb = new StringBuilder();
            sb.append("\n**********");
            sb.append("\nTrasition probability matrix");
            sb.append("\n mobiles% | NW | N | NE | W | Immobile | E | WS | S | ES\n");

            for (double[][] transProbability : transProbabilities) {
                for (int i = 0; i < transProbability.length; i++) {
                    double[] transProbability1 = transProbability[i];
                    sb.append(CommonFunctions.toString(transProbability1)).append('\n');
                }
            }
            _logger.info(sb.toString());
        }

        return Collections.unmodifiableList(transProbabilities);
    }
    ////// custom properties /////

    public MacroCell initMC(sim.run.SimulationBaseRunner simulation, Area area) throws CriticalFailureException {
        try {
            MacroCell macro = MacroCell.createMacrocell(simulation, area);
            return macro;
        } catch (Exception ex) {
            _logger
                    .log(Level.SEVERE, ex.getMessage(), ex);
            throw new CriticalFailureException(ex);
        }
    }

    public RandomGeneratorWrapper getRandomGenerator() {
        if (randGen == null) {
            try {
                randGen = new RandomGeneratorWrapper(seed());
            } catch (RuntimeException ex) {
                throw new InconsistencyException("seed property not defined or not retrieved from"
                        + " properties file. Make sure the properties file is loaded *and*"
                        + " parsed before using any method that invokes the random generator",
                        ex);
            }
        }
        return randGen;
    }

    public Area initArea(sim.run.SimulationBaseRunner sim) throws CriticalFailureException {
        Area theArea = new Area(sim,
                intProperty(Space.AREA__Y),
                intProperty(Space.AREA__X));
        _logger
                .log(Level.INFO, "{0}: {1}x{2} area; number of points={3}\n",
                        new Object[]{
                            sim.simTime(),
                            intProperty(Space.AREA__Y),
                            intProperty(Space.AREA__X),
                            theArea.size()
                        });
        return theArea;
    }

    public AbstractClock initClock(sim.run.SimulationBaseRunner sim) throws CriticalFailureException {
        AbstractClock clock = null;
        try {
            String clockClasspath = stringProperty(Simulation.Clock.TYPE, true);

            Constructor constructor = Class.forName(clockClasspath).getConstructor(new Class[]{sim.run.SimulationBaseRunner.class});
            Object clockInstance = constructor.newInstance(sim);
            if (!(clockInstance instanceof sim.time.AbstractClock)) {
                throw new InconsistencyException("Not a valid clock class. Must be an extention of "
                        + sim.time.AbstractClock.class.getCanonicalName());
            }

            return clock = (AbstractClock) clockInstance;
        } catch (Exception ex) {
            _logger
                    .log(Level.SEVERE, ex.getMessage(), ex);
            throw new CriticalFailureException(ex);
        }
    }

    //</editor-fold>
    /**
     * Parses the list of connection policy rules to a SC.
     *
     * The list is parsed the first simTime this method is invoked or it is
     * simply returned for consequent calls to this method.
     *
     * @return the list of connection policy rules
     * 
     */
    public List<String> parseConnPolicySC() {

        List<String> conn2SCPolicy = listOfStringsProperty(Space.SC__CONNECTION_POLICY, false);

        if (conn2SCPolicy.size() == 1) {
            if (conn2SCPolicy.get(0).equals(app.properties.valid.Values.CC)
                    || conn2SCPolicy.get(0).equals(app.properties.valid.Values.CACHED_CONTENT)) {
                conn2SCPolicy.remove(0);
                conn2SCPolicy.add(OUT_OF_RANGE);
                conn2SCPolicy.add(MAX_CACHED_EPC_STD);
                conn2SCPolicy.add(RANDOM_IN_RANGE);
            }
        }
        if (conn2SCPolicy.size() == 1) {
            if (conn2SCPolicy.get(0).equals(app.properties.valid.Values.CR)
                    || conn2SCPolicy.get(0).equals(app.properties.valid.Values.CLOSEST__RANGE)) {
                conn2SCPolicy.remove(0);
                conn2SCPolicy.add(CLOSEST_IN_RANGE);
                //conn2SCPolicy.add(MAX_CACHED_EPC_STD);
                conn2SCPolicy.add(RANDOM_IN_RANGE);
            }
        }
        return conn2SCPolicy;
    }

    public List<AbstractCachingPolicy> loadCachingPolicies() throws CriticalFailureException {
        List<AbstractCachingPolicy> loaded = new ArrayList();

        Collection<String> cachingPolicies = listOfStringsProperty(app.properties.Caching.CACHING__POLICIES, false
        );

        for (String nxtMthd : cachingPolicies) {
            try {
                loaded.add(CachingPoliciesFactory.addCachingPolicy(nxtMthd));
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException ex) {
                throw new CriticalFailureException(ex);
            }
        }
        return loaded;
    }

    @Override
    public int compareTo(Scenario s) {
        return _id - s._id;
    }

    /**
     * @return the id of this _setup.
     */
    public int getId() {
        return _id;
    }

    /**
     * @return the id of this _setup in string format.
     */
    public String getIDStr() {
        return String.valueOf(_id);
    }

}// inner class Scenario
