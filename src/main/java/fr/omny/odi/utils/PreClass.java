package fr.omny.odi.utils;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Getter;

@Getter
public class PreClass {

	public static final PreClass NONE = null;

	private String packageName;
	private String className;
	private String superClass;
	private Map<String, Map<String, String>> annotations = new HashMap<>();
	private List<String> interfaces = new ArrayList<>();

	public PreClass(String packageName, BytecodeHarvester harvester) {
		this.packageName = packageName;
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

	public boolean isNotInner() {
		return !this.className.contains("$");
	}

}
