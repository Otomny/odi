package fr.omny.odi.listener;

import java.lang.reflect.Parameter;

/**
 * Listen on constructor called from
 * {@link fr.omny.odi.Utils#callConstructor(java.lang.reflect.Constructor, Class, Object[])} method
 */
public interface OnConstructorCallListener {

	/**
	 * Called when a instance is create
	 * @param instance
	 */
	void newInstance(Object instance);

	/**
	 * Affect a value to a parameter of the constructor, if the result is not null
	 * @param parameter The parameter of the constructor
	 * @return Null if no parameter was found, or the value of the parameter
	 */
	Object value(Parameter parameter);

}
