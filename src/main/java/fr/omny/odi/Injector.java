package fr.omny.odi;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
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

import fr.omny.odi.caching.CacheProxyListener;
import fr.omny.odi.listener.OnMethodComponentCallListener;
import fr.omny.odi.listener.OnPreWireListener;
import fr.omny.odi.proxy.ProxyFactory;

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
			Object proxyInstance = ProxyFactory.newProxyInstance(klass, service,
					List.of(new CacheProxyListener()));
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
			return instance.getServiceInstance(klass, "default");
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static <T> T getService(Class<T> klass, String name) {
		try {
			return instance.getServiceInstance(klass, name);
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
	private Map<Class<?>, Map<String, Object>> singletons;
	private Map<Object, Object> proxied;

	private Injector() {
		singletons = new HashMap<>();
		proxied = new HashMap<>();
	}

	public void add(Class<?> implementationClass) throws Exception {
		Object originalInstance = Utils.callConstructor(implementationClass);
		Object proxyInstance = originalInstance;
		if (CacheProxyListener.hasCacheMethod(implementationClass)) {
			proxyInstance = ProxyFactory.newProxyInstance(implementationClass, originalInstance,
					List.of(new CacheProxyListener()));
			this.proxied.put(proxyInstance, originalInstance);
		}

		var componentData = implementationClass.getAnnotation(Component.class);
		if (componentData.requireWire()) {
			Injector.wire(originalInstance);
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
				throw new RuntimeException(e);
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
						if (!Modifier.isFinal(returnType.getModifiers())) {
							Object proxyInstance = ProxyFactory.newProxyInstance(returnType, nestedService,
									List.of(new CacheProxyListener()));
							this.proxied.put(proxyInstance, nestedService);
						}

						if (this.singletons.containsKey(returnType)) {
							getLogger().ifPresent(logger -> {
								logger.config("Registered component of type " + returnType + " with name " + componentData.value());
							});
							this.singletons.get(returnType).put(componentData.value(), nestedService);
						} else {
							getLogger().ifPresent(logger -> {
								logger.config("Registered component of type " + returnType + " with name " + componentData.value());
							});
							Map<String, Object> maps = new HashMap<>();
							maps.put(componentData.value(), nestedService);
							this.singletons.put(returnType, maps);
						}
					}
				} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
					throw new RuntimeException(e);
				}
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
	private <T> T getServiceInstance(Class<?> serviceClass, String name) {
		if (this.singletons.containsKey(serviceClass)) {
			var instance = (T) this.singletons.get(serviceClass).get(name);
			if (instance != null)
				return instance;
			if (!this.singletons.get(serviceClass).isEmpty()) {
				return (T) List.of(this.singletons.get(serviceClass).values()).get(0);
			}
			return null;
		} else {
			return null;
		}
	}

}
