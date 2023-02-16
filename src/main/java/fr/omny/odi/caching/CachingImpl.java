package fr.omny.odi.caching;


import java.lang.reflect.Method;

import lombok.NonNull;

public interface CachingImpl {

	/**
	 * Create cache instance
	 * @param forClass
	 * @param forMethod
	 * @param cacheSettings
	 */
	void applySettings(Class<?> forClass, Method forMethod, Caching cacheSettings);

	/**
	 * If a key is present in the cache
	 * 
	 * @param key The key
	 * @return True if the cache contains the key, false otherwise
	 */
	boolean contains(int key);

	/**
	 * Get the value associated with the cache
	 * 
	 * @param key The key
	 * @return The object associated with that key
	 */
	@NonNull
	Object get(int key);

	/**
	 * Put the object with the key
	 * 
	 * @param key    The key
	 * @param result The object
	 */
	void put(int key, @NonNull Object result);

}
