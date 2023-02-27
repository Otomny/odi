package fr.omny.odi.listener;

import java.lang.reflect.Method;

/**
 * Listener for proxy method invocation
 */
public interface OnProxyCallListener {

	/**
	 * Check if a listener is concerned by a method
	 * 
	 * @param method The method
	 * @return True if this listener should replace default invoke, false otherwise
	 */
	boolean pass(Method method);

	/**
	 * Replace the default invoke by this call.
	 * <p>
	 * If and only if the returned object is not null and the return type of the
	 * original function is <strong>void</strong>
	 * </p>
	 * 
	 * @param instance Original instance
	 * @param remoteMethod Original method
	 * @param arguments Arguments passed
	 * @return The result
	 */
	Object invoke(Object instance, Method remoteMethod, Object[] arguments) throws Exception;

}
