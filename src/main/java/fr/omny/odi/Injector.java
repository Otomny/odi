package fr.omny.odi;


import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.stream.Stream;

import fr.omny.odi.listener.OnPreWireListener;

public class Injector {

	private static Injector instance;
	private static Optional<Logger> logger = Optional.empty();
	protected static Set<OnPreWireListener> preWireListeners = new HashSet<>();

	public static void registerWireListener(OnPreWireListener onWireListener) {
		preWireListeners.add(onWireListener);
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
		Utils.autowireNoException(object);
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
	 * Call a service constructor with the desired parameters and add them to services
	 * 
	 * @param klass
	 * @param parameters
	 */
	public static void addServiceParams(Class<?> klass, Object... parameters) {
		try {
			var service = Utils.callConstructor(klass, false, parameters);
			if (instance.singletons.containsKey(klass)) {
				instance.singletons.get(klass).put("default", service);
			} else {
				Map<String, Object> maps = new HashMap<>();
				maps.put("default", service);
				instance.singletons.put(klass, maps);
			}
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Call a service constructor with the desired parameters and add them to services
	 * 
	 * @param klass
	 * @param parameters
	 */
	public static void addServiceParams(Class<?> klass, String name, Object... parameters) {
		try {
			var service = Utils.callConstructor(klass, false, parameters);
			if (instance.singletons.containsKey(klass)) {
				instance.singletons.get(klass).put(name, service);
			} else {
				Map<String, Object> maps = new HashMap<>();
				maps.put(name, service);
				instance.singletons.put(klass, maps);
			}
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			e.printStackTrace();
		}
	}

	public static <T> T getService(Class<T> klass) {
		try {
			return instance.getServiceInstance(klass, "default");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static <T> T getService(Class<T> klass, String name) {
		try {
			return instance.getServiceInstance(klass, name);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * @param mainClass
	 */
	public static void addFrom(Class<?> mainClass) {
		addFrom(mainClass.getPackageName());
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
		return instance.singletons.values().stream().flatMap(m -> m.values().stream())
				.filter(obj -> o.test(obj.getClass()));
	}

	/**
	 * All instances of a service
	 */
	private Map<Class<?>, Map<String, Object>> singletons;

	private Injector() {
		singletons = new HashMap<>();
	}

	public void add(String packageName) {
		var classes = Utils.getClasses(packageName, klass -> klass.isAnnotationPresent(Component.class));
		for (Class<?> implementationClass : classes) {
			try {
				if (this.singletons.containsKey(implementationClass))
					continue;
				Object serviceInstance = Utils.callConstructor(implementationClass);
				var componentData = implementationClass.getAnnotation(Component.class);
				if (componentData.requireWire()) {
					Injector.wire(serviceInstance);
				}
				addMethodReturns(implementationClass, serviceInstance);
				if (this.singletons.containsKey(implementationClass)) {
					getLogger().ifPresent(logger -> {
						logger.info("Registered component of type " + implementationClass + " with name " + componentData.value());
					});
					this.singletons.get(implementationClass).put(componentData.value(), serviceInstance);
				} else {
					getLogger().ifPresent(logger -> {
						logger.info("Registered component of type " + implementationClass + " with name " + componentData.value());
					});
					Map<String, Object> maps = new HashMap<>();
					maps.put(componentData.value(), serviceInstance);
					this.singletons.put(implementationClass, maps);
				}
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
					| SecurityException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * @param implementationClass
	 * @param serviceInstance
	 */
	public void addMethodReturns(Class<?> implementationClass, Object englobedService) {
		for (Method method : implementationClass.getDeclaredMethods()) {
			if (method.isAnnotationPresent(Component.class)) {
				var componentData = method.getAnnotation(Component.class);
				method.setAccessible(true);
				// Inject autowired arguments
				try {
					Class<?> returnType = method.getReturnType();
					if (returnType != void.class) {
						if (this.singletons.containsKey(returnType))
							continue;
						Object nestedService = Utils.callMethod(method, implementationClass, englobedService, new Object[] {});
						if (this.singletons.containsKey(returnType)) {
							getLogger().ifPresent(logger -> {
								logger.info("Registered component of type " + returnType + " with name " + componentData.value());
							});
							this.singletons.get(returnType).put(componentData.value(), nestedService);
						} else {
							getLogger().ifPresent(logger -> {
								logger.info("Registered component of type " + returnType + " with name " + componentData.value());
							});
							Map<String, Object> maps = new HashMap<>();
							maps.put(componentData.value(), nestedService);
							this.singletons.put(returnType, maps);
						}
					}
				} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
					e.printStackTrace();
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
			return (T) this.singletons.get(serviceClass).get(name);
		} else {
			return null;
		}
	}

}
