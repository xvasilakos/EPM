package sim.space;

/**
 * All members that are part of the area have to implement this interface.
 *
 * @author Xenofon Vasilakos xvas@aueb.gr
 */
public interface ISpaceMember {

   /**
    * @return the getCoordinates as a point.
    */
   public Point getCoordinates();

   /**
    * @return The x space dimension getCoordinates, i.e. the "latitude".
    */
   public int getX();

   /**
    * @return The y space dimension coordinate, i.e the "altitude".
    */
   public int getY();
}
