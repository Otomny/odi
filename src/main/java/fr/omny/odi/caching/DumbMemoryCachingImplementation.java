package fr.omny.odi.caching;


import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import lombok.NonNull;

/**
 * Worst memory caching technique
 */
public class DumbMemoryCachingImplementation implements CachingImpl {

	private Map<Integer, Object> cache = new HashMap<>();

	@Override
	public void applySettings(Class<?> forClass, Method forMethod, Caching cacheSettings) {
		
	}

	@Override
	public boolean contains(int arrayHashCode) {
		return cache.containsKey(arrayHashCode);
	}

	@Override
	public @NonNull Object get(int arrayHashCode) {
		return cache.get(arrayHashCode);
	}

	@Override
	public void put(int arrayHashCode, @NonNull Object result) {
		cache.put(arrayHashCode, result);
	}

	
}
