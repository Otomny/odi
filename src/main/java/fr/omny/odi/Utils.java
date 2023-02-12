package fr.omny.odi;


import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.math.BigDecimal;
import java.net.URL;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import fr.omny.odi.listener.OnConstructorCallListener;
import fr.omny.odi.utils.PreClass;
import fr.omny.odi.utils.Predicates;
import fr.omny.odi.utils.Reflections;

public class Utils {

	private static Map<String, List<String>> knownPathes = new HashMap<>();
	private static Map<String, PreClass> knownPreclassses = new HashMap<>();
	private static Set<OnConstructorCallListener> constructorCallListeners = new HashSet<>();

	/**
	 * @param listener
	 */
	public static void registerCallConstructor(OnConstructorCallListener listener) {
		constructorCallListeners.add(listener);
	}

	/**
	 * @param <T>
	 * @param instanceClass
	 * @return
	 * @throws InvocationTargetException
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 */
	public static <T> T callConstructor(Class<? extends T> instanceClass)
			throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		return callConstructor(instanceClass, false, new Object[] {});
	}

	/**
	 * @param instanceClass
	 * @return
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 */
	public static <T> T callConstructor(Class<? extends T> instanceClass, boolean useDefaultConstructor,
			Object... parameters)
			throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		int constructorCount = instanceClass.getConstructors().length;
		for (Constructor<?> constructor : instanceClass.getConstructors()) {
			if (useDefaultConstructor && constructor.getParameters().length == 0) {
				return callConstructor(constructor, instanceClass, parameters);
			}
			// if multiple constructor, don't take the default one (The one without any parameters)
			if (constructorCount == 1 || constructor.getParameters().length > 0) {
				return callConstructor(constructor, instanceClass, parameters);
			}
		}
		return null;
	}

	/***
	 * @param constructor
	 * @param instanceClass
	 * @param parameters
	 * @return
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 */
	public static <T> T callConstructor(Constructor<?> constructor, Class<?> instanceClass, Object[] parameters)
			throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		var targetParameters = constructor.getParameters();
		Object[] finalParameters = new Object[targetParameters.length];

		List<Object> usedParameters = new ArrayList<>();

		Function<Class<?>, Object> getInputParameter = klass -> {
			for (var obj : parameters) {
				if (usedParameters.contains(obj))
					continue;
				if (klass.isAssignableFrom(obj.getClass())) {
					usedParameters.add(obj);
					return obj;
				}
			}
			return null;
		};

		for (int i = 0; i < finalParameters.length; i++) {
			var targetParam = targetParameters[i];
			if (targetParam.isAnnotationPresent(Autowired.class)) {
				var autowiredData = targetParam.getAnnotation(Autowired.class);
				var autowireObj = Injector.getService(targetParam.getType(), autowiredData.value());
				finalParameters[i] = autowireObj;
			} else {
				var inputObj = getInputParameter.apply(targetParam.getType());
				if (inputObj != null) {
					finalParameters[i] = inputObj;
				}
			}
		}
		constructor.setAccessible(true);
		@SuppressWarnings("unchecked")
		T returnObj = (T) constructor.newInstance(finalParameters);
		Utils.constructorCallListeners.forEach(listener -> listener.newInstance(returnObj));
		constructor.setAccessible(false);
		return returnObj;
	}

	/**
	 * @param methodName
	 * @param instance
	 * @return
	 * @throws InvocationTargetException
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 */
	public static Object callMethod(String methodName, Object instance)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		return callMethod(methodName, instance, new Object[] {});
	}

	/**
	 * @param methodName
	 * @param instance
	 * @return
	 * @throws InvocationTargetException
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 */
	public static Object callMethod(String methodName, Object instance, Object... parameters)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		var classInstance = instance.getClass();
		for (var method : classInstance.getDeclaredMethods()) {
			if (method.getName().equalsIgnoreCase(methodName)) {
				return callMethod(method, classInstance, instance, parameters);
			}
		}
		return null;
	}

	/**
	 * @param method
	 * @param classInstance
	 * @param instance
	 * @param parameters
	 * @return
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 */
	public static Object callMethod(Method method, Class<?> classInstance, Object instance, Object[] parameters)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		var targetParameters = method.getParameters();
		Object[] finalParameters = new Object[targetParameters.length];

		Function<Class<?>, Object> getInputParameter = klass -> {
			for (var obj : parameters) {
				if (klass.isAssignableFrom(obj.getClass()))
					return obj;
			}
			return null;
		};

		for (int i = 0; i < finalParameters.length; i++) {
			var targetParam = targetParameters[i];
			if (targetParam.isAnnotationPresent(Autowired.class)) {
				var autowiredData = targetParam.getAnnotation(Autowired.class);
				var autowireObj = Injector.getService(targetParam.getType(), autowiredData.value());
				finalParameters[i] = autowireObj;
			} else {
				var inputObj = getInputParameter.apply(targetParam.getType());
				if (inputObj != null) {
					finalParameters[i] = inputObj;
				}
			}
		}
		method.setAccessible(true);
		var returnObj = method.invoke(instance, finalParameters);
		method.setAccessible(false);
		return returnObj;
	}

	public static List<Class<?>> getClasses(String packageName) {
		return getClasses(packageName, Predicates.alwaysTrue());
	}

	public static List<Class<?>> getClasses(String packageName, Predicate<PreClass> filter) {
		if (knownPathes.containsKey(packageName)) {
			return getClasses(knownPathes.get(packageName), filter);
		}
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
		knownPathes.put(packageName, classes);
		return getClasses(classes, filter);
	}

	/**
	 * Scan classes using ASM to read ConstantPool and other things without loading the class in the JVM (less memory
	 * overhead)
	 * 
	 * @param classes
	 * @param filter
	 * @return
	 */
	public static List<Class<?>> getClasses(List<String> classes, Predicate<PreClass> filter) {
		Map<String, List<String>> classesInPackage = new HashMap<>();
		List<Class<?>> filteredClasses = new ArrayList<>();

		// Find all classes and put them in their folder respectively
		for (String classPath : classes) {
			var parentPackage = Paths.get(classPath).getParent().toString();
			classesInPackage.computeIfAbsent(parentPackage, k -> new ArrayList<>());
			classesInPackage.get(parentPackage).add(classPath);
		}
		var start = BigDecimal.valueOf(System.nanoTime());

		for (String folder : classesInPackage.keySet()) {
			for (String classPath : classesInPackage.get(folder)) {
				var preClass = knownPreclassses.getOrDefault(classPath, Reflections.readPreClass(classPath));
				if (preClass != PreClass.NONE) {
					if (filter.test(preClass)) {
						try {
							filteredClasses.add(Reflections.readClass(classPath.replace("/", ".")));
						} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException
								| InvocationTargetException e) {
							e.printStackTrace();
						}
					}
					knownPreclassses.putIfAbsent(classPath, preClass);
				}

			}
		}

		var end = BigDecimal.valueOf(System.nanoTime());

		Injector.getLogger().ifPresent(log -> log.info(
				"Analyzed " + classes.size() + " class files in " + (end.subtract(start).doubleValue() / 1_000_000) + "ms"));

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
	public static <T> T autowireNoException(T instance) {
		try {
			autowire(instance);
			return instance;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return instance;
	}

	/**
	 * Perform autowiring
	 * 
	 * @param classInstance
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 */
	public static void autowire(Object instance) throws InstantiationException, IllegalAccessException {
		if (instance == null)
			return;
		Class<?> klass = instance.getClass();
		Injector.preWireListeners.forEach(listener -> listener.wire(instance));
		for (Field field : klass.getDeclaredFields()) {
			if (field.isAnnotationPresent(Autowired.class)) {
				var autowiredData = field.getAnnotation(Autowired.class);
				field.setAccessible(true);
				Object serviceInstance = null;
				if (field.getType() == Optional.class) {
					ParameterizedType type = (ParameterizedType) field.getGenericType();
					Class<?> serviceType = (Class<?>) type.getActualTypeArguments()[0];
					serviceInstance = Injector.getService(serviceType, autowiredData.value());
					// serviceInstance
					field.set(instance, Optional.ofNullable(serviceInstance));
				} else {
					serviceInstance = Injector.getService(field.getType(), autowiredData.value());
					field.set(instance, serviceInstance);
				}
				if (serviceInstance == null) {
					Injector.getLogger().ifPresent(logger -> {
						logger.warning("Could not find service of type " + field.getType() + " with name " + autowiredData.value());
					});
				}
				autowire(serviceInstance);
				field.setAccessible(false);
			}
		}
	}
}
