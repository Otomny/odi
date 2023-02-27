package fr.omny.odi.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Getter;

@Getter
/**
 * Represent a non loaded state of a class
 */
public class PreClass {

	public static final PreClass NONE = null;

	private String fullPath;
	private String packageName;
	private String className;
	private String superClass;
	private Map<String, Map<String, String>> annotations = new HashMap<>();
	private List<String> interfaces = new ArrayList<>();

	/**
	 * Constructor
	 * @param packageName The package path (with . separator) to the class
	 * @param fullPath The file path to the class in the jar (with / separator)
	 * @param harvester The bytecode harvester used to analyse class structure
	 */
	public PreClass(String packageName, String fullPath, BytecodeHarvester harvester) {
		this.packageName = packageName;
		this.fullPath = fullPath;
		this.className = harvester.getClassName();
		this.interfaces = harvester.getInterfaces();
		this.superClass = harvester.getSuperClass();
		this.annotations = harvester.getAnnotationsDatas();
	}

	/**
	 * Test if annotation is present on a class depending on a pre state loading
	 * 
	 * @param annotation
	 * @return
	 */
	public boolean isAnnotationPresent(Class<?> annotation) {
		if (annotation == null)
			return false;
		return this.annotations.containsKey(annotation.getCanonicalName().replace(".", "/"));
	}

	/**
	 * test if interface is present on a class depending on a pre state loading
	 * 
	 * @param interface_
	 * @return
	 */
	public boolean isInterfacePresent(Class<?> intt) {
		if (intt == null)
			return false;
		return this.interfaces.contains(intt.getCanonicalName().replace(".", "/"));
	}

	/**
	 * Test if the super class of the pre state data is equal to the parameter
	 * 
	 * @param superClass
	 * @return
	 */
	public boolean isSuperClass(Class<?> superClass) {
		if (superClass == null)
			return false;
		return this.superClass.equalsIgnoreCase(superClass.getCanonicalName().replace(".", "/"));
	}

	/**
	 * 
	 * @return True if class is not an inner class, false otherwise
	 */
	public boolean isNotInner() {
		return !this.className.contains("$");
	}

	/**
	 * 
	 * @return True if class is not a bytebuddy generated class, false otherwise
	 */
	public boolean isNotByteBuddy() {
		return !this.fullPath.contains("$ByteBuddy");
	}

}
