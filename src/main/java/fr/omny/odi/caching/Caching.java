package fr.omny.odi.caching;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Caching {
	
	Class<? extends CachingImpl> implementation() default DumbMemoryCachingImplementation.class;
	

}
