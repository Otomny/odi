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
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import fr.omny.odi.listener.OnConstructorCallListener;
import fr.omny.odi.proxy.ProxyFactory;
import fr.omny.odi.proxy.ProxyMarker;
import fr.omny.odi.utils.PreClass;
import fr.omny.odi.utils.Predicates;
import fr.omny.odi.utils.Reflections;

public class Utils {

	private static Map<String, List<String>> knownPathes = new HashMap<>();
	private static Map<String, PreClass> knownPreclassses = new HashMap<>();
	private static Set<OnConstructorCallListener> constructorCallListeners = new HashSet<>();

	/**
	 * Check if an object is a proxy object
	 *
	 * @param instance
	 * @return True if object is a proxy, false if instance is null or object is
	 *         not a proxy
	 */
	public static boolean isProxy(Object instance) {
		if (instance == null)
			return false;
		return instance instanceof ProxyMarker;
	}

	/**
	 * Recursively find a method
	 * 
	 * @deprecated Better user {@link Utils#findMethod(Class, Predicate)}
	 *
	 * @param klass
	 * @param methodName
	 * @param parametersType
	 * @return
	 */
	@Deprecated()
	public static Method recursiveFindMethod(Class<?> klass, String methodName,
			Class<?>[] parametersType) {
		try {
			if (klass.isInterface()) {
				return Object.class.getDeclaredMethod(methodName, parametersType);
			}
			return klass.getDeclaredMethod(methodName, parametersType);
		} catch (NoSuchMethodException e) {
			if (klass == Object.class) {
				throw new RuntimeException(methodName +
						" not found even on Object.class");
			}
			return recursiveFindMethod(klass.getSuperclass(), methodName,
					parametersType);
		}
	}

	/**
	 * Find a method by return type and parameters
	 * 
	 * @deprecated Better user {@link Utils#findMethod(Class, Predicate)}
	 *
	 * @param klass
	 * @param parametersType
	 * @return
	 */
	@Deprecated()
	public static Method findBySignature(Class<?> klass, Class<?> returnType,
			Class<?>[] parametersType) {
		MethodLoop: for (Method method : klass.getDeclaredMethods()) {
			if (method.getReturnType() != returnType)
				continue;
			if (method.getParameterCount() != parametersType.length)
				continue;
			for (int i = 0; i < parametersType.length; i++) {
				Class<?> parameterType = parametersType[i];
				Class<?> methodParameterType = method.getParameterTypes()[i];
				if (parameterType != methodParameterType)
					continue MethodLoop;
			}
			return method;
		}
		return null;
	}

	/**
	 * Find a method by name
	 *
	 * @deprecated Better user {@link Utils#findMethod(Class, Predicate)}
	 * 
	 * @param klass
	 * @param parametersType
	 * @return
	 */
	@Deprecated()
	public static Method findByName(Class<?> klass, String methodName) {
		for (Method method : klass.getDeclaredMethods()) {
			if (method.getName().equalsIgnoreCase(methodName))
				return method;
		}
		return null;
	}

	/**
	 * Recursively find a method
	 * 
	 * @param klass     The klass
	 * @param predicate The predicate
	 * @return The method or null if not found
	 */
	public static Method findMethod(Class<?> klass, Predicate<Method> predicate) {
		for (Method method : klass.getDeclaredMethods()) {
			if (predicate.test(method))
				return method;
		}
		if (klass.getSuperclass() == null)
			return null;
		if (klass.getInterfaces().length > 0) {
			for (Class<?> interf : klass.getInterfaces()) {
				Method foundMethod = findMethod(interf, predicate);
				if (foundMethod != null)
					return foundMethod;
			}
		}
		return findMethod(klass.getSuperclass(), predicate);
	}

	/**
	 * 
	 * @param klass
	 * @param methodName
	 * @param parametersType
	 * @return
	 */
	public static Optional<Method> safeGetDeclaredMethod(Class<?> klass, String methodName, Class<?>[] parametersType) {
		try {
			return Optional.of(klass.getDeclaredMethod(methodName, parametersType));
		} catch (NoSuchMethodException | SecurityException e) {
			return Optional.empty();
		}
	}

	/**
	 * Recursively find a field
	 * 
	 * @param klass     The klass
	 * @param predicate The predicate
	 * @return The field or null if not found
	 */
	public static Field findField(Class<?> klass, Predicate<Field> predicate) {
		if (klass.isInterface()) {
			return findField(Object.class, predicate);
		}
		for (Field field : klass.getDeclaredFields()) {
			if (predicate.test(field))
				return field;
		}
		if (klass.getSuperclass() == null)
			return null;
		return findField(klass.getSuperclass(), predicate);
	}

