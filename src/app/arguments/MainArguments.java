package app.arguments;

import app.properties.Preprocessor;
import exceptions.WrongOrImproperArgumentException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Xenofon Vasilakos xvas@aueb.gr
 */
public class MainArguments {

    public static String replaceAllTags(String taggedPath) {

        return Defaults.replaceAllTagsWithPaths(taggedPath);

    }

    public static boolean containsSomeTag(String str) {
        return str.contains((CharSequence) Defaults.INSTALLATION_PATH_TAG)
                || str.contains((CharSequence) Defaults.FILES_TAG)
                || str.contains((CharSequence) Defaults.SIMCORE_TAG)
                || str.contains((CharSequence) Defaults.PROPS_PATH_TAG)
                || str.contains((CharSequence) Defaults.PROPS_MASTER__INI_TAG);
    }

    public static class Defaults {

        public static final String SYSTEM_SEP = System.getProperty("file.separator");

        public static final String INSTALLATION_PATH_TAG = "<INSTALLATION_PATH>";
        public static String INSTALLATION_PATH = xtr.InstallationDirPath.INSTALLATION_PATH;

        public static final String FILES_TAG = "<FILES>";
        public static final String FILES_PATH = INSTALLATION_PATH + "/files";

        public static final String SIMCORE_TAG = "<SIMCORE>";
        public static final String CORE_FILES_PATH = FILES_PATH + "/sim/core";

        public static final String PROPS_PATH_TAG = "<DEFAULT_PROPS>";
        public static final String PROPS_PATH = FILES_PATH + "/sim/core/default_properties";

        public static final String PROPS_MASTER__INI_TAG = "<DEFAULT_PROPS_MASTER__INI>";
        public static final String DEFAULT_PROPS_MASTER__INI_PATH = INSTALLATION_PATH + "/files/sim/core/default_properties/master.ini";

        public static String replaceAllTagsWithPaths(String taggedPAth) {
            return taggedPAth.
                    replaceAll(
                            MainArguments.Defaults.INSTALLATION_PATH_TAG,
                            MainArguments.Defaults.INSTALLATION_PATH
                    ).replaceAll(
                            MainArguments.Defaults.FILES_TAG,
                            MainArguments.Defaults.FILES_PATH
                    ).replaceAll(
                            MainArguments.Defaults.SIMCORE_TAG,
                            MainArguments.Defaults.CORE_FILES_PATH
                    ).replaceAll(MainArguments.Defaults.PROPS_PATH_TAG,
                            MainArguments.Defaults.PROPS_PATH
                    ).replaceAll(MainArguments.Defaults.PROPS_MASTER__INI_TAG,
                            MainArguments.Defaults.DEFAULT_PROPS_MASTER__INI_PATH
                    );
        }

        private Defaults() {// do not instanciate
        }

        private static final String ARGUMENTS__INI_CLASSPATH
                = "/" + MainArguments.class.getPackage().toString().substring(8).trim().replace('.', '/')
                + "/default_arg_values.ini";

        private static Properties loadFromProperties() {
            Properties loadedProps = new Properties();
            InputStream in = Preprocessor.class.getResourceAsStream(ARGUMENTS__INI_CLASSPATH);

            try {
                if (in == null) {
                    throw new IOException("Resource loaded from " + ARGUMENTS__INI_CLASSPATH + " is null");
                }
                loadedProps.load(in);
            } catch (IOException ioex) {
                String uponIOEX_msg = "Failed to load default properties file: " + ARGUMENTS__INI_CLASSPATH + "\n";
                LOG.severe(uponIOEX_msg);
            }
            return loadedProps;
        }
        private static final Properties PROPERTIES = loadFromProperties();

        public static final int PARALLEL = Integer.parseInt(PROPERTIES.getProperty("parallel"));
    }

    public interface ArgFlag {

    }

    public enum Flag implements ArgFlag {

        DEFAULT("--default", "-d"),
        VERBOSE("--verbose", "-v"),
        PROPERTIES_FULL_PATH("--properties_full_path", "-pp"),
        PARALLEL("--parallel", "-p"),
        D("-d", "--default"),
        V("-v", "--verbose"),
        PP("-pp", "--properties_full_path"),
        P("-p", "--parallel");
        private final String flag;
        private final String alternative;

        Flag(String flag, String alternative) {
            this.flag = flag;
            this.alternative = alternative;
        }

        public static boolean isValidFlag(String enumString) {

            try {
                Flag.valueOf(enumString);
            } catch (IllegalArgumentException e) {
                return false;
            }
            return true;
        }

        /**
         * @return the flag value, e.g: "--default"
         */
        @Override
        public String toString() {
            return this.flag;
        }

        public boolean equals(String _flag) {
            return this.flag.equals(_flag)
                    || this.alternative.equals(_flag);
        }

        public boolean equals(Flag _flag) {
            return this.flag.equals(_flag.flag)
                    || this.flag.equals(_flag.alternative);
        }
    }

