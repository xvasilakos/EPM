package statistics.handlers;

import caching.base.AbstractCachingModel;

/**
 * @author xvas
 *
 */
public abstract class AbstractPerformanceStat<USER_TYPE, CELL_TYPE, REQUEST_TYPE> extends BaseHandler implements statistics.handlers.ICompute {

    private final AbstractCachingModel _cachingPolicy;

    public AbstractPerformanceStat(AbstractCachingModel cachingMethod) {
        super();
        _cachingPolicy = cachingMethod;
    }

    public abstract double computeGain(USER_TYPE user, REQUEST_TYPE r) throws statistics.StatisticException;

    @Override
    public String title() {
        return getClass().getSimpleName() + "(" + _cachingPolicy.nickName() + ")";
    }

    public String title(String str) {
        return getClass().getSimpleName() + "<" + str + ">" + "(" + getCachingModel().nickName() + ")";
    }

    /**
     * @return the _cachingPolicy
     */
    public AbstractCachingModel getCachingModel() {
        return _cachingPolicy;
    }
}
