package caching;

import caching.base.AbstractCachingPolicy;
import exceptions.InvalidOrUnsupportedException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;

/**
 *
 * @author xvas
 */
public class CachingPoliciesFactory {

    /**
     * The registry of the supported caching methods.
     */
    private static final HashMap<String, AbstractCachingPolicy> supportedPoliciesRegistry
            = new HashMap<>(5);
    /**
     * The registry of the appropriate buffer type per caching method.
     */
    private static final HashMap<String, Class> bufferTypesRegistry
            = new HashMap<>(2);


    public static AbstractCachingPolicy getCachingPolicy(String cachingPolicy) throws InvalidOrUnsupportedException {
        if (!supportedPoliciesRegistry.containsKey(cachingPolicy)) {
            throw new InvalidOrUnsupportedException("Invalid or "
                    + "unsupported type of caching method: " + cachingPolicy);
        }
        return supportedPoliciesRegistry.get(cachingPolicy);
    }

    public static Class getBufferType(String cachingPolicy) throws InvalidOrUnsupportedException {
        if (!bufferTypesRegistry.containsKey(cachingPolicy)) {
            throw new InvalidOrUnsupportedException("Invalid or "
                    + "unsupported type of caching method: " + cachingPolicy);
        }
        return bufferTypesRegistry.get(cachingPolicy);
    }

    public static Class bufferTypeOf(AbstractCachingPolicy cachingPolicy) throws InvalidOrUnsupportedException {
        return getBufferType(cachingPolicy.toString());
    }

    public static AbstractCachingPolicy addCachingPolicy(String policy)
            throws ClassNotFoundException, InstantiationException,
            IllegalAccessException, IllegalArgumentException,
            InvocationTargetException, NoSuchMethodException {
//        AbstractMethod mthd = (AbstractMethod) Class.forName(mthdStr).newInstance();

        Method instance = Class.forName(policy).getMethod("instance");
        AbstractCachingPolicy abstrctMthd = (AbstractCachingPolicy) instance.invoke(null);

        supportedPoliciesRegistry.put(policy, abstrctMthd);

        Method bufferType = Class.forName(policy).getMethod("bufferType");
        Class buffClass = (Class) bufferType.invoke(null);

        bufferTypesRegistry.put(policy, buffClass);

        return abstrctMthd;
    }

}
