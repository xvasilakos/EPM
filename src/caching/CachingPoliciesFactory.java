package caching;

import caching.base.AbstractCachingModel;
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
    private static final HashMap<String, AbstractCachingModel> supportedPoliciesRegistry
            = new HashMap<>(5);
    /**
     * The registry of the appropriate buffer type per caching method.
     */
    private static final HashMap<String, Class> bufferTypesRegistry
            = new HashMap<>(2);


    public static AbstractCachingModel getCachingModel(String cachingModel) throws InvalidOrUnsupportedException {
        if (!supportedPoliciesRegistry.containsKey(cachingModel)) {
            throw new InvalidOrUnsupportedException("Invalid or "
                    + "unsupported type of caching method: " + cachingModel);
        }
        return supportedPoliciesRegistry.get(cachingModel);
    }

    public static Class getBufferType(String cachingModel) throws InvalidOrUnsupportedException {
        if (!bufferTypesRegistry.containsKey(cachingModel)) {
            throw new InvalidOrUnsupportedException("Invalid or "
                    + "unsupported type of caching method: " + cachingModel);
        }
        return bufferTypesRegistry.get(cachingModel);
    }

    public static Class bufferTypeOf(AbstractCachingModel cachingModel) throws InvalidOrUnsupportedException {
        return getBufferType(cachingModel.toString());
    }

    public static AbstractCachingModel addCachingPolicy(String model)
            throws ClassNotFoundException, InstantiationException,
            IllegalAccessException, IllegalArgumentException,
            InvocationTargetException, NoSuchMethodException {
//        AbstractMethod mthd = (AbstractMethod) Class.forName(mthdStr).newInstance();

        Method instance = Class.forName(model).getMethod("instance");
        AbstractCachingModel abstrctMthd = (AbstractCachingModel) instance.invoke(null);

        supportedPoliciesRegistry.put(model, abstrctMthd);

        Method bufferType = Class.forName(model).getMethod("bufferType");
        Class buffClass = (Class) bufferType.invoke(null);

        bufferTypesRegistry.put(model, buffClass);

        return abstrctMthd;
    }

}
