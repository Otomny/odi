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
	private static Optional<Logger> logger;
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
					instance = new Injector();
					Injector.logger = Optional.ofNullable(logger);
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
		if (instance.singletons.containsKey(klass))
			return;
		instance.singletons.put(klass, object);
	}

	/**
	 * Call a service constructor with the desired parameters and add them to services
	 * 
	 * @param klass
	 * @param parameters
	 */
	public static void addServiceParams(Class<?> klass, Object... parameters) {
		try {
			if (instance.singletons.containsKey(klass))
				return;
			instance.singletons.put(klass, Utils.callConstructor(klass, parameters));
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			e.printStackTrace();
		}
	}

	public static <T> T getService(Class<T> klass) {
		try {
			return instance.getServiceInstance(klass);
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
	public static Stream<Object> findEach(Predicate<Object> o) {
		return instance.singletons.values().stream().filter(o);
	}

	/**
	 * All instances of a service
	 */
	private Map<Class<?>, Object> singletons;

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
				addMethodReturns(implementationClass, serviceInstance);
				this.singletons.put(implementationClass, serviceInstance);
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
	public void addMethodReturns(Class<?> implementationClass, Object serviceInstance) {
		for (Method method : implementationClass.getDeclaredMethods()) {
			if (method.isAnnotationPresent(Component.class)) {
				method.setAccessible(true);
				// Inject autowired arguments
				try {
					Class<?> returnType = method.getReturnType();
					if (returnType != void.class) {
						if (this.singletons.containsKey(returnType))
							continue;
						Object service = Utils.callMethod(method, implementationClass, serviceInstance, new Object[] {});
						this.singletons.put(returnType, service);
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
	private <T> T getServiceInstance(Class<?> serviceClass) {
		if (this.singletons.containsKey(serviceClass)) {
			return (T) this.singletons.get(serviceClass);
		} else {
			return null;
		}
	}

}
