package fr.omny.odi;
import static java.lang.annotation.ElementType.*;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Classes that uses services should use this annotation
 */
@Target({METHOD, CONSTRUCTOR, FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Autowired {
	
}
