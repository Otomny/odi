package fr.omny.odi.listener;

import java.lang.reflect.Method;

public interface OnProxyCallListener {
	
	/**
	 * 
	 * @param method
	 * @return
	 */
	boolean pass(Method method);

	/**
	 * 
	 * @param instance
	 * @param remoteMethod
	 * @param arguments
	 */
	Object invoke(Object instance, Method remoteMethod, Object[] arguments) throws Exception;

}
