package app.arguments;

import app.properties.Preprocessor;
import exceptions.WrongOrImproperArgumentException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Properties;
import java.util.logging.Level;
import static logging.LoggersRegistry.CONSOLE_LOGGER;

/**
 *
 * @author Xenofon Vasilakos xvas@aueb.gr
 */
public class MainArguments {

    public static class Defaults {

        public static final String HOME = System.getProperty("user.home")
                + "/Dropbox/2014-2015-EPC+POP/trunk";

        public static final String DEFAULT_FILES_TAG = "<files>";
        public static final String FILES_PATH = HOME + "/files";

        public static final String DEFAULT_SIMCORE_TAG = "<simcore>";
        public static final String CORE_FILES_PATH = FILES_PATH + "/sim/core";

        public static final String DEFAULT_PROPS_TAG = "<defaultprops>";
        public static final String PROPERTIES_DIR_PATH = CORE_FILES_PATH + "/properties";

        public static final String HOME_TAG = "<home>";

        private Defaults() {// do not instanciate
        }

        private static final String ARGUMENTS__INI_CLASSPATH
                = "/" + MainArguments.class.getPackage().toString().substring(8).trim().replace('.', '/')
                + "/default_arg_values.ini";

        public static final String DEFAULT_PROPERTIES__INI_CLASSPATH = "app/properties/default_property.values.ini";

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
                CONSOLE_LOGGER.severe(uponIOEX_msg);
            }
            return loadedProps;
        }
        private static final Properties PROPERTIES = loadFromProperties();

        public static final int PARALLEL = Integer.parseInt(PROPERTIES.getProperty("parallel"));
    }

    public interface ArgFlag {
    }

    public enum Flag implements ArgFlag {

        DEFAULT("-default") {
            public boolean equals(Flag_shortcut _flag) {
                return _flag.equals(Flag_shortcut.dash_d);
            }
        },
        /////////////////////
        VERBOSE("-verbose") {
            public boolean equals(Flag_shortcut _flag) {
                return _flag.equals(Flag_shortcut.dash_v);
            }
        },
        /////////////////////
        PROPERTIES_FULL_PATH("-properties_full_path") {
            public boolean equals(Flag_shortcut _flag) {
                return _flag.equals(Flag_shortcut.dash_pp);
            }
        },
        /////////////////////
        PARALLEL("-parallel") {
            public boolean equals(Flag_shortcut _flag) {
                return _flag.equals(Flag_shortcut.dash_p);
            }
        };
        private final String flag;

        Flag(String _flag) {
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
            return this.flag.equals(_flag.flag);
        }
    }

    public enum Flag_shortcut implements ArgFlag {

        dash_d("-d") {
            public boolean equals(Flag _flag) {
                return this.toString().equals(Flag.DEFAULT.toString());
            }
        }, d("d"),
        /////////////////////
        dash_v("-v") {
            public boolean equals(Flag _flag) {
                return this.toString().equals(Flag.VERBOSE.toString());
            }
        }, v("v"),
        /////////////////////
        dash_pp("-pp") {
            public boolean equals(Flag _flag) {
                return this.toString().equals(Flag.PROPERTIES_FULL_PATH.toString());
            }
        }, pp("pp"),
        /////////////////////
        dash_p("-p") {
            public boolean equals(Flag _flag) {
                return this.toString().equals(Flag.PARALLEL.toString());
            }
        }, p("p");
        private final String flag;

        Flag_shortcut(String _flag) {
            this.flag = _flag;
        }

        @Override
        public String toString() {
            return this.flag;
        }

        public boolean equals(String _flag) {
            return this.flag.equals(_flag);
        }

        public boolean equals(Flag_shortcut _flag) {
            return this.flag.equals(_flag.flag);
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
        propertiesPath = Defaults.DEFAULT_PROPERTIES__INI_CLASSPATH;
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
            CONSOLE_LOGGER.log(Level.WARNING, "Using default arguments:\n{0}\n", loaded.toString());
            return loaded;
        }

        int i = 0, j;
        char flag;

        while (i < args.length && args[i].startsWith("-")) {
            String nxtArg = args[i++];

            if (Flag.DEFAULT.equals(nxtArg) || Flag_shortcut.dash_d.equals(nxtArg)) {
                //<editor-fold defaultstate="collapsed" desc="default overides everything else">
                CONSOLE_LOGGER.log(Level.WARNING, "Default arguments mode. "
                        + "Other arguments passed (if any) overriden by default values.\n");

                return new MainArguments();
                //</editor-fold>
            } 
            else if (Flag.PROPERTIES_FULL_PATH.equals(nxtArg)){
                //<editor-fold defaultstate="collapsed" desc="handle properties input path">
                if (i < args.length) {
                    loaded.propertiesPath = args[i++];
                } else {
                    String msg = "Argument \"" + nxtArg + "\" requires a path to a file";
                    throw new WrongOrImproperArgumentException(msg);
                }
                //</editor-fold>
            } 
            else if (Flag_shortcut.dash_pp.equals(nxtArg)) {
                //<editor-fold defaultstate="collapsed" desc="handle properties input path">
                if (i < args.length) {
                    loaded.propertiesPath = 
                            Defaults.PROPERTIES_DIR_PATH
//                            System.getProperty("user.home")+ "/Dropbox"
                                    + "/" + args[i++];
                } else {
                    String msg = "Argument \"" + nxtArg + "\" requires a path to a file";
                    throw new WrongOrImproperArgumentException(msg);
                }
                //</editor-fold>
            } 
            else if (Flag.PARALLEL.equals(nxtArg) || Flag_shortcut.dash_p.equals(nxtArg)) {
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
                        CONSOLE_LOGGER.log(Level.FINE, "Default arguments mode on.. any other arguments will be ignored\n");
                        return new MainArguments();
                    } else if (MultiFlag.v.equals(String.valueOf(flag))) {
                        CONSOLE_LOGGER.setLevel(Level.ALL);
                        CONSOLE_LOGGER.setLevel(Level.ALL);
                        CONSOLE_LOGGER.log(Level.INFO, "verbose mode on\n");
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
