package statistics.handlers;

import sim.space.cell.AbstractCell;

/**
 * Requires only the AbstractCell to compute statistics.
 *
 * @author xvas
 * @param <CELLTYPE>
 */
public interface ICompute4<CELLTYPE extends AbstractCell > extends statistics.handlers.ICompute{
   public double compute4(CELLTYPE cell) throws statistics.StatisticException;
}
