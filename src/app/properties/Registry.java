package app.properties;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;
import static logging.LoggersRegistry.PROPERTIES_LOGGER;

/**
 *
 * @author xvas
 */
public final class Registry {

   /**
    * Maps property names to property types.
    */
   private static final Map<String, String> PROPERTIES_TO_TYPES_MAP = new TreeMap<>();
   /**
    * Set of property names to be used for the output results file.
    */
   private static final Set<String> PROPS_FOR_RESULTS_FILENAME = new TreeSet<>();
   /**
    * Set of property names to be used for the output results directory.
    */
   private static final Set<String> PROPS_FOR_RESULTS_DIRNAME = new TreeSet<>();
   /**
    * Maps property names to abbreviated property names.
    */
   private static final Map<String, String> PROPERTIES_TO_ABBREVIATIONS_MAP = new TreeMap<>();
   //<editor-fold defaultstate="collapsed" desc="initilizing maps">

   static {
      loadPropTypesMap("/"
            + Registry.class.getPackage().toString().substring(8).trim().replace('.', '/')
            + "/resources/props.types.ini"
      );
      
      loadPropsAbbrevationsMap("/"
            + Registry.class.getPackage().toString().substring(8).trim().replace('.', '/')
            + "/resources/props.abbreviations.ini"
      );

      loadProps4ResultFiles("/"
            + Registry.class.getPackage().toString().substring(8).trim().replace('.', '/')
            + "/resources/props.results.filenames.ini"
      );

      loadProps4ResultDirectories("/"
            + Registry.class.getPackage().toString().substring(8).trim().replace('.', '/')
            + "/resources/props.results.dirnames.ini"
      );
   }

   public static void loadPropTypesMap(String path) {

      Properties props = new Properties();
      PROPERTIES_LOGGER.log(Level.INFO, "Loading from {0}.", path);
      InputStream in = Preprocessor.class.getResourceAsStream(path);

      try {
         if (in == null) {
            throw new IOException("Resource loaded from " + path + " is null");
         }
         props.load(in);
      } catch (IOException ioex) {
         String uponIOEX_msg = "Failed to load default properties file: " + path + "\n";
         PROPERTIES_LOGGER.log(Level.SEVERE, uponIOEX_msg, ioex);
         System.exit(-1000);
      }

      Set<Object> keySet = props.keySet();
      for (Iterator iter_keySet = keySet.iterator(); iter_keySet.hasNext();) {
         String nxtKey = ((String) iter_keySet.next()).trim();
         String type = props.getProperty(nxtKey).trim();
         PROPERTIES_TO_TYPES_MAP.put(nxtKey, type);
      }
   }
    public static void loadPropsAbbrevationsMap(String path) {

        Properties props = new Properties();
        PROPERTIES_LOGGER.log(Level.INFO, "Loading from {0}.", path);
        InputStream in = Preprocessor.class.getResourceAsStream(path);

        try {
            if (in == null) {
                throw new IOException("Resource loaded from " + path + " is null");
            }
            props.load(in);
        } catch (IOException ioex) {
            String uponIOEX_msg = "Failed to load default properties file: " + path + "\n";
            PROPERTIES_LOGGER.log(Level.SEVERE, uponIOEX_msg, ioex);
            System.exit(-1000);
        }

        Set<Object> keySet = props.keySet();
        for (Iterator iter_keySet = keySet.iterator(); iter_keySet.hasNext();) {
            String nxtKey = ((String) iter_keySet.next()).trim();
            String abbrv = props.getProperty(nxtKey).trim();
            PROPERTIES_TO_ABBREVIATIONS_MAP.put(nxtKey, abbrv);
        }
    }

   public static void loadProps4ResultFiles(String path) {

      Properties props = new Properties();
      PROPERTIES_LOGGER.log(Level.INFO, "Loading from {0}.", path);
      InputStream in = Preprocessor.class.getResourceAsStream(path);

      try {
         if (in == null) {
            throw new IOException("Resource loaded from " + path + " is null");
         }
         props.load(in);
      } catch (IOException ioex) {
         String uponIOEX_msg = "Failed to load default properties file: " + path + "\n";
         PROPERTIES_LOGGER.log(Level.SEVERE, uponIOEX_msg, ioex);
         System.exit(-1000);
      }

      Set<Object> keySet = props.keySet();
      for (Iterator iter_keySet = keySet.iterator(); iter_keySet.hasNext();) {
         String nxtKey = ((String) iter_keySet.next()).trim();
         PROPS_FOR_RESULTS_FILENAME.add(nxtKey);
      }
   }

   public static void loadProps4ResultDirectories(String path) {

      Properties props = new Properties();
      PROPERTIES_LOGGER.log(Level.INFO, "Loading from {0}.", path);
      InputStream in = Preprocessor.class.getResourceAsStream(path);

      try {
         if (in == null) {
            throw new IOException("Resource loaded from " + path + " is null");
         }
         props.load(in);
      } catch (IOException ioex) {
         String uponIOEX_msg = "Failed to load default properties file: " + path + "\n";
         PROPERTIES_LOGGER.log(Level.SEVERE, uponIOEX_msg, ioex);
         System.exit(-1000);
      }

      Set<Object> keySet = props.keySet();
      for (Iterator iter_keySet = keySet.iterator(); iter_keySet.hasNext();) {
         String nxtKey = ((String) iter_keySet.next()).trim();
         PROPS_FOR_RESULTS_DIRNAME.add(nxtKey);
      }
   }

 

   /**
    * @param prop the property
    * @return the the abbreviated property name of prop
    */
   public static String getAbbreviation(String prop) {
      return PROPERTIES_TO_ABBREVIATIONS_MAP.get(prop);
   }

   /**
    * Returns a boolean value denoting weather the property name and value must be used
    * for the output results file. Note that if not specified, the default value returned
    * is false.
    *
    * @param propName
    *
    * @return true if the property name and value must be used for the output results file
    * or false if otherwise or unspecified.
    */
   public static boolean printInFileName(String propName) {
      return PROPS_FOR_RESULTS_FILENAME.contains(propName);
   }

   /**
    * Returns a boolean value denoting weather the property name and value must be used
    * for the output results directory. Note that if not specified, the default value
    * returned is false.
    *
    * @param propName
    *
    * @return true if the property name and value must be used for the output results
    * directory or false if otherwise or unspecified.
    */
   public static boolean printInDirName(String propName) {
      return PROPS_FOR_RESULTS_DIRNAME.contains(propName);
   }

   /**
    * property types, plus a registry of property names to types
    */
   public enum Type {

      CUSTOM("custom"),
      INT("int"),
      STRING("string"),
      DOUBLE("double"),
      LIST_INT("list_int"),
      LIST_STRING("list_string"),
      LIST_DOUBLE("list_double");
      private String _type;

      private Type(String type) {
         _type = type;
      }

      @Override
      public String toString() {
         return _type;
      }

      public boolean equals(Type other) {
         return _type.equals(other._type);
      }
   }

   public static void printKnown(PrintStream pr) {
      for (Map.Entry<String, String> nxt_regEntry : PROPERTIES_TO_TYPES_MAP.entrySet()) {
         String propName = nxt_regEntry.getKey();
         String propType = nxt_regEntry.getValue();
         pr.println("<" + propName + ":" + propType + ">");
      }
   }

   public static void printKnown(OutputStream ous) {
      printKnown(new PrintStream(ous));
   }

   public static String getTypeOf(String propertyName) {
      return PROPERTIES_TO_TYPES_MAP.get(propertyName);
   }

   private Registry() {// no instances
   }
}
