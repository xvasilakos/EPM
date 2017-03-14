package sim.space.cell;

import sim.space.Point;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Summarizes cells and points of the intersected coverage area.
 * @author Xenofon Vasilakos xvas@aueb.gr
 */
public class Intersection {

   /**
    * null when not initialized with setter method.
    */
   private Set<AbstractCell> intersectedCells;
   /**
    * null when not initialized with setter method.
    */
   private Set<Point> intersectionPoints;

   public Intersection() {
   }

   public void setIntersectedCells(AbstractCell... cells) {
      intersectedCells = new HashSet<>();

      for (int i = 0; i < cells.length; i++) {
         AbstractCell nxt_cell = cells[i];
         intersectedCells.add(nxt_cell);
      }
   }

   public void addIntersectionPoint(Point... points) {
      if (intersectionPoints == null) {
         intersectionPoints = new HashSet<>();
      }
      
      for (int i = 0; i < points.length; i++) {
         intersectionPoints.add(points[i]);
      }
   }

   /**
    * Finds and returns the intersection of all sets passed in the parameters list.
    *
    *  cells
    * @return
    */
   public static Set<Point> intersection(AbstractCell... cells) {

      if (cells.length <= 1) {
         return null;// no intersection in this case
      }
      Set<Point> intersectionSet = cells[0].computeIntersectionWith(cells[1]);// simplest case 

      for (int i = 2; i < cells.length; i++) {
         AbstractCell nxtCell = cells[i];
         Set<Point> newIntersection = nxtCell.computeIntersectionWith(intersectionSet);
         intersectionSet = newIntersection;
      }

      if (intersectionSet.isEmpty()) {
         return null;
      }

      return intersectionSet;
   }

   /**
    *  cells
    * @return the intersection between the two cells.
    */
   public static Set<Point> intersection(AbstractCell a, AbstractCell b) {
//      Set<Point> intersectionSet = new HashSet<>();
//      Set<Point> pointsOfA = a.coveredArea;
//      Set<Point> pointsOfB = b.coveredArea;
//      for (Iterator<Point> a_iter = pointsOfA.iterator(); a_iter.hasNext();) {
//         Point nxt_pointA = a_iter.next();
//         if (pointsOfB.contains(nxt_pointA)) {
//            intersectionSet.add(nxt_pointA);
//         }
//      }
//
//      if (intersectionSet.isEmpty()) {
//         return null;
//      }
//      return intersectionSet;
//   }

      return a.computeIntersectionWith(b.getCoverageArea());
   }

   public static int hashCode(Set<? extends AbstractCell> intesectionCells) {
      int hash = 5;
      for (Iterator<? extends AbstractCell> it = intesectionCells.iterator(); it.hasNext();) {
         AbstractCell nxt_Cell = it.next();
         hash = 47 * hash + nxt_Cell.hashCode();
      }
      return hash;
   }

   @Override
   public int hashCode() {
      int hash = hashCode(this.intersectedCells);
      return hash;
   }

   public boolean equals(Object b) {
      if (b == null || !(b instanceof Intersection)) {
         return false;
      }

      return ((Intersection) b).hashCode() == this.hashCode();
   }
}
