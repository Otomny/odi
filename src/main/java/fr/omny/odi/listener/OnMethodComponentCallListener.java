package fr.omny.odi.listener;

import java.lang.reflect.Method;

public interface OnMethodComponentCallListener {
	
	boolean isFiltered(Class<?> containgClass, Method method);

	boolean canCall(Class<?> containgClass, Method method);

}
