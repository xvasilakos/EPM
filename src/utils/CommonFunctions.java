package utils;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import sim.ISimulationMember;
import sim.space.cell.smallcell.SmallCell;

/**
 *
 * @author Xenofon Vasilakos xvas@aueb.gr
 */
public final class CommonFunctions extends utilities.CommonFunctions {

    private static final double GBPS_FACTOR = Math.pow(10, 9) / 8;
    private static final double MBPS_FACTOR = Math.pow(10, 6) / 8;
    private static final double KBPS_FACTOR = Math.pow(10, 3) / 8;
    private static final double BPS_FACTOR = 1 / 8;

    public static double[] doubleArray(Double[] array) {
        double[] _doubleArray = new double[array.length];
        for (int i = 0; i < _doubleArray.length; i++) {
            _doubleArray[i] = array[i];
        }
        return _doubleArray;
    }

    public static String toString(Collection objs) {
        return toString("", objs);
    }

    public static String toString(Map map) {
        StringBuilder sb = new StringBuilder("\n=== MAP ===\n{\n");

        for (Iterator iterator = map.keySet().iterator(); iterator.hasNext();) {
            Object k = iterator.next();
            sb.append("\t").
                    append("\"").
                    append(k.toString()).
                    append("\"").
                    append(" --> ");
            sb.append("\"").
                    append(map.get(k).toString()).
                    append("\"\n");
        }
        return sb.toString();
    }

    public static String toSynopsisString(Collection<ISynopsisString> objs) {
        return toSynopsisString("", objs);
    }

    public static String toString(String nesting, Collection objs) {
        return toString(nesting, "\n\t{\n\t", "\n\t}", ",\n\t", objs.toArray(new Object[objs.size()]));
    }

    public static String toSynopsisString(String nesting, Collection<ISynopsisString> objs) {
        return toSynopsisString(nesting, "\n\t{\n\t", "\n\t}", ",\n\t", objs);
    }

   

    public static String toString(Object... objs) {
        return toString("", objs);
    }

    public static String toString(String nesting, Object... objs) {
        return toString(nesting, "\n\t{\n\t", "\n\t}", ",\n\t", objs);
    }

    public static String toString(String nesting, String prefix, String suffix, String separator, Object... objs) {
        if (objs.length == 0) {
            return prefix + suffix;
        }
        StringBuilder _toString = new StringBuilder(nesting + prefix);

        int i = 0;
        for (Object nxtObj : objs) {

            _toString.append(nesting).append(nesting);
            if (nxtObj instanceof ISynopsisString) {
                _toString.append(nxtObj == null ? "null" : ((ISynopsisString) nxtObj).toSynopsisString());
            } else {
                _toString.append(nxtObj == null ? "null" : nxtObj.toString());
            }
            _toString.append(nesting);
            if (++i < objs.length) {// if it has next, add the separator
                _toString.append(separator);
            }
        }
        return _toString.append(nesting).append(suffix).toString();
    }

    public static String toSynopsis(String nesting, String prefix, String suffix, String separator, Collection objs) {
        if (!objs.iterator().hasNext()) {
            return prefix + suffix;
        }
        StringBuilder _toString = new StringBuilder(nesting + prefix);

        int i = 0;
        for (Iterator it = objs.iterator(); it.hasNext();) {
            Object nxtObj = it.next();
            _toString.append(nesting).append(nesting);
            _toString.append(nxtObj == null ? "null" : nxtObj.toString());
            _toString.append(nesting);
            if (it.hasNext()) {// if it has next, add the separator
                _toString.append(separator);
            }
        }
        return _toString.append(nesting).append(suffix).toString();
    }

    public static String toSynopsisString(String nesting, String prefix, String suffix, String separator, Collection<ISynopsisString> objs) {
        if (!objs.iterator().hasNext()) {
            return prefix + suffix;
        }
        StringBuilder _toSynopsisString = new StringBuilder(nesting + prefix);

        int i = 0;
        for (Iterator<ISynopsisString> it = objs.iterator(); it.hasNext();) {
            ISynopsisString nxtObj = it.next();
            _toSynopsisString.append(nesting).append(nesting);
            _toSynopsisString.append(nxtObj == null ? "null" : nxtObj.toSynopsisString());
            _toSynopsisString.append(nesting);
            if (it.hasNext()) {// if it has next, add the separator
                _toSynopsisString.append(separator);
            }
        }
        return _toSynopsisString.append(nesting).append(suffix).toString();
    }

    public static String toString(double[] dbls) {
        return toString("", "{", "}", ", ", dbls);
    }

    public static String toString(String nesting, String prefix, String suffix, String separator, double... doubles) {
        if (doubles.length == 0) {
            return prefix + suffix;
        }
        StringBuilder _toString = new StringBuilder(nesting + prefix);
        for (int i = 0; i < doubles.length; i++) {
            Object nxt_obj = doubles[i];
            _toString.append(nesting).append(nesting).append(nxt_obj.toString()).append(nesting);
            if (i + 1 < doubles.length) {// if it has next, add the separator
                _toString.append(separator);
            }
        }
        return _toString.append(nesting).append(suffix).toString();
    }

