package statistics.handlers;

import caching.base.AbstractCachingPolicy;

/**
 * @author xvas
 *
 */
public abstract class AbstractPerformanceStat<USER_TYPE, CELL_TYPE, REQUEST_TYPE> extends BaseHandler implements statistics.handlers.ICompute {

    private final AbstractCachingPolicy _cachingPolicy;

    public AbstractPerformanceStat(AbstractCachingPolicy cachingMethod) {
        super();
        _cachingPolicy = cachingMethod;
    }

    public abstract double computeGain(USER_TYPE user, REQUEST_TYPE r) throws statistics.StatisticException;

    @Override
    public String title() {
        return getClass().getSimpleName() + "(" + _cachingPolicy.nickName() + ")";
    }

    public String title(String str) {
        return getClass().getSimpleName() + "<" + str + ">" + "(" + getCachingPolicy().nickName() + ")";
    }

    /**
     * @return the _cachingPolicy
     */
    public AbstractCachingPolicy getCachingPolicy() {
        return _cachingPolicy;
    }
}
