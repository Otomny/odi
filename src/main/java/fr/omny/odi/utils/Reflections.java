package fr.omny.odi.utils;

import java.lang.reflect.InvocationTargetException;

public class Reflections {
	
	private Reflections(){}


	/**
	 * Read class content without linking it
	 * @param path
	 * @return
	 * @throws SecurityException
	 * @throws NoSuchMethodException
	 * @throws InvocationTargetException
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 */
	public static Class<?> readClass(String path) throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException{
		var classLoader = Reflections.class.getClassLoader();
		var classLoaderClass = classLoader.getClass();
		var method = classLoaderClass.getDeclaredMethod("loadClass", String.class, boolean.class);
		method.setAccessible(true);
		Class<?> klass = (Class<?>) method.invoke(classLoader, path.replace(".class", ""), false);
		method.setAccessible(false);
		return klass;
	}

}
