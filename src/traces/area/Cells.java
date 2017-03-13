package traces.area;

import exceptions.CriticalFailureException;
import exceptions.InconsistencyException;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import sim.space.Point;
import utilities.Couple;

/**
 *
 * @author xvas
 */
public class Cells {

    public static Couple<Point, Point> extractAreaFromMetadata(
            File metaF, int minX, int minY, int maxX, int maxY)
            throws CriticalFailureException, NumberFormatException {

        try {

            LOG.log(Level.INFO, "Loading area dimensions from metadata: \"{0}\"",
                    metaF.getAbsolutePath());

            Scanner sc = new Scanner(metaF);
            while (sc.hasNext()) {
                String ln = sc.nextLine();

                String[] split = ln.split("=");

                if (split[0].equalsIgnoreCase("min coordinate X")) {
                    minX = (int) Double.parseDouble(split[1]);
                }

                if (split[0].equalsIgnoreCase("min coordinate X")) {
                    minX = (int) Double.parseDouble(split[1]);
                }

                if (split[0].equalsIgnoreCase("min coordinate Y")) {
                    minY = (int) Double.parseDouble(split[1]);
                }

                if (split[0].equalsIgnoreCase("max coordinate X")) {
                    maxX = (int) Double.parseDouble(split[1]);
                }

                if (split[0].equalsIgnoreCase("max coordinate Y")) {
                    maxY = (int) Double.parseDouble(split[1]);
                }

            }
            sc.close();
        } catch (FileNotFoundException | InconsistencyException ex) {
            throw new CriticalFailureException("Metadata file has issues: "
                    + "\""
                    + metaF.getAbsolutePath()
                    + "\"", ex);
        }

        Couple<Point, Point> fromTo = new Couple<>(
                new Point(minX-1, minY-1),
                new Point(maxX+1, maxY+1)
        );

        LOG.log(Level.INFO, "Loaded dimensions of are from metadata file: "
                + "from ({0,number,#}}, {1,number,#}})"
                + "]"
                + " to ({2,number,#}}, {3,number,#}})", new Object[]{minX, minY, maxX, maxY});

        return fromTo;
    }
    private static final Logger LOG = Logger.getLogger(Cells.class.getName());
}
