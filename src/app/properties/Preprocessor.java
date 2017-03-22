package app.properties;

import app.arguments.MainArguments;
import app.properties.valid.Values;
import exceptions.CriticalFailureException;
import exceptions.InconsistencyException;
import exceptions.InvalidOrUnsupportedException;
import exceptions.WrongOrImproperArgumentException;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import utilities.Couple;

/**
 * ";" is used for multiple values of the same property for scheduling multiple
 * simulation setups. "," is used for separating a list of multiple values for
 * the same setup, e.g. "0.5,0.7" are a set of 2 different probabilities, while
 * "0.5,0.7;0.455,0.275;0.25,0.1" is set of three different setups of 2 values
 * each.
 *
 * @author Xenofon Vasilakos xvas@aueb.gr
 */
public final class Preprocessor {

    /**
     * The root properties file path as specified in the main() arguments, which
     * is loaded by static method process()
     */
    public static String USER_PASSED_PROPS_PATH;

    /**
     * Used to ensure that same file is not recursively loaded, which would lead
     * in an endless loop of loading the same file
     */
    private final Set<File> _includedFiles;

    private static final Preprocessor _singleton = new Preprocessor();
    /**
     * key=value from properties file after discarding all comments (both after
     * values and stand-alone comments in
     */
    private final SortedMap<String, String> rawPrps = new TreeMap<>();

    private Preprocessor() {
        _includedFiles = new HashSet<File>();
    }

    public static Preprocessor defaultPreprocessor() {
        return _singleton;
    }

    /**
     * Performs the initial processing of a properties file. Calling this method
     * discards the result of a previous invocation.
     *
     * @param thePath the path to the properties file
     *
     *
     * @return the default Preprocessor instance after performing the processing
     * of the properties file.
     */
    public static Preprocessor process(String thePath)
            throws CriticalFailureException {
        USER_PASSED_PROPS_PATH = thePath;
        boolean customPathUsed = true;
        try {
            thePath = MainArguments.replaceAllTags(thePath);

            _singleton.loadNameValuesPairs(thePath);
        } catch (IOException | WrongOrImproperArgumentException e) {
            LOG.log(Level.SEVERE, "\n", e);
            throw new CriticalFailureException(e);
        }

        //<editor-fold defaultstate="collapsed" desc="logging">
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PrintStream printStream = new PrintStream(baos)) {
            try {
                _singleton.list(printStream);
            } catch (InconsistencyException ex) {
                // impossible because this method initilizes the singleton..
            } finally {
                LOG.log(Level.FINER, "The list of {0}  loaded\n\n{1}\n\n",
                        new Object[]{
                            (customPathUsed ? "custom" : "default"),
                            new String(baos.toByteArray())
                        });
            }
        }
//</editor-fold>

