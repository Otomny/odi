package fr.omny.odi.caching;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import fr.omny.odi.Utils;
import fr.omny.odi.listener.OnProxyCallListener;

/**
 * Listen to intercept method that has the {@link Caching} annotation
 */
public class CacheProxyListener implements OnProxyCallListener {

	/**
	 * Check if a class has caching method
	 * 
	 * @param klass
	 * @return
	 */
	public static boolean hasCacheMethod(Class<?> klass) {
		for (Method method : klass.getDeclaredMethods()) {
			if (method.isAnnotationPresent(Caching.class))
				return true;
		}
		return false;
	}

	/**
	 * Caching implementation of methods
	 */
	private Map<Method, CachingImpl> cachingMethodMap = new HashMap<>();

	@Override
	public boolean pass(Method method) {
		if (!method.isAnnotationPresent(Caching.class)) {
			return false;
		}
		if (cachingMethodMap.containsKey(method))
			return true;
		var cachingSettings = method.getAnnotation(Caching.class);
		try {
			CachingImpl impl = Utils.callConstructor(cachingSettings.implementation());
			impl.applySettings(method.getDeclaringClass(), method, cachingSettings);
			this.cachingMethodMap.put(method, impl);
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
		return true;
	}

	@Override
	public Object invoke(Object instance, Method remoteMethod, Object[] arguments) throws Exception {
		CachingImpl cachingImpl = cachingMethodMap.get(remoteMethod);
		int arrayHashCode = Arrays.hashCode(arguments);
		if (cachingImpl.contains(arrayHashCode)) {
			return cachingImpl.get(arrayHashCode);
		} else {
			Object result = remoteMethod.invoke(instance, arguments);
			cachingImpl.put(arrayHashCode, result);
			return result;
		}
	}

}
