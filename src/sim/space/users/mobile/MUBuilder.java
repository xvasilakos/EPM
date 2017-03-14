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
public class MUBuilder extends Builder{


    protected final double[] __probsTransition;
    protected final int __maxProbDirection;


    protected Collection<AbstractCachingPolicy> _cachingPolicies;

    public MUBuilder(
            SimulationBaseRunner simulation, MobileGroup group,
            Point startPoint, double[] probsTransition,
            List<String> connectionPolicySC,
            Collection<AbstractCachingPolicy> cachingPolicies) {
        super(simulation, group, startPoint, connectionPolicySC, cachingPolicies);

        // find the maximum probability to define __maxProbDirection
        this.__probsTransition = probsTransition;
        int maxProbDirection = -1;
        double maxProb = -1;
        for (int i = 0; i < __probsTransition.length; i++) {
            if (maxProb < __probsTransition[i]) {
                maxProbDirection = i;
                maxProb = __probsTransition[i];
            }
        }
        this.__maxProbDirection = maxProbDirection;
    }



    public MobileUser build() {
        return new MobileUser(this);
    }

}
