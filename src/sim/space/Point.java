package sim.space;

import exceptions.InconsistencyException;
import java.util.Collections;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Set;
import utils.ISynopsisString;
import sim.space.cell.AbstractCell;
import sim.space.cell.MacroCell;
import sim.space.cell.smallcell.SmallCell;
import sim.space.users.User;
import sim.space.util.DistanceComparator;
import utils.CommonFunctions;

/**
 * A point in the area. A point is covered by one or more cells.
 *
 * @author Xenofon Vasilakos xvas@aueb.gr
 */
public final class Point implements sim.space.ISpaceMember, ISynopsisString {
  /**
   * A point for special usage (e.g to denote out of area range).
   */
   public static final Point NONE = new Point(-1, -1);

   private final Set<User> _usersInPoint = new HashSet<>();
   private final Set<SmallCell> _coveringSmallCells = new HashSet<>();
   private MacroCell coveringMacroCell;
   private final int y;
   private final int x;
   /**
    * HashID is needed to differentiate hash codes of (x,y) and (y,x);
    */
   private final int id;
   private static int id_gen = 1;
   private PriorityQueue<SmallCell> _closestCoveringSCs;

   public Point(int x, int y) {
      this._closestCoveringSCs = null; // intentionally null to be initilized with first invocation
      id = id_gen++;

      this.x = x;
      this.y = y;
   }

   @Override
   /**
    * @return true iff same hashID.
    */
   public boolean equals(Object b) {
      if (b == null) {
         return false;
      }

      if (!(b instanceof Point)) {
         return false;
      }

      Point bP = (Point) b;
      return bP.getId() == this.getId();
   }

   @Override
   public int hashCode() {
      return id;
   }

   public void addCoverage(AbstractCell cell) {
      if (cell instanceof SmallCell) {
         _coveringSmallCells.add((SmallCell) cell);
      } else if (cell instanceof MacroCell) {
         coveringMacroCell = (MacroCell) cell;
      } else {
         throw new UnsupportedOperationException("Unsupported type or not known decendant of AbstractCell");
      }
   }

   public void removeCoverage(AbstractCell cell) throws Exception {
      if (cell instanceof SmallCell) {
         _coveringSmallCells.remove((SmallCell) cell);
      } else if (cell instanceof MacroCell) {
         if (coveringMacroCell.hashCode() == cell.hashCode()) {
            coveringMacroCell = null;
         } else {
            throw new Exception("Macrocell passed to remove does not match the current macrocell");
         }
      } else {
         throw new UnsupportedOperationException("Unsupported type or not known decendant of "
               + "AbstractCell");
      }
   }

   /**
    * @return the y
    */
   @Override
   public int getY() {
      return y;
   }

   /**
    * @return the x
    */
   @Override
   public int getX() {
      return x;
   }

   public void addUser(User mu) {
      _usersInPoint.add(mu);
   }

   public void removeUser(User mu) {
      if (!_usersInPoint.remove(mu)) {
         throw new InconsistencyException(
               "Cannot remove " + mu.toSynopsisString()+ " from " + toSynopsisString()
               + " The mobile user appears not to be curently conected to this point, despite its current point being: "
               + mu.getCoordinates().toSynopsisString());
      }
   }

   public Set<User> getUsers() {
      return Collections.unmodifiableSet(_usersInPoint);
   }

   public boolean containsUsers(AbstractCell FC) {
      return !_usersInPoint.isEmpty();
   }

   @Override
   public String toString() {
      StringBuilder toStr = new StringBuilder().append('<');
      toStr.append(toSynopsisString());
      toStr.append("; id=");
      toStr.append(getId());
      toStr.append("; hosting: ");
      toStr.append(CommonFunctions.toString("\n\t", getUsers()));
      toStr.append(CommonFunctions.toString("\n\t", _coveringSmallCells));

      toStr.append(">");
      return toStr.toString();
   }

   @Override
   public String toSynopsisString() {
      StringBuilder _toString = new StringBuilder();
      _toString.append(getClass().getSimpleName());
      _toString.append("[");
      _toString.append(getX()).append(", ").append(getY());
      _toString.append("]");

      return _toString.toString();
   }

   /**
    * @return the id
    */
   public int getId() {
      return id;
   }

   /**
    * @return an unmodifiable set of small cells that cover this point.
    */
   public Set<SmallCell> getCoveringSCs() {
      return Collections.unmodifiableSet(_coveringSmallCells);
   }

   public MacroCell getCoveringMacroCell() {
      return this.coveringMacroCell;
   }

   public boolean isCoveredBy(AbstractCell cell) {
      if (cell == null) {
         throw new RuntimeException("Cannot check coverage from a null cell because a null cell reference refers to out of area points.");
      }

      if (cell instanceof SmallCell) {
         return _coveringSmallCells.contains((SmallCell) cell);
      }
      if (cell instanceof MacroCell) {
         return this.getCoveringMacroCell().equals((MacroCell) cell);
      }

      if (DistanceComparator.euclidianDistance(cell.getCoordinates(), this) < cell.getRadius()) {
         if (cell instanceof SmallCell) {
            _coveringSmallCells.add((SmallCell) cell);
            return true;
         } else {
            coveringMacroCell = (MacroCell) cell;
            return true;
         }
      }

      throw new UnsupportedOperationException("Unsupported for " + cell.getClass().getCanonicalName());
   }

   /**
    * Set recompute to true if you intend to recompute distance, e.g. because distances or
    * SCs have changed since last call; otherwise it returns a precomputed set.
    *
    * @param recompute True if intended to recompute with this call; otherwise returns a
    * precomputed.
    * @param recomputeIfNoneKnown
    * @return a sorted set of closest with respect to range SCs.
    */
   public PriorityQueue<SmallCell> getClosestCoveringSCs(boolean recompute, boolean recomputeIfNoneKnown) {
      if (_closestCoveringSCs == null) { // if first time used.
         return _closestCoveringSCs = computeClosestSCs();
      }

      if (recompute) {
         return _closestCoveringSCs = computeClosestSCs();
      }

      if (recomputeIfNoneKnown && _closestCoveringSCs.isEmpty()) {
         return _closestCoveringSCs = computeClosestSCs();
      }

      return _closestCoveringSCs;
   }

   public PriorityQueue<SmallCell> computeClosestSCs() {
      _closestCoveringSCs = null;
      Set<SmallCell> coveringSCells = getCoveringSCs();
      this._closestCoveringSCs = new PriorityQueue<>(coveringSCells.size(), new DistanceComparator(this, 100000));
      _closestCoveringSCs.addAll(coveringSCells);
      return _closestCoveringSCs;
   }

   @Override
   /**
    * @return itself.
    */
   public Point getCoordinates() {
      return this;
   }
}
