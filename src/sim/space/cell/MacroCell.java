package sim.space.cell;

import java.util.logging.Level;
import java.util.logging.Logger;
import sim.run.SimulationBaseRunner;
import sim.space.Area;
import sim.space.Point;
import sim.space.util.DistanceComparator;

/**
 *
 * @author Xenofon Vasilakos xvas@aueb.gr
 */
public class MacroCell extends AbstractCell {

    public MacroCell(SimulationBaseRunner sim, int centerY, int centerX, double radius,
            Area area) throws Exception {
        super(sim, centerY, centerX, radius, area);
    }

    /**
     * Creates and returns a macro cell for which the following hold:
     *
     * The radius is computed and set as to cover the whole area (based on
     * _center and lowest right corner point). Therefore, all points of the area
 are added in the coverage of this macro.

 Center getCoordinates are computed and set as to be in the _center of the
 area. If the center getCoordinates are not integer values, ceil values are
 used.
     *
     * @param sim
     * @param area All points of the area will be covered by this macro cell
     * @return
     * @throws Exception
     */
    public static MacroCell createMacrocell(SimulationBaseRunner sim, Area area)
            throws Exception {
        MacroCell macrocell = null;

        // compute the _center's getCoordinates //
        Point center = new Point(area.getLengthX() / 2, area.getLengthY() / 2);

        // compute _radius //
        int lowX = area.getLengthX() - 1;  // _center uses floor, thus compute _radius based on low right corner
        int lowY = area.getLengthY() - 1;  // _center uses floor, thus compute _radius based on low right corner
        double radius = DistanceComparator.euclidianDistance(center, area.getPointAt(lowX, lowY));

        macrocell = new MacroCell(sim, center.getY(), center.getX(), radius, area);
        macrocell.addInCoveredArea(area);

        LOG.log(Level.FINER, "{0}: Created macrocell covering all area {1}\n",
                new Object[]{
                    sim.simTime(),
                    area.toSynopsisString()
                });

        return macrocell;
    }
    private static final Logger LOG = Logger.getLogger(MacroCell.class.getName());

    private void addInCoveredArea(Area area) {
        _coveredAreaPoints.addAll(area.getPoints());
        for (Point nxtPoint : getCoverageArea()) {
            nxtPoint.addCoverage(this);
        }
    }

}
