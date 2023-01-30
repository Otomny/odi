package fr.omny.odi.utils;


import java.util.ArrayList;
import java.util.List;

import lombok.Getter;

@Getter
public class PreClass {

	public static final PreClass NONE = null;

	private String className;
	private String superClass;
	private List<String> annotations = new ArrayList<>();
	private List<String> interfaces = new ArrayList<>();

	public PreClass(BytecodeHarvester harvester) {
		this.className = harvester.getClassName();
		this.annotations = harvester.getAnnotations();
		this.interfaces = harvester.getInterfaces();
		this.superClass = harvester.getSuperClass();
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
		return this.annotations.contains(annotation.getCanonicalName().replace(".", "/"));
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
	 * @param superClass
	 * @return
	 */
	public boolean isSuperClass(Class<?> superClass) {
		if (superClass == null)
			return false;
		return this.superClass.equalsIgnoreCase(superClass.getCanonicalName().replace(".", "/"));
	}

}
