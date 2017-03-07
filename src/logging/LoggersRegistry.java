package logging;

import app.properties.LoggingProperty;
import app.properties.Preprocessor;
import exceptions.InconsistencyException;
import exceptions.InvalidOrUnsupportedException;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Objects;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import sim.ISimulationMember;
import sim.Scenario;
import sim.run.SimulationBaseRunner;
import sim.space.cell.CellRegistry;

/**
 *
 * @author Xenofon Vasilakos xvas@aueb.gr
 */
public class LoggersRegistry implements ISimulationMember {

    private final SimulationBaseRunner simulation;
    /////////////////// FORMATERS ////////////////////
    private final static Formatter simpleFrmt = new SimpleFormatter();
    private final Formatter idFrmt;
    private static final Formatter rawFrmt = new Formatter() {
        @Override
        public String format(LogRecord lr) {
            return formatMessage(lr).toString();
        }
    };
    private static final Date time = Calendar.getInstance().getTime();
    private static final String date_yyMMdd_HHmmss = new SimpleDateFormat("yy-MM-dd_HH-mm-ss").format(time);
    // = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = Global Loggers = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = 
    //<editor-fold defaultstate="collapsed" desc="Global Loggers initilization">
    private static final ConsoleHandler CONSOLE_HANDLER;

    static {
        CONSOLE_HANDLER = new ConsoleHandler();
        CONSOLE_HANDLER.setLevel(Level.ALL);
    }
    /**
     * Logs to the console only. The level used is Level.ALL
     */
    public static final Logger CONSOLE_LOGGER;

    static {
        String name = date_yyMMdd_HHmmss + "_CONSOLE_LOGGER";
        CONSOLE_LOGGER = Logger.getLogger(name);
        CONSOLE_LOGGER.addHandler(CONSOLE_HANDLER);
        CONSOLE_LOGGER.setUseParentHandlers(false);
        CONSOLE_LOGGER.log(
                Level.INFO, "CONSOLE_LOGGER initilized with name {0} and level {1}\n", new Object[]{
                    CONSOLE_LOGGER.getName(), CONSOLE_LOGGER.getLevel()
                });
    }
//    public static final Logger CONSOLE_LOGGER = initGlobalLogger("CONSOLE_LOGGER");
    public final static Logger PROPERTIES_LOGGER = initGlobalLogger("PROPERTIES_LOGGER");

    private static Logger initGlobalLogger(String name) {
        Logger someLogger = Logger.getLogger(name);
        someLogger.addHandler(CONSOLE_HANDLER);
        someLogger.setLevel(Level.ALL);
        return someLogger;
    }
    //</editor-fold>
    //= = = = = = = = = = = = = = = = = = = = = = = = = Loggers per simulation thread = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = 
//    private final Logger mus_logger;
//    private final Logger cells_logger;
//    private final Logger buffers_logger;

    public LoggersRegistry(SimulationBaseRunner _simulation, final Scenario scenarioSetup, Preprocessor rawSimProps)
            throws InvalidOrUnsupportedException, InconsistencyException {
        simulation = _simulation;

        idFrmt = new Formatter() {
            @Override
            public String format(LogRecord lr) {

                StringBuilder builder = new StringBuilder(180);
                String lineSep = "\n";

                builder.append('\t').append(scenarioSetup.getIDStr()).append('@');
                int pos = lr.getSourceClassName().lastIndexOf('.');
                if (pos > -1) {
                    builder.append(lr.getSourceClassName().substring(pos + 1));
                } else {
                    builder.append(lr.getSourceClassName());
                }
                builder.append(':').append(':');
                builder.append(lr.getSourceMethodName());

                builder.append(' ');
                builder.append(formatMessage(lr));

//            builder.append('\t');
//            builder.append('[');
//            builder.append(lr.getLevel());
//            builder.append(']');
                builder.append(lineSep);
                builder.append(lineSep);

                Throwable throwable = lr.getThrown();
                if (throwable != null) {
                    StringWriter sink = new StringWriter();
                    throwable.printStackTrace(new PrintWriter(sink, true));
                    builder.append(sink.toString());
                }

                return builder.toString();

            }
        };

        String logOutputPath = rawSimProps.getValues(LoggingProperty.LOGGING__PATH).getFirst().get(0);

//        if (scenarioSetup.isTrue(LoggingProperty.LOGGING__GENERAL__ENABLE_UNIVERSAL)) {
//            mus_logger = buffers_logger = cells_logger = CONSOLE_LOGGER;
//        } else {
//            //<editor-fold defaultstate="collapsed" desc="separate initilization per logger">
//            mus_logger = initLocalLogger("MU_LOGGER", false);
//            String musLevel = rawSimProps.getValues(LoggingProperty.LOGGING__MUS__LEVEL).getFirst().get(0);
//            resetLogger(
//                    idFrmt, Level.parse(musLevel), true, mus_logger, logOutputPath,
//                    date_yyMMdd_HHmmss + "_" + "mus.log",
//                    date_yyMMdd_HHmmss + "_" + "general.log");
//
//            cells_logger = initLocalLogger("CELL_LOGGER", false);
//            String cellsLevel = rawSimProps.getValues(LoggingProperty.LOGGING__CELLS__LEVEL).getFirst().get(0);
//            resetLogger(
//                    idFrmt, Level.parse(cellsLevel), true, cells_logger,
//                    date_yyMMdd_HHmmss + "_" + "cells.log",
//                    date_yyMMdd_HHmmss + "_" + "general.log");
//
//            buffers_logger = initLocalLogger("BUFFER_LOGGER", false);
//            String buffersLevel = rawSimProps.getValues(LoggingProperty.LOGGING__BUFF__LEVEL).getFirst().get(0);
//            resetLogger(
//                    idFrmt, Level.parse(buffersLevel), true, buffers_logger, logOutputPath,
//                    date_yyMMdd_HHmmss + "_" + "buffers.log");
//            //</editor-fold>
//        }
    }

