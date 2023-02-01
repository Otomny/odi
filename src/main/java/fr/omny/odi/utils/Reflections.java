package fr.omny.odi.utils;


import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

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
	 * 
	 * @param path
	 * @return a PreClass state
	 */
	public static PreClass readPreClass(String path) {
		var classLoader = Reflections.class.getClassLoader();
		try (var inputStream = classLoader.getResourceAsStream(path)) {
			ClassReader cr = new ClassReader(inputStream.readAllBytes());
			String className = path.substring(path.lastIndexOf("/") + 1, path.indexOf(".class"));
			String packageName = path.substring(0, path.lastIndexOf("/") - 1)
				.replaceAll("/", ".");
			var harvester = new BytecodeHarvester(className);
			// Reading information about the class (Methods, Annotations, Super class extends, etc...)
			// doesn't require to read any of the code content
			// it's only about the ConstantPool
			cr.accept(harvester, ClassReader.SKIP_FRAMES & ClassReader.SKIP_DEBUG & ClassReader.SKIP_CODE);
			return new PreClass(packageName, harvester);
		} catch (Exception e) {
			e.printStackTrace();
			return PreClass.NONE;
		}
	}

	/**
	 * 
	 * @return
	 */
	public static String[] findTypeName(Type[] typeParameters, Class<?> iGenericClass){
		for(var type : typeParameters){
			var ptype = (ParameterizedType) type;
			if(ptype.getRawType().getTypeName().equalsIgnoreCase(iGenericClass.getSimpleName())){
				String[] s = new String[ptype.getActualTypeArguments().length];
				for (int i = 0; i < ptype.getActualTypeArguments().length; i++) {
					s[i] = ptype.getActualTypeArguments()[i].getTypeName();
				}
				return s;
			}
		}
		return null;
	}

}
