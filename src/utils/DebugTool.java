package utils;

import app.SimulatorApp;
import app.arguments.MainArguments;
import caching.base.AbstractCachingModel;
import caching.incremental.EMC;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import sim.run.SimulationBaseRunner;
import sim.content.Chunk;
import sim.space.cell.CellRegistry;
import sim.space.cell.smallcell.SmallCell;
import sim.space.users.User;
import sim.space.users.mobile.MobileGroup;
import sim.space.users.mobile.MobileUser;

/**
 *
 * @author xvas
 */
public class DebugTool {

    private static HashSet<Chunk> set = new HashSet();

    public static void setAdd(Chunk c) {
        set.add(c);
    }

    public static boolean setContains(Chunk c) {
        return set.contains(c);
    }

    public static PrintStream printer;//yyy

    private static final int monitorCellID = -1;//  21;
    private static final String monitorUID = "-1";//  21;
    private static final AbstractCachingModel MODEL = EMC.instance();

    public static void init() {
        try {
            SimpleDateFormat sdfDate = new SimpleDateFormat("yy-MM-dd");
            SimpleDateFormat sdfTime = new SimpleDateFormat("HH-mm-ss");
            Date now = new Date();
            String name = sdfTime.format(now) + "runInfo.txt";

            String parent = MainArguments.Defaults.FILES_PATH + "/debugInfo/[" + sdfDate.format(now) + "]";
            new File(parent).mkdirs();

            printer = new PrintStream(parent + "/" + name);

        } catch (FileNotFoundException ex) {
            Logger.getLogger(DebugTool.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void flushLogs() {
        if (printer != null) {
            printer.flush();
        }
    }

    public static void close() {
        if (printer != null) {
            printer.close();
        }
    }

    public static void appendLogNewRecord(String txt, SmallCell sc, caching.base.AbstractCachingModel model) {
        if ((sc.getID() == monitorCellID) && model.getClass() == MODEL.getClass()) {
            printer.append("\n\n");
            printer.append("{simID=");
            printer.append("" + sc.getSimulation().getID());
            printer.append("}");

            printer.append("[simTime=" + sc.simTime() + "]");
            printer.append("\t" + txt);
        }
    }

    public static void appendLogNewRecord(SimulationBaseRunner theSim, String txt) {
        printer.append("[simTime=" + theSim.simTime() + "]");
        printer.append("\t" + txt);
    }

    public static void trackUser(boolean newRec, String txt, User usr, boolean overrideMU) {
        if (usr.getID().equals(monitorUID)
                || overrideMU) {

            printer.append("\n\n");
            printer.append("{simID=");
            printer.append("" + usr.getSimulation().getID());
            printer.append("}");

            if (newRec) {
                printer.append(usr.simTimeStr());
                printer.append(" user: " + usr.getID());//toSynopsisString());
                printer.append("\n");
            }
            printer.append(txt);
        }
    }

    public static void trackSC(boolean newRec, String txt, SmallCell sc,
            boolean overrideSC) {
        if (sc.getID() == monitorCellID || overrideSC) {

            printer.append("\n\n");
            printer.append("{simID=");
            printer.append("" + sc.getSimulation().getID());
            printer.append("}");

            if (newRec) {
                printer.append(sc.simTimeStr());
                printer.append("\n");
            }
            printer.append(txt);
        }
    }

    public static void trackSCOut(boolean newRec, String txt, SmallCell sc, boolean overrideSC) {
        if (sc.getID() == 13 || overrideSC) {
            try {
                System.out.append("\n");
                if (newRec) {
                    System.out.append(sc.simTimeStr());
                    System.out.append("\n");
                }
                System.out.append(txt);
                System.out.println();
                System.in.read();
            } catch (IOException ex) {

            }
        }

    }

    public static void append(String msg) {
        printer.append(msg);
    }

    public static void appendln(String msg) {
        printer.append("\n").append(msg);
    }

    public static void append(boolean check, String msg) {
        if (!check) {
            return;
        }
        printer.append(msg);
    }

    public static void appendln(boolean check, String msg) {
        if (!check) {
            return;
        }
        printer.append("\n").append(msg);
    }

    public static void appendln(int tabs, String msg) {
        printer.append("\n");
        for (int i = 0; i < tabs; i++) {
            printer.append("\t");
        }
        printer.append(msg);
    }

    public static void printProbs(MobileUser mu, CellRegistry r) {
        for (SmallCell s1 : r.getSmallCells()) {
            printer.append('\n');
            SortedSet<Double> probsSorted = new TreeSet(Collections.reverseOrder());
            for (SmallCell s2 : r.getSmallCells()) {
                double p = r.handoverProbability(mu, s1, s2);
                probsSorted.add(p);
            }
            int max = 5; //print the first 5 at most
            for (Double nxtP : probsSorted) {
                if (max-- == 0) {
                    return;
                }
                printer.append(nxtP + ", ");
            }
        }
    }


}
