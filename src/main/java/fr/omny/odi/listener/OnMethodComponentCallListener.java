package fr.omny.odi.listener;

import java.lang.reflect.Method;

/**
 * Listen to method registering when traversing a class that has the
 * {@link fr.omny.odi.Component} annotation on it
 */
public interface OnMethodComponentCallListener {

	/**
	 * Check if the method is filtered by this listener
	 * 
	 * @param containingClass The class with the {@link fr.omny.odi.Component}
	 *                        annotation
	 * @param method          The method declared the the containingClass
	 * @return True if the listener is listen to this method registering
	 */
	boolean isFiltered(Class<?> containingClass, Method method);

	/**
	 * Check if the method can be call in the context
	 * 
	 * @param containingClass The class with the {@link fr.omny.odi.Component}
	 *                        annotation
	 * @param method          The method declared the the containingClass
	 * @return True if the method can be called, false otherwise
	 */
	boolean canCall(Class<?> containingClass, Method method);

}
