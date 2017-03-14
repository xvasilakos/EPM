/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sim.space.util;

import sim.space.ISpaceMember;
import java.util.Comparator;

/**
 *
 * @author Xenofon Vasilakos xvas@aueb.gr
 */
public class DistanceComparator implements Comparator<ISpaceMember> {

   /**
    * The reference space member from which Euclidian distances are computed.
    */
   private ISpaceMember referenceSpaceMember;
   /**
    * given that distances are doubles, the scale parameter is multiplied by the distances
    * from the reference space member in order for the #compare(first, second) to return a
    * more precise integer difference.
    */
   private int scale;

   /**
    * @param referenceSpaceMember the reference space member from which Euclidian
    * distances are computed.
    * @param scale given that distances are doubles, the scale parameter is multiplied by
    * the distances from the reference space member in order for the #compare(first,
    * second) to return a more precise integer difference.
    */
   public DistanceComparator(ISpaceMember referenceSpaceMember, int scale) {
      this.referenceSpaceMember = referenceSpaceMember;
      this.scale = scale;
   }

   /**
    * Uses Euclidean geometry to compute the distance between each parameter and the
    * reference space member, and then returns the difference of the distance of parameter
    * first minus the distance of parameter second.
    *
    * @param first
    * @param second
    * @return
    */
   @Override

   public int compare(ISpaceMember first, ISpaceMember second) {
      return (int) (scale * euclidianDistance(first, getReferenceSpaceMember()))
            - (int) (scale * euclidianDistance(second, getReferenceSpaceMember()));
   }

   /**
    *
    * @param a
    * @param b
    * @return the Euclidian distance between two ISpaceMember instances.
    */
   public static double euclidianDistance(ISpaceMember a, ISpaceMember b) {
      double euclidian = Math.pow(a.getY() - b.getY(), 2);
      euclidian += Math.pow(a.getX() - b.getX(), 2);
      euclidian = Math.sqrt(euclidian);
      return euclidian;
   }

   /**
    * @return the referenceSpaceMember
    */
   public ISpaceMember getReferenceSpaceMember() {
      return referenceSpaceMember;
   }

   /**
    * @return the scale
    */
   public int getScale() {
      return scale;
   }
}
