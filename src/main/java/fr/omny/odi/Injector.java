package fr.omny.odi;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.stream.Stream;

import fr.omny.odi.joinpoint.Joinpoint;
import fr.omny.odi.listener.OnMethodComponentCallListener;
import fr.omny.odi.listener.OnPreWireListener;
import fr.omny.odi.proxy.ProxyFactory;
import fr.omny.odi.proxy.ProxyMarker;

public class Injector {

	private static Injector instance;
	private static Optional<Logger> logger = Optional.empty();
	protected static Set<OnPreWireListener> preWireListeners = new HashSet<>();
	protected static Set<OnMethodComponentCallListener> methodCallListeners = new HashSet<>();

	public static void registerWireListener(OnPreWireListener onWireListener) {
		preWireListeners.add(onWireListener);
	}

	public static void registerMethodCallListener(OnMethodComponentCallListener onMethodListener) {
		methodCallListeners.add(onMethodListener);
	}

	public static Optional<Logger> getLogger() {
		return logger;
	}

	public static void startApplication(Class<?> mainClass) {
		startApplication(mainClass, null);
	}

	public static void startApplication(Class<?> mainClass, Logger logger) {
		try {
			synchronized (Injector.class) {
				if (instance == null) {
					Injector.logger = Optional.ofNullable(logger);
					instance = new Injector();
					instance.add(mainClass.getPackageName());
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Method for Injector test
	 */
	public static void startTest() {
		try {
			synchronized (Injector.class) {
				if (instance == null) {
					instance = new Injector();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void wipeTest() {
		try {
			synchronized (Injector.class) {
				if (instance != null) {
					instance = null;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Perform autowiring
	 *
	 * @param object
	 */
	public static void wire(Object object) {
		if (Injector.instance.proxied.containsKey(object)) {
			Utils.autowireNoException(Injector.instance.proxied.get(object));
		} else {
			Utils.autowireNoException(object);
		}
	}

	/**
	 * @param klass
	 * @param object
	 */
	public static void addService(Class<?> klass, Object object) {
		addService(klass, "default", object);
	}

	/**
	 * @param klass
	 * @param name
	 * @param object
	 */
	public static void addService(Class<?> klass, String name, Object object) {
		if (instance.singletons.containsKey(klass)) {
			instance.singletons.get(klass).put(name, object);
		} else {
			Map<String, Object> maps = new HashMap<>();
			maps.put(name, object);
			instance.singletons.put(klass, maps);
		}
	}

	/**
	 * Call a service constructor with the desired parameters and add them to
	 * services
	 *
	 * @param klass
	 * @param parameters
	 */
	public static void addServiceParams(Class<?> klass, Object... parameters) {
		addServiceParams(klass, "default", parameters);
	}

	/**
	 * Call a service constructor with the desired parameters and add them to
	 * services
	 *
	 * @param klass
	 * @param parameters
	 */
	public static void addServiceParams(Class<?> klass, String name, Object... parameters) {
		try {
			Object service = Utils.callConstructor(klass, false, parameters);
			Object proxyInstance = ProxyFactory.newProxyInstance(klass, service);
			Injector.instance.proxied.put(proxyInstance, service);

			if (instance.singletons.containsKey(klass)) {
				instance.singletons.get(klass).put(name, service);
			} else {
				Map<String, Object> maps = new HashMap<>();
				maps.put(name, service);
				instance.singletons.put(klass, maps);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static <T> T getService(Class<T> klass) {
		try {
			return instance.getServiceInstance(klass, "default", false);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static <T> T getOriginalService(Class<T> klass) {
		try {
			return instance.getOriginalServiceInstance(klass, "default");
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static List<Method> getJoinpoints(Class<?> klass, String joinPointName) {
		if (!Injector.instance.joinPoints.containsKey(klass))
			return List.of();
		if (!Injector.instance.joinPoints.get(klass).containsKey(joinPointName))
			return List.of();
		return Injector.instance.joinPoints.get(klass).get(joinPointName);
	}

	public static void joinpoint(Object instance, String joinPointName) {
		joinpoint(instance, joinPointName, new Object[] {});
	}

	public static void joinpoint(Object instance, String joinPointName, Object[] parameters) {
		if (instance == null)
			return;
		if (Utils.isProxy(instance))
			// BETTER HANDLING by returning real class behind
			return;
		Class<?> instanceClass = instance.getClass();
		List<Method> joinPoints = getJoinpoints(instanceClass, joinPointName);
		for (Method joinPoint : joinPoints) {
			Class<?> joinPointClass = joinPoint.getDeclaringClass();
			if (!Injector.instance.singletons.containsKey(joinPointClass))
				continue;
			var joinPointInstance = Injector.getService(joinPointClass);
			try {
				Utils.callMethod(joinPoint, joinPointClass, joinPointInstance,
						UnsafeUtils.concatenate(new Object[] { instance }, parameters));
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				e.printStackTrace();
			}
		}
	}

	public static <T> T getServiceRaw(Class<T> klass, String name) {
		try {
			return instance.getServiceInstance(klass, name, true);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static <T> T getService(Class<T> klass, String name) {
		try {
			return instance.getServiceInstance(klass, name, false);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * @param mainClass
	 */
	public static void addFrom(Class<?> mainClass) {
		addFrom(mainClass.getPackageName());
	}

	/**
	 *
	 * @param implementationClass
	 * @throws Exception
	 */
	public static void addSpecial(Class<?> implementationClass) throws Exception {
		instance.add(implementationClass);
	}

	/**
	 * @param packageName
	 */
	public static void addFrom(String packageName) {
		if (instance != null) {
			instance.add(packageName);
		}
	}

	/**
	 * Find each that correspond to validate the predicate
	 *
	 * @param o
	 * @return
	 */
	public static Stream<Object> findEach(Predicate<Class<?>> o) {
		return instance.singletons.entrySet().stream().filter(e -> o.test(e.getKey()))
				.flatMap(e -> e.getValue().values().stream());
	}

	/**
	 * Find each component that respect specific predicate
	 *
	 * @param o The predicate
	 * @return
	 */
	public static Stream<Entry<Class<?>, Map<String, Object>>> findEachWithClasses(Predicate<Class<?>> o) {
		return instance.singletons.entrySet().stream().filter(e -> o.test(e.getKey()));
	}

	/**
	 * All instances of a service
	 */
	private Map<Class<?>, Map<String, Object>> singletons = new HashMap<>();
	private Map<Class<?>, Map<String, List<Method>>> joinPoints = new HashMap<>();
	private Map<Object, Object> proxied = new HashMap<>();

	private Injector() {
	}

	public void addJoinPoint(Class<?> klass, String joinPointName, Method callable) {
		if (this.joinPoints.containsKey(klass)) {
			if (this.joinPoints.get(klass).containsKey(joinPointName)) {
				this.joinPoints.get(klass).get(joinPointName).add(callable);
			} else {
				this.joinPoints.get(klass).put(joinPointName, new ArrayList<>());
				addJoinPoint(klass, joinPointName, callable);
			}
		} else {
			this.joinPoints.put(klass, new HashMap<>());
			addJoinPoint(klass, joinPointName, callable);
		}
	}

	public void add(Class<?> implementationClass) throws Exception {
		Object originalInstance = Utils.callConstructor(implementationClass);

		var componentData = implementationClass.getAnnotation(Component.class);
		if (componentData.requireWire()) {
			Injector.wire(originalInstance);
		}
		Object proxyInstance = originalInstance;
		if (componentData.proxy() && !Modifier.isFinal(implementationClass.getModifiers())
				&& !implementationClass.isInterface()) {
			proxyInstance = ProxyFactory.newProxyInstance(implementationClass, originalInstance);
		}

		addMethodReturns(implementationClass, originalInstance);
		if (this.singletons.containsKey(implementationClass)) {
			getLogger().ifPresent(logger -> {
				logger.config("Registered component of type " + implementationClass + " with name " + componentData.value());
			});
			this.singletons.get(implementationClass).put(componentData.value(), proxyInstance);
		} else {
			getLogger().ifPresent(logger -> {
				logger.config("Registered component of type " + implementationClass + " with name " + componentData.value());
			});
			Map<String, Object> maps = new HashMap<>();
			maps.put(componentData.value(), proxyInstance);
			this.singletons.put(implementationClass, maps);
		}
	}

	public void add(String packageName) {
		var classes = Utils.getClasses(packageName,
				klass -> klass.isAnnotationPresent(Component.class) && klass.isNotByteBuddy());
		for (Class<?> implementationClass : classes) {
			if (implementationClass.getCanonicalName().contains("$ByteBuddy"))
				continue;
			try {
				if (this.singletons.containsKey(implementationClass))
					continue;
				add(implementationClass);
			} catch (Exception e) {
				throw new RuntimeException("Failed to create service for " + implementationClass, e);
			}
		}
	}

	/**
	 * @param implementationClass
	 * @param serviceInstance
	 * @throws Exception
	 */
	public void addMethodReturns(Class<?> implementationClass, Object englobedService) throws Exception {
		for (Method method : implementationClass.getDeclaredMethods()) {
			if (method.isAnnotationPresent(Component.class)) {
				if (methodCallListeners.stream().filter(listener -> listener.isFiltered(implementationClass, method))
						.anyMatch(listener -> !listener.canCall(implementationClass, method))) {
					continue;
				}
				var componentData = method.getAnnotation(Component.class);
				method.setAccessible(true);
				// Inject autowired arguments
				try {
					Class<?> returnType = method.getReturnType();
					if (returnType != void.class) {
						if (this.singletons.containsKey(returnType))
							continue;
						Object nestedService = Utils.callMethod(method, implementationClass, englobedService, new Object[] {});
						Object proxyInstance = nestedService;
						if (componentData.proxy() && !Modifier.isFinal(returnType.getModifiers())
								&& !implementationClass.isInterface()) {
							proxyInstance = ProxyFactory.newProxyInstance(implementationClass, nestedService);
						}

						if (this.singletons.containsKey(returnType)) {
							getLogger().ifPresent(logger -> {
								logger.config("Registered component of type " + returnType + " with name " + componentData.value());
							});
							this.singletons.get(returnType).put(componentData.value(), proxyInstance);
						} else {
							getLogger().ifPresent(logger -> {
								logger.config("Registered component of type " + returnType + " with name " + componentData.value());
							});
							Map<String, Object> maps = new HashMap<>();
							maps.put(componentData.value(), proxyInstance);
							this.singletons.put(returnType, maps);
						}
					}
				} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
					throw new RuntimeException(e);
				}
			} else if (method.isAnnotationPresent(Joinpoint.class)) {
				var joinpointData = method.getAnnotation(Joinpoint.class);
				method.setAccessible(true);
				addJoinPoint(joinpointData.on(), joinpointData.value(), method);
			}
		}
	}

	/**
	 * Retrieve the service instance
	 *
	 * @param <T>
	 * @param serviceClass
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private <T> T getServiceInstance(Class<?> serviceClass, String name, boolean raw) {
		if (this.singletons.containsKey(serviceClass)) {
			var instance = (T) this.singletons.get(serviceClass).get(name);
			if (instance != null)
				return instance;
			if (!this.singletons.get(serviceClass).isEmpty() && !raw) {
				return (T) List.of(this.singletons.get(serviceClass).values()).get(0);
			}
			return null;
		} else {
			return null;
		}
	}

	/**
	 * Retrieve the service instance
	 *
	 * @param <T>
	 * @param serviceClass
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private <T> T getOriginalServiceInstance(Class<?> serviceClass, String name) {
		if (this.singletons.containsKey(serviceClass)) {
			var instance = (T) this.singletons.get(serviceClass).get(name);
			if (instance != null) {
				if (instance instanceof ProxyMarker marker) {
					return (T) marker.getOriginalInstance();
				}
				return instance;
			}
			if (!this.singletons.get(serviceClass).isEmpty()) {
				instance = (T) List.of(this.singletons.get(serviceClass).values()).get(0);
				if (instance instanceof ProxyMarker marker) {
					return (T) marker.getOriginalInstance();
				}
				return instance;
			}
			return null;
		} else {
			return null;
		}
	}

}
