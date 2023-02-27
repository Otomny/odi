package fr.omny.odi.joinpoint;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Provider should use this annotation on class declaration
 * 
 * For this annotation to be use on a method returns, the class should have also this annotation
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface Joinpoint {

	/**
	 * Name of join point, can be the method name to capture the whole thing
	 * @return
	 */
	String value();

	Class<?> on();

	JoinpointPlace place() default JoinpointPlace.AFTER_INVOKE;
	
}
