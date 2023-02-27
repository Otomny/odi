package fr.omny.odi.joinpoint;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
public @interface Pointcut {

	/**
	 * 
	 * @return The name of the pointcut (default = methodName)
	 */
	String value() default "__methodName";

}