    public static String toString(String nesting, String prefix, String suffix, String separator, Collection objs) {
        if (objs.isEmpty()) {
            return prefix + suffix;
        }
        StringBuilder _toString = new StringBuilder(nesting + prefix);
        for (Iterator it = objs.iterator(); it.hasNext();) {
            Object nxt_obj = it.next();
            _toString.append(nesting).append(nesting).append(nxt_obj.toString()).append(nesting);
            if (it.hasNext()) {
                _toString.append(separator);
            }
        }
        return _toString.append(nesting).append(suffix).toString();
    }

    public static Set<String> extractKeys(Set<Map.Entry<String, String>> entries) {
        Set<String> _extractKeys = new HashSet<>();
        for (Map.Entry<String, String> entry : entries) {
            _extractKeys.add(entry.getKey());
        }
        return _extractKeys;
    }

    public static Set<String> extractValues(Set<Map.Entry<String, String>> entries) {
        Set<String> _extractValues = new HashSet<>();
        for (Map.Entry<String, String> entry : entries) {
            _extractValues.add(entry.getValue());
        }
        return _extractValues;
    }

    /**
     * If the value is float or double, then it is rounded to the roundDecimal.
     * Otherwise the value is returned intact
     *
     * @param n
     * @param roundDecimal
     * @return the value rounded to a decimal
     */
    public static Number roundNumber(Number n, int roundDecimal) {
        if (n instanceof Double) {
            Double d = (Double) n;
            int rounded = (int) (d.doubleValue() * Math.pow(10, roundDecimal));
            return rounded / Math.pow(10, roundDecimal);
        }
        if (n instanceof Float) {
            Float d = (Float) n;
            int rounded = (int) (d.doubleValue() * Math.pow(10, roundDecimal));
            return rounded / Math.pow(10, roundDecimal);
        }

        return n;
    }

    public static int countOccurrences(String word, char occurChar) {
        int count = 0;
        for (int i = 0; i < word.length(); i++) {
            if (word.charAt(i) == occurChar) {
                count++;
            }
        }
        return count;
    }

    public static String combineCellMUGroup(SmallCell cell, int grpID) {
        String cellID = "cell=" + cell.getID();
        String grpIDStr = "grp=" + grpID;
        return cellID + ":" + grpIDStr;
    }

    /**
     * *
     * Parses and returns the number of corresponding bytes
     *
     * @param str
     * @return
     */
    public static long parseSizeToBytes(String str) {
        if (str.toLowerCase().endsWith("gbps")) {
            String substring = str.substring(0, str.length() - 4);
            double tmp = Double.parseDouble(substring);
            return (long) (tmp * GBPS_FACTOR);
        } else if (str.toLowerCase().endsWith("mbps")) {
            String substring = str.substring(0, str.length() - 4);
            double tmp = Double.parseDouble(substring);
            return (long) (tmp * MBPS_FACTOR);
        } else if (str.toLowerCase().endsWith("kbps")) {
            String substring = str.substring(0, str.length() - 4);
            double tmp = Double.parseDouble(substring);
            return (long) (tmp * KBPS_FACTOR);
        } else if (str.toLowerCase().endsWith("bps")) {
            String substring = str.substring(0, str.length() - 3);
            double tmp = Double.parseDouble(substring);
            return (long) (tmp * BPS_FACTOR);// 125 bytes <=> 1Mbps
        } else if (str.endsWith("B") || str.endsWith("b")) {
            String substring = str.substring(0, str.length() - 1);
            double tmp = Double.parseDouble(substring);
            return (long) (tmp);
        } else if (str.endsWith("KB") || str.endsWith("kb")) {
            String substring = str.substring(0, str.length() - 2);
            Double tmp = Double.parseDouble(substring);
            return (long) (1024 * tmp);
        } else if (str.endsWith("MB") || str.endsWith("mb")) {
            String substring = str.substring(0, str.length() - 2);
            Double tmp = Double.parseDouble(substring);
            return (long) (Math.pow(1024, 2) * tmp);
        } else if (str.endsWith("GB") || str.endsWith("gb")) {
            String substring = str.substring(0, str.length() - 2);
            Double tmp = Double.parseDouble(substring);
            return (long) (Math.pow(1024, 3) * tmp);
        } else if (str.endsWith("TB") || str.endsWith("tb")) {
            String substring = str.substring(0, str.length() - 2);
            Double tmp = Double.parseDouble(substring);
            return (long) (Math.pow(1024, 4) * tmp);
        } else {
            return (int) Double.parseDouble(str);
        }
    }

    public static Logger getLoggerFor(ISimulationMember iSimClass) {
        return utilities.CommonFunctions.getLoggerFor(iSimClass.getClass(), "simID=" + iSimClass.simID());
    }

    private CommonFunctions() {
        super();
    }

}
