package statistics.handlers;

import sim.space.cell.AbstractCell;
import sim.space.cell.smallcell.SmallCell;
import statistics.StatisticException;

/**
 *
 * @author Xenofon Vasilakos xvas@aueb.gr
 */
public abstract class BaseHandler implements Comparable<BaseHandler> {

    abstract public String title();

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof BaseHandler)) {
            return false;
        } else {
            return this.title().equals(((BaseHandler) other).title());
        }
    }

    /**
     * Checks if parameter cell is a an instance of a small cell
     *
     * @param cell the cell checked by this
     * @return the cell casted to a small cell.
     * @throws StatisticException if the cell reference is not an instance of
     * SmallCell
     * @throws NullPointerException if the cell reference is null
     */
    final protected SmallCell checkSmallCell(AbstractCell cell) throws StatisticException {
        if (cell == null) {
            throw new NullPointerException("Null cell reference.");
        }
        if (!(cell instanceof SmallCell)) {
            throw new StatisticException(
                    SmallCell.class.getSimpleName() + " expected but " + cell.getClass().getSimpleName()
                    + " was passed intead.");
        }

        return (SmallCell) cell;
    }

    /**
     * Checks if parameter cell is a an instance of a small cell
     *
     * @param cells the cell or cells checked by this method invocation.
     * @return the cell casted to a small cell.
     * @throws StatisticException
     */
    final protected SmallCell[] checkSmallCell(AbstractCell... cells) throws StatisticException {
        SmallCell[] _checkSmallerCell = new SmallCell[cells.length];
        int i = 0;
        for (AbstractCell cell : cells) {
            _checkSmallerCell[i++] = checkSmallCell(cell);
        }
        return _checkSmallerCell;
    }

    @Override
    public int compareTo(BaseHandler other) {
        return title().compareTo(other.title());
    }

    @Override
    public int hashCode() {
        return title().hashCode();
    }

}