    public enum MultiFlag implements ArgFlag {

        d("d"), v("v");
        private final String flag;

        MultiFlag(String _flag) {
            this.flag = _flag;
        }

        @Override
        public String toString() {
            return this.flag;
        }

        public boolean equals(String _flag) {
            return this.flag.equals(_flag);
        }

        public boolean equals(Flag _flag) {
            return _flag.equals(this);
        }

        public boolean equals(MultiFlag _flag) {
            return this.flag.equals(_flag.flag);
        }
    }

    private String propertiesPath;
    private int parallelSimsNum;

    private void defaults() {
        propertiesPath = Defaults.DEFAULT_PROPS_MASTER__INI_PATH;
        parallelSimsNum = Defaults.PARALLEL;
    }

    /**
     * uses default values.
     */
    public MainArguments() {
        defaults();
    }

    public static MainArguments load(String[] args) throws WrongOrImproperArgumentException {
        MainArguments loaded = new MainArguments(); // initially all defaults
        if (args.length == 0) {
            LOG.log(Level.WARNING, "Using default arguments:\n{0}\n", loaded.toString());
            return loaded;
        }

        int i = 0, j;
        char flag;

        while (i < args.length && args[i].startsWith("-")) {
            String nxtArg = args[i++];

            if (Flag.DEFAULT.equals(nxtArg) || Flag.D.equals(nxtArg)) {
                //<editor-fold defaultstate="collapsed" desc="default overides everything else">
                LOG.log(Level.WARNING, "Default arguments mode. "
                        + "Other arguments passed (if any) overriden by default values.\n");

                return new MainArguments();
                //</editor-fold>
            } else if (Flag.PROPERTIES_FULL_PATH.equals(nxtArg) || Flag.PP.equals(nxtArg)) {
                //<editor-fold defaultstate="collapsed" desc="handle properties input path">
                if (i < args.length) {
                    loaded.propertiesPath = MainArguments.replaceAllTags(args[i++]);

                } else {
                    String msg = "Argument \"" + nxtArg + "\" requires a path to a file";
                    throw new WrongOrImproperArgumentException(msg);
                }
                //</editor-fold>
            } else if (Flag.PARALLEL.equals(nxtArg) || Flag.P.equals(nxtArg)) {
                //<editor-fold defaultstate="collapsed" desc="handle properties input path">
                if (i < args.length) {
                    loaded.parallelSimsNum = Integer.valueOf(args[i++]);
                } else {
                    String msg = "Argument \"" + nxtArg + "\" requires a path to a file";
                    throw new WrongOrImproperArgumentException(msg);
                }
                //</editor-fold>
            } else {
                //<editor-fold defaultstate="collapsed" desc="Handle single letter options">
                for (j = 1;
                        j < nxtArg.length();
                        j++) {// start from 1 to jump the "-"
                    flag = nxtArg.charAt(j);

                    if (MultiFlag.d.equals(String.valueOf(flag))) {
                        LOG.log(Level.FINE, "Default arguments mode on.. any other arguments will be ignored\n");
                        return new MainArguments();
                    } else if (MultiFlag.v.equals(String.valueOf(flag))) {
                        LOG.setLevel(Level.ALL);
                        LOG.setLevel(Level.ALL);
                        LOG.log(Level.INFO, "verbose mode on\n");
                    } else {
                        String msg = "Argument \"" + nxtArg + "\":  illegal option or argument passed\n";
                        throw new WrongOrImproperArgumentException(msg);
                    }
                }
                //</editor-fold>
            }
        }//while
        return loaded;
    }
    private static final Logger LOG = Logger.getLogger(MainArguments.class.getName());

    @Override
    public String toString() {
        StringBuilder toReturn = new StringBuilder();
        String name = null;
        Object value = null;

        toReturn.append("<");
        Field[] fields = getClass().getDeclaredFields();
        try {
            for (int index = 0; index < fields.length; index++) {
                fields[index].setAccessible(true);// print also static values
                name = fields[index].getName();
                value = fields[index].get(this);

                toReturn.append('(').append(name);
                toReturn.append(':');
                toReturn.append(value).append(')');
                if (index < fields.length - 1) {
                    toReturn.append(';');
                }
            }
        } catch (IllegalArgumentException | IllegalAccessException ex) {
            // ignore.. :(
            value = "Cannot load value due to " + ex.getMessage();
            toReturn.append('(').append(name).append(':').append(value).append(')');
        }
        toReturn.append(">");
        return toReturn.toString();
    }

    public String getPropertiesPath() {
        return this.propertiesPath;
    }

    public File getPropertiesParentDir() {
        return (new File(this.propertiesPath)).getParentFile();
    }

    public String getPropertiesParent() {
        return (new File(this.propertiesPath)).getParent();
    }

    public int getMaxConcurrentWorkers() {
        return this.parallelSimsNum;
    }
}
