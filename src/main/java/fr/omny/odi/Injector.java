package fr.omny.odi;


import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class Injector {

	private static Injector instance;
	private static Optional<Logger> logger;

	public static Optional<Logger> getLogger(){
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
	 * Perform autowiring
	 * 
	 * @param object
	 */
	public static void wire(Object object) {
		Utils.autowireNoException(object);
	}

	/**
	 * 
	 * @param klass
	 * @param object
	 */
	public static void addSpecial(Class<?> klass, Object object){
		instance.singletons.put(klass, object);
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
	 * 
	 * @param mainClass
	 */
	public static void addFrom(Class<?> mainClass) {
		addFrom(mainClass.getPackageName());
	}

	/**
	 * 
	 * @param packageName
	 */
	public static void addFrom(String packageName) {
		if (instance != null) {
			instance.add(packageName);
		}
	}

	/**
	 * Find each that correspond to validate the predicate
	 * @param o
	 * @return
	 */
	public static Stream<Object> findEach(Predicate<Object> o){
		return instance.singletons
			.values()
			.stream()
			.filter(o);
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
				Object serviceInstance = implementationClass.getConstructor().newInstance();
				this.singletons.put(implementationClass, serviceInstance);
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
					| NoSuchMethodException | SecurityException e) {
				e.printStackTrace();
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
