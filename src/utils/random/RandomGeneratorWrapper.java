package utils.random;

/**
 * Wraps the the MersenneTwister random numbers generator.
 *
 * @author Xenofon Vasilakos xvas@aueb.gr
 */
public final class RandomGeneratorWrapper {

    private final MersenneTwister mersenneTwister;
    private final int seed;

    /**
     * @return the mersenneTwister
     */
    public MersenneTwister getMersenneTwister() {
        return mersenneTwister;
    }

    public RandomGeneratorWrapper(int seed) {
        this.seed = seed;
        mersenneTwister = new MersenneTwister(seed);
    }

    public double getGaussian(double mean, double stdev) {
        return mean + mersenneTwister.nextGaussian() * stdev;
    }

    /**
     * @param from inclusive lower bound
     * @param to inclusive upper bound
     * @return a random uniformly chosen number between "from" and "to" (both
     * inclusive)
     */
    public double randDoubleInRange(double from, double to) {
        return from + getMersenneTwister().nextDouble() * (to - from);
    }

    /**
     * @param from inclusive lower bound
     * @param to inclusive upper bound
     * @return a random uniformly chosen number between "from" and "to" (both
     * inclusive)
     */
    public int randIntInRange(int from, int to) {
        return from + (int) (getMersenneTwister().nextDouble() * (to - from)) + 1;
    }

    /**
     * This is an equivalent call to
     * RandomGeneratorWrapper#randDoubleInRange(0.0, 1.0);
     *
     * @return a random probability in [0.0, 1.0])
     */
    public double randProbability() {
        return randDoubleInRange(0.0, 1.0);
    }

    /**
     * @return the seed
     */
    public int getSeed() {
        return seed;
    }
}
