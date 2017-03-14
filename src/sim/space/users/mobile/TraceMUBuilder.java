package sim.space.users.mobile;

import caching.base.AbstractCachingPolicy;
import java.util.Collection;
import java.util.List;
import sim.run.SimulationBaseRunner;
import sim.space.Point;

/**
 *
 * @author xvas
 */
public class TraceMUBuilder extends Builder {

    private final double __dx;
    private final double __dy;

    public TraceMUBuilder(SimulationBaseRunner simulation, MobileGroup group,
            Point startPoint, 
            List<String> connectionPolicySC,
            Collection<AbstractCachingPolicy> cachingPolicies, double dx, double dy) {
        super(simulation, group, startPoint, connectionPolicySC, cachingPolicies);
        __dx = dx;
        __dy = dy;
    }

    @Override
        public TraceMU build() {
        return new TraceMU(this);
    }

    /**
     * @return the __dx
     */
    public double getDx() {
        return __dx;
    }

    /**
     * @return the __dy
     */
    public double getDy() {
        return __dy;
    }

}