    private Logger initLocalLogger(String name, boolean addConsoleHandler) {
        Logger locLogger = Logger.getLogger(name);
        if (addConsoleHandler) {
            locLogger.addHandler(CONSOLE_HANDLER);
        }
        return locLogger;
    }

    public static final void resetGlobalLoggers(Preprocessor rawSimProps) throws InvalidOrUnsupportedException {
        try {
            String logOutputPath = rawSimProps.getValues(LoggingProperty.LOGGING__PATH).getFirst().get(0);

            String consoleLevel = rawSimProps.getValues(LoggingProperty.LOGGING__CONSOLE__LEVEL).getFirst().get(0);
            CONSOLE_LOGGER.setLevel(Level.parse(consoleLevel));

            String generalLevel = rawSimProps.getValues(LoggingProperty.LOGGING__GENERAL__LEVEL).getFirst().get(0);
            resetLogger(simpleFrmt, Level.parse(generalLevel), false, CONSOLE_LOGGER, logOutputPath, "general.log");

            String propsLevel = rawSimProps.getValues(LoggingProperty.LOGGING__PROPERTIES__LEVEL).getFirst().get(0);
            resetLogger(rawFrmt, Level.parse(propsLevel), true, PROPERTIES_LOGGER, logOutputPath, "properties.log", "general.log");
        } catch (InconsistencyException ex) {
            throw new InvalidOrUnsupportedException(ex);
        }
    }

    public static final void resetLogger(Formatter formater, Level logLevel, boolean removeConsooleHandeler,
            Logger theLogger2reset, String logOutputPath, String... outputFileName) {
        if (removeConsooleHandeler) {
            theLogger2reset.removeHandler(CONSOLE_HANDLER);
        }
        if (logLevel != null) {
            theLogger2reset.setLevel(logLevel);
        }
        theLogger2reset.setUseParentHandlers(false);

        for (String nxt_outputFileName : outputFileName) {

            File dir = new File(logOutputPath);
            dir.mkdirs();
            File file = new File(dir, nxt_outputFileName);
            try {
                file.createNewFile();
                FileHandler logFileHandler = new FileHandler(file.getCanonicalPath());
                logFileHandler.setLevel(logLevel);
                logFileHandler.setFormatter(formater);
                CONSOLE_LOGGER.log(Level.FINEST, "{0} reset to {1} and level {2}\n",
                        new Object[]{
                            theLogger2reset.getName(), logOutputPath, theLogger2reset.getLevel()
                        });
                theLogger2reset.addHandler(logFileHandler);
            } catch (IOException | SecurityException ex) {
                //<editor-fold defaultstate="collapsed" desc="upon catch">
                StringBuilder msg = new StringBuilder(theLogger2reset.getName());
                msg.append("  failed to use a file handler for logging in path=\n\t\"");
                msg.append(file.getPath());
                msg.append("\"\nMessages will be logged to the console ");
                msg.append("with log level ").append(theLogger2reset.getLevel()).append("\n");
                theLogger2reset.addHandler(CONSOLE_HANDLER);
                theLogger2reset.log(Level.INFO, msg.toString(), ex);
//</editor-fold>
            }
        }
    }

//    /**
//     * @return the mus_logger
//     */
//    public Logger getMus_logger() {
//        return mus_logger;
//    }
//
//    /**
//     * @return the cells_logger
//     */
//    public Logger getCells_logger() {
//        return cells_logger;
//    }

//    /**
//     * @return the buffers_logger
//     */
//    public Logger getBuffers_logger() {
//        return buffers_logger;
//    }

    @Override
    public final int simTime() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String simTimeStr() {
        return "[" + simTime() + "]";
    }

    @Override
    public final int simID() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public final SimulationBaseRunner getSim() {
        return simulation;
    }

    @Override
    public final CellRegistry simCellRegistry() {
        return getSim().getCellRegistry();
    }

    @Override
    public int hashCode() {
        return this.simulation.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final LoggersRegistry other = (LoggersRegistry) obj;
        if (!Objects.equals(this.simulation, other.simulation)) {
            return false;
        }
        return true;
    }

    
}
