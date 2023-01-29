package fr.omny.odi.utils;


import java.lang.reflect.InvocationTargetException;

import org.objectweb.asm.ClassReader;

public class Reflections {

	private Reflections() {}

	/**
	 * Read class content without linking it
	 * 
	 * @param path
	 * @return
	 * @throws SecurityException
	 * @throws NoSuchMethodException
	 * @throws InvocationTargetException
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 */
	public static Class<?> readClass(String path) throws NoSuchMethodException, SecurityException, IllegalAccessException,
			IllegalArgumentException, InvocationTargetException {
		var classLoader = Reflections.class.getClassLoader();
		var classLoaderClass = classLoader.getClass();
		var method = classLoaderClass.getDeclaredMethod("loadClass", String.class, boolean.class);
		method.setAccessible(true);
		Class<?> klass = (Class<?>) method.invoke(classLoader, path.replace(".class", ""), true);
		method.setAccessible(false);
		return klass;
	}

	/**
	 * Read bytecode class declaration without loading dependencies, without loading code and all
	 * @param path
	 * @return a PreClass state
	 */
	public static PreClass readPreClass(String path) {
		var classLoader = Reflections.class.getClassLoader();
		try (var inputStream = classLoader.getResourceAsStream(path)) {
			ClassReader cr = new ClassReader(inputStream.readAllBytes());
			String className = path.substring(path.lastIndexOf("/")+1, path.indexOf(".class"));
			var harvester = new AnnotationHarvester(className);
			cr.accept(harvester, ClassReader.SKIP_FRAMES & ClassReader.SKIP_DEBUG);
			return new PreClass(harvester);
		} catch (Exception e) {
			e.printStackTrace();
			return PreClass.NONE;
		}
	}

}
