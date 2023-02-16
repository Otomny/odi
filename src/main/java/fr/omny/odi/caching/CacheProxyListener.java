package fr.omny.odi.caching;


import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

import fr.omny.odi.Utils;
import fr.omny.odi.listener.OnProxyCallListener;

public class CacheProxyListener implements OnProxyCallListener {

	private CachingImpl cachingImpl;

	@Override
	public boolean pass(Method method) {
		if (!method.isAnnotationPresent(Caching.class)) {
			return false;
		}
		if (cachingImpl != null)
			return true;
		var cachingSettings = method.getAnnotation(Caching.class);
		try {
			this.cachingImpl = Utils.callConstructor(cachingSettings.implementation());
			this.cachingImpl.applySettings(method.getDeclaringClass(), method, cachingSettings);
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
		return true;
	}

	@Override
	public Object invoke(Object instance, Method remoteMethod, Object[] arguments) throws Exception {
		int arrayHashCode = Arrays.hashCode(arguments);
		if(cachingImpl.contains(arrayHashCode)){
			return cachingImpl.get(arrayHashCode);
		}else{
			Object result = remoteMethod.invoke(instance, arguments);
			cachingImpl.put(arrayHashCode, result);
			return result;
		}
	}

}