        return _singleton;
    }

    /**
     * Loads key=value in map of raw entries by discarding all comments (both
     * after values and stand-alone comments in different lines).
     *
     * If the same key is found later in parsing, a warning is logged and the
     * latest value(s) are kept, while previously encountered name={value;value
     * ..} pairs are neglected. This applies also to pairs included from other
     * property files.
     *
     * inStream
     *
     * IOException
     */
    private void loadNameValuesPairs(String pth) throws IOException, WrongOrImproperArgumentException {

        //@todo ocnsider if these lines not need (..?)
//        int sep = pth.lastIndexOf("/");
//        if (sep == -1) {
//            sep = pth.lastIndexOf("\\");
//        }
//
//        // case of flags: -d or --default
//        if (sep != -1) {
//            String flagstr = pth.substring(sep + 1);
//
//            if (flagstr.startsWith("--")) {
//                flagstr = flagstr.substring(2).toUpperCase();
//            } else if (flagstr.startsWith("-")) {
//                flagstr = flagstr.substring(1).toUpperCase();
//            }
//
//            if (MainArguments.Flag.isValidFlag(flagstr)) {
//                MainArguments.Flag theFlag = MainArguments.Flag.valueOf(flagstr);
//                if (theFlag == MainArguments.Flag.DEFAULT
//                        || theFlag == MainArguments.Flag.D) {
//                    pth = pth.replace(theFlag.toString(),
//                            MainArguments.Defaults.DEFAULT_PROPS_MASTER__INI_PATH);
//                }
//
//            }
//        }
        File pFile = new File(pth);

        FileInputStream inStream = new FileInputStream(pth);

        String line;
        int lineCounter = 0;
        BufferedReader readBf = new BufferedReader(new InputStreamReader(inStream));
        while ((line = readBf.readLine()) != null) {
            line = line.trim();
            lineCounter++;
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            if (line.startsWith(Values.INCLUDE)) {
                StringTokenizer comm_tok = new StringTokenizer(line, "#");
                if (comm_tok.hasMoreTokens()) {
                    line = comm_tok.nextToken(); // use the part before comments
                }
                String includedPaths = line.substring(Values.INCLUDE.length());
                StringTokenizer tok = new StringTokenizer(includedPaths, ";");
                while (tok.hasMoreTokens()) {
                    String nxtPth = tok.nextToken().trim();

                    nxtPth = MainArguments.replaceAllTags(nxtPth);

                    if (nxtPth.startsWith("./")) {

                        File pfileParent = pFile.getParentFile();
//                        nxtPth = pfileParent.getAbsolutePath() + "/" + nxtPth.substring(2);
                        File includedFile = null;

                        includedFile = new File(pfileParent, nxtPth.substring(2));

                        loadFromIncludedFile(includedFile);
                        continue;
                    }

                    if (nxtPth.startsWith("../")) {
                        File pfileGParent = pFile.getParentFile().getParentFile();
//                        nxtPth = pfileGParent.getAbsolutePath() + "/" + nxtPth.substring(2);
                        File includedFile = new File(pfileGParent, nxtPth.substring(2));
                        loadFromIncludedFile(includedFile);
                        continue;
                    }

                    File includedFile = new File(nxtPth);
                    loadFromIncludedFile(includedFile);
                }

                continue;
            }

            StringTokenizer commTok = new StringTokenizer(line, "#");
            if (commTok.hasMoreTokens()) {
                line = commTok.nextToken(); // use the part before comments
            }

            int eqIdx = line.indexOf("=");

            if (eqIdx == -1) {
                throw new InconsistencyException("Non valid entry: " + line
                        + " found on line #" + lineCounter
                        + "\n at path: " + pth);
            }
            String key = line.substring(0, eqIdx).trim();
            String val = line.substring(eqIdx + 1).trim();
            String prvIgnored = rawPrps.put(key, val.equals("\"\"") ? "" : val);

            if (prvIgnored != null) {
                LOG.log(Level.WARNING, "Property {0} in file \"{3}\" overriden: "
                        + " Previous value(s) = {1} was (were) replaced by: {2}",
                        new Object[]{key, prvIgnored, val, pth});
            }
        }//while
        replaceRefsToOtherProps();

    }

    /**
     * Replaces references to other properties, marked within "%", e.g.
     * %ref.to.other.prop%. The only references left intact, are the ones which
     * include one or more colons ";", i.e. the ones that have multiple values,
     * as that could cause inconsistencies. For instance: a=a1;a2;a3 yields
     * three scenarios. If b=%a% gets replaced by a1;a2;a3, then that would
     * result in 3 x 3 = 9 scenarios.
     *
     * {@literal @note} that #replaceForceRefsToOtherProps() allows such
     * scenarios, only it is invoked when {@literal "<"  and ">" are used, e.g. "b=<a>"
     * }
     *
     * @throws WrongOrImproperArgumentException
     * @throws IOException
     */
    private void replaceRefsToOtherProps() throws WrongOrImproperArgumentException, IOException {
        for (Map.Entry<String, String> entry : rawPrps.entrySet()) {
            String key = entry.getKey();
            String original = entry.getValue();
            String val = entry.getValue();

            if (val.contains(";")) {
                continue;
            }

            // whatever between two consequtive "%", try to see if it refers to another property value
            Pattern p = Pattern.compile("%([^%]*)%");
            Matcher m = p.matcher(val);

            int countLim = 100;
            while (m.find() && countLim-- > 0) {
//                System.err.println("press enter");
//                System.in.read();

                String tmp = "%" + m.group(1) + "%";

                val = val.replace(tmp, rawPrps.get(m.group(1)));
//                System.err.print("replacing \"");
//                System.err.print(tmp);
//                System.err.print("\" in " + key + "->" + rawPrps.get(key));
//                System.err.println(" with \"");
//                System.err.print(rawPrps.get(m.group(1)));
//                System.err.println("\"");
                rawPrps.put(key, val);
            }
            if (countLim == 0) {
                throw new exceptions.WrongOrImproperArgumentException(
                        "Parameter "
                        + "\""
                        + original
                        + "\""
                        + " suspected to cause cyclic references between property values."
                );
            }

        }
    }

    /**
     * Replaces references to other properties, marked within
     * {@literal "<" and ">".} Unlike #replaceRefsToOtherProps(), this method
     * allows to replace with multiple values. For instance: a=a1;a2;a3 yields
     * three scenarios. If b=%a% gets replaced by a1;a2;a3, it will result in 3
     * x 3 = 9 scenarios.
     *
     * @throws WrongOrImproperArgumentException
     * @throws IOException
     */
    private void replaceForceRefsToOtherProps() throws WrongOrImproperArgumentException, IOException {
        for (Map.Entry<String, String> entry : rawPrps.entrySet()) {
            String key = entry.getKey();
            String original = entry.getValue();
            String val = entry.getValue();

            // whatever between two consequtive "%", try to see if it refers to another property value
            Pattern p = Pattern.compile("%([^%]*)%");
            Matcher m = p.matcher(val);

            int countLim = 100;
            while (m.find() && countLim-- > 0) {
//                System.err.println("press enter");
//                System.in.read();

                String tmp = "%" + m.group(1) + "%";

                val = val.replace(tmp, rawPrps.get(m.group(1)));
//                System.err.print("replacing \"");
//                System.err.print(tmp);
//                System.err.print("\" in " + key + "->" + rawPrps.get(key));
//                System.err.println(" with \"");
//                System.err.print(rawPrps.get(m.group(1)));
//                System.err.println("\"");
                rawPrps.put(key, val);
            }
            if (countLim == 0) {
                throw new exceptions.WrongOrImproperArgumentException(
                        "Parameter "
                        + "\""
                        + original
                        + "\""
                        + " suspected to cause cyclic references between property values."
                );
            }

        }
    }
    private static final Logger LOG = Logger.getLogger(Preprocessor.class.getName());

    private void loadFromIncludedFile(File includedFile) throws IOException, WrongOrImproperArgumentException {
        if (!_includedFiles.contains(includedFile)) {
            _includedFiles.add(includedFile);
        } else {
            throw new IOException(
                    "Cyclic reference to roperties file "
                    + "\""
                    + includedFile.getCanonicalPath()
                    + "\"");
        }

        loadNameValuesPairs(includedFile.getCanonicalPath());
    }

    public void list(PrintStream printStream) {
        printStream.append(toString());
    }

    public Set<String> getPropertyNames() {
        return rawPrps.keySet();
    }

    public Set<Map.Entry<String, String>> getEntries() {
        return rawPrps.entrySet();
    }

    public void print(PrintStream printStream) {
        printStream.print(toString());
    }

    @Override
    public String toString() {
        StringBuilder _toString = new StringBuilder();
        _toString.append(Preprocessor.class.getSimpleName()).append(':');
        _toString.append("Listing raw \"type => property:=value\", including comments: ");

        Iterator<Map.Entry<String, String>> iter_entries = rawPrps.entrySet().iterator();
        while (iter_entries.hasNext()) {
            Map.Entry<String, String> entry = iter_entries.next();

            String propName = entry.getKey();
            _toString.append("\n\t");
            _toString.append(Registry.getTypeOf(propName));
            _toString.append("=> ");
            _toString.append(propName);
            _toString.append(":=");

            String propVal = entry.getValue();
            _toString.append(propVal);
            _toString.append("");
        }
        return _toString.toString();
    }

    /**
     * property
     *
     * @param property
     * @return the (possibly multiple) values for this property, coupled by
     * comments (optionally) following property values
     * InvalidOrUnsupportedException
     * @throws exceptions.InvalidOrUnsupportedException
     */
    public Couple<List<String>, String> getValues(IProperty property)
            throws InvalidOrUnsupportedException, InconsistencyException {
        return getValues(property.propertyName());
    }

    /**
     * propName
     *
     * @param propName
     * @return the (possibly multiple) values for this property name, coupled by
     * the optional comments following property values
     * InvalidOrUnsupportedException
     * @throws exceptions.InvalidOrUnsupportedException
     */
    public Couple<List<String>, String> getValues(String propName)
            throws InvalidOrUnsupportedException {

        ArrayList<String> _propertyValueList = new ArrayList<>();
        String rawValues_comments = rawPrps.get(propName);
        if (rawValues_comments == null) {
            throw new InvalidOrUnsupportedException("No such key loaded: " + propName);
        }

        int commentedOutPos = rawValues_comments.indexOf("#");
        String comment = commentedOutPos != -1
                ? rawValues_comments.substring(commentedOutPos)
                : "";
        String values_nonTokenized = commentedOutPos != -1
                ? rawValues_comments.substring(0, commentedOutPos)
                : rawValues_comments;

        StringTokenizer values_nonTokenizer = new StringTokenizer(
                values_nonTokenized, Values.SETUP_SEPARATOR);
        while (values_nonTokenizer.hasMoreTokens()) {
            String value = values_nonTokenizer.nextToken();
            _propertyValueList.add(value);
        }
        return new Couple(_propertyValueList, comment);
    }

    /**
     * statsProperty
     *
     * @param statsProperty
     * @return the first value if according to the current scenarios setups this
     * is a multiple values property, or if this is a list property. Note that
     * the value returned is in string format.
     * epc.femto.exceptions.InvalidOrUnsupportedException
     * @throws exceptions.InvalidOrUnsupportedException
     */
    public String getFirstValue(StatsProperty statsProperty) throws InvalidOrUnsupportedException {
        return getValues(statsProperty).getFirst().get(0);
    }

    /**
     * statsProperty
     *
     * @param statsProperty
     * @return the first value if according to the current scenarios setups this
     * is a multiple values property, or if this is a list property. Note that
     * the value returned is in string format.
     * epc.femto.exceptions.InvalidOrUnsupportedException
     * @throws exceptions.InvalidOrUnsupportedException
     */
    public String getFirstValue(String statsProperty) throws InvalidOrUnsupportedException {
        return getValues(statsProperty).getFirst().get(0);
    }

    public String getUSER_PASSED_PROPS_PATH() {
        return USER_PASSED_PROPS_PATH;
    }

}
