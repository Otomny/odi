package fr.omny.odi.caching;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Caching {

	/**
	 * @return Implementation of cache
	 */
	Class<? extends CachingImpl> implementation() default DumbMemoryCachingImplementation.class;

	/**
	 * Default 60 seconds
	 * @return Time to live in cache
	 */
	long ttl() default 60 * 1000L;

	/**
	 * Default 500
	 * @return Cache max capacity
	 */
	int size() default 500;

	/**
	 * Idle time
	 * @return maxIdleTime
	 */
	long maxIdleTime() default 60 * 1000L;

}
