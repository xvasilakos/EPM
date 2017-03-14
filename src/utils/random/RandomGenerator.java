package utils.random;

import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Xenofon Vasilakos xvas@aueb.gr
 */
public final class RandomGenerator {

   private static MersenneTwisterFast mersenneTwisterRndGen;
   private static java.util.Random javaUtilRndGen;

   public RandomGenerator(int seed) {
      mersenneTwisterRndGen = new MersenneTwisterFast(seed);
      javaUtilRndGen = new Random(seed);
   }

   /**
    * @param from inclusive lower bound
    * @param to inclusive upper bound
    * @return a random uniformly chosen number between "from" and "to" (both inclusive)
    */
   public double randDoubleInRange(double from, double to) {
      try {
         return from + mersenneTwisterRndGen.nextDouble() * (to - from);
      } catch (Exception ex) {
         Logger.getLogger(RandomGenerator.class.getSimpleName()).log(
                 Level.SEVERE, "SOLVED: Using standard java random generator instead .. ",
                 ex);
         return from + javaUtilRndGen.nextDouble() * (to - from);
      }
   }

   /**
    * @param from inclusive lower bound
    * @param to inclusive upper bound
    * @return a random uniformly chosen number between "from" and "to" (both inclusive)
    */
   public int randIntInRange(int from, int to) {
      try {
         return from + (int) Math.ceil(mersenneTwisterRndGen.nextDouble() * (to - from));
      } catch (Exception ex) {
         Logger.getLogger(RandomGenerator.class.getSimpleName()).log(
                 Level.SEVERE, "SOLVED: Using standard java random generator instead .. ",
                 ex);
         return from + (int) Math.ceil(javaUtilRndGen.nextDouble() * (to - from));
      }
   }
}
