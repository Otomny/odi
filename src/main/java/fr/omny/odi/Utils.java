package fr.omny.odi;


import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import fr.omny.odi.utils.Predicates;
import fr.omny.odi.utils.Reflections;

public class Utils {

	public static void addDefaultConstructorIfNotExists(Class<?> klass) {

	}

	public static List<Class<?>> getClasses(String packageName) {
		return getClasses(packageName, Predicates.alwaysTrue());
	}

	public static List<Class<?>> getClasses(String packageName, Predicate<Class<?>> filter) {
		String packageRelPath = packageName.replace('.', '/');
		CodeSource src = Utils.class.getProtectionDomain().getCodeSource();
		List<String> classes = new ArrayList<>();

		// Find all classes in the jar file that is in the desired package
		if (src != null) {
			URL jar = src.getLocation();
			try (ZipInputStream zip = new ZipInputStream(jar.openStream())) {
				while (true) {
					ZipEntry e = zip.getNextEntry();
					if (e == null)
						break;
					String name = e.getName();
					if (name.startsWith(packageRelPath) && name.endsWith(".class")) {
						classes.add(name);
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		Map<String, List<String>> classesInPackage = new HashMap<>();
		List<Class<?>> filteredClasses = new ArrayList<>();

		// Find all classes and put them in their folder respectively
		for (String classPath : classes) {
			var parentPackage = Paths.get(classPath).getParent().toString();
			classesInPackage.computeIfAbsent(parentPackage, k -> new ArrayList<>());
			classesInPackage.get(parentPackage).add(classPath.replace("/", "."));
		}
		for (String folder : classesInPackage.keySet()) {
			for (String classPath : classesInPackage.get(folder)) {
				try {
					var klass = Reflections.readClass(classPath.replace("/", "."));
					if (filter.test(klass)) {
						filteredClasses.add(klass);
						System.out.println("Loaded class " + classPath);
					}
				} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException
						| InvocationTargetException e) {
					// e.printStackTrace();
				}
			}
		}

		return filteredClasses.stream().map(Utils::forceInit).collect(Collectors.toList());
	}

	/**
	 * Forces the initialization of the class pertaining to the specified <tt>Class</tt> object. This method does nothing
	 * if the class is already initialized prior to invocation.
	 *
	 * @param klass the class for which to force initialization
	 * @return <tt>klass</tt>
	 */
	public static <T> Class<T> forceInit(Class<T> klass) {
		try {
			Class.forName(klass.getName(), true, klass.getClassLoader());
		} catch (ClassNotFoundException e) {
			throw new AssertionError(e); // Can't happen
		}
		return klass;
	}

	/**
	 * Perform autowiring
	 * 
	 * @param instance
	 */
	public static void autowireNoException(Object instance) {
		try {
			autowire(instance);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Perform autowiring
	 * 
	 * @param classInstance
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 */
	public static void autowire(Object instance) throws InstantiationException, IllegalAccessException {
		Class<?> klass = instance.getClass();
		for (Field field : klass.getDeclaredFields()) {
			if (field.isAnnotationPresent(Autowired.class)) {
				field.setAccessible(true);
				Object serviceInstance = Injector.getService(field.getType());
				field.set(instance, serviceInstance);
				autowire(serviceInstance);
			}
		}
	}
}
