package statistics.handlers;

/**
 * Same as ICompute2 but requiring the knowledge of two caching methods in order to
 * compute statistics.
 *
 * @author xvas
 */
public interface ICompute3<U, C> extends ICompute {

      public abstract double compute3(U user, C cell) throws statistics.StatisticException;

}
