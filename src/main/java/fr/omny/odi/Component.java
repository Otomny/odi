package fr.omny.odi;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Provider should use this annotation on class declaration
 * 
 * For this annotation to be use on a method returns, the class should have also
 * this annotation
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface Component {

	/**
	 * Name of a component
	 * 
	 * @return
	 */
	String value() default "default";

	/**
	 * Tell if this component require a pre-wire injection
	 * 
	 * @return
	 */
	boolean requireWire() default false;

	/**
	 * Disable or Enable proxies
	 * 
	 * Use full when you want to return Interfaces OR Classes from external
	 * libraries
	 * 
	 * @return
	 */
	boolean proxy() default true;

}