	/**
	 * Recursively find all fields
	 * 
	 * @param klass The klass
	 * @return All fields, or empty if interface
	 */
	public static List<Field> findAllFields(Class<?> klass) {
		if (klass.isInterface()) {
			return new ArrayList<>();
		}
		if (klass.getSuperclass() == null) {
			return new ArrayList<>();
		}
		List<Field> fields = new ArrayList<>();
		for (Field field : klass.getDeclaredFields()) {
			fields.add(field);
		}
		fields.addAll(findAllFields(klass.getSuperclass()));
		return fields;
	}

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
			throws InstantiationException, IllegalAccessException,
			IllegalArgumentException, InvocationTargetException {
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
	public static <T> T callConstructor(Class<? extends T> instanceClass,
			boolean useDefaultConstructor,
			Object... parameters)
			throws InstantiationException, IllegalAccessException,
			IllegalArgumentException, InvocationTargetException {
		int constructorCount = instanceClass.getDeclaredConstructors().length;
		for (Constructor<?> constructor : instanceClass.getDeclaredConstructors()) {
			if (constructorCount > 1) {
				// Multiple constructor
				if (useDefaultConstructor && constructor.getParameterCount() == 0) {
					// Use default one
					return callConstructor(constructor, instanceClass, parameters);
				} else if (!useDefaultConstructor &&
						constructor.getParameterCount() > 0) {
					// Use others
					return callConstructor(constructor, instanceClass, parameters);
				}
			} else {
				// only one, take it
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
	public static <T> T callConstructor(Constructor<?> constructor,
			Class<?> instanceClass,
			Object[] parameters)
			throws InstantiationException, IllegalAccessException,
			IllegalArgumentException, InvocationTargetException {
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
			var externalObj = Utils.constructorCallListeners.stream()
					.map(listener -> listener.value(targetParam))
					.filter(o -> o != null)
					.findFirst()
					.orElse(null);
			if (targetParam.isAnnotationPresent(Autowired.class)) {
				var autowiredData = targetParam.getAnnotation(Autowired.class);
				var autowireObj = Injector.getService(targetParam.getType(), autowiredData.value());
				finalParameters[i] = autowireObj;
				if (autowireObj != null)
					continue;
			}

			if (externalObj != null) {
				finalParameters[i] = externalObj;
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
		Utils.constructorCallListeners.forEach(
				listener -> listener.newInstance(returnObj));
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
			throws IllegalAccessException, IllegalArgumentException,
			InvocationTargetException {
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
	public static Object callMethod(String methodName, Object instance,
			Object... parameters)
			throws IllegalAccessException, IllegalArgumentException,
			InvocationTargetException {
		var classInstance = instance.getClass();
		for (var method : classInstance.getDeclaredMethods()) {
			if (method.getName().equalsIgnoreCase(methodName)) {
				return callMethod(method, classInstance, instance, parameters);
			}
		}
		return null;
	}

	/**
	 *
	 * @param method
	 * @param classInstance
	 * @param instance
	 * @param parameters
	 * @return
	 */
	public static Object callMethodQuiet(Method method, Class<?> classInstance,
			Object instance, Object[] parameters) {
		try {
			return callMethod(method, classInstance, instance, parameters);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
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
	public static Object callMethod(Method method, Class<?> classInstance,
			Object instance, Object[] parameters)
			throws IllegalAccessException, IllegalArgumentException,
			InvocationTargetException {
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
				if (autowireObj != null)
					continue;
			}
			var inputObj = getInputParameter.apply(targetParam.getType());
			if (inputObj != null) {
				finalParameters[i] = inputObj;
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

	public static List<Class<?>> getClasses(String packageName,
			Predicate<PreClass> filter) {
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
	 * Scan classes using ASM to read ConstantPool and other things without
	 * loading the class in the JVM (less memory overhead)
	 *
	 * @param classes
	 * @param filter
	 * @return
	 */
	public static List<Class<?>> getClasses(List<String> classes,
			Predicate<PreClass> filter) {
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
				var preClass = knownPreclassses.getOrDefault(
						classPath, Reflections.readPreClass(classPath));
				if (preClass != PreClass.NONE) {
					if (filter.test(preClass)) {
						try {
							filteredClasses.add(
									Reflections.readClass(classPath.replace("/", ".")));
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

		Injector.getLogger().ifPresent(
				log -> log.info("Analyzed " + classes.size() + " class files in " +
						(end.subtract(start).doubleValue() / 1_000_000) + "ms"));

		return filteredClasses.stream()
				.map(Utils::forceInit)
				.collect(Collectors.toList());
	}

	/**
	 * Forces the initialization of the class pertaining to the specified
	 * <tt>Class</tt> object. This method does nothing
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
	public static void autowire(Object instance)
			throws InstantiationException, IllegalAccessException {
		if (instance == null)
			return;
		Class<?> klass = ProxyFactory.getOriginalClass(instance);
		Injector.preWireListeners.forEach(listener -> listener.wire(instance));
		for (Field field : Utils.findAllFields(klass)) {
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
					if (serviceInstance != null) {
						final Class<?> originalClass = ProxyFactory.getOriginalClass(serviceInstance);
						if (!field.getType().isAssignableFrom(originalClass)) {
							Injector.getLogger().ifPresent(logger -> {
								logger.warning("Tried to inject type " + originalClass + " on type " + field.getType()
										+ " with component name " + autowiredData.value());
							});
						}
						field.set(instance, serviceInstance);
					}
				}
				if (serviceInstance != null) {
					Injector.wire(serviceInstance);
				}
			}
		}
	}
}
