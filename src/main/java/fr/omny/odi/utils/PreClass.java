package fr.omny.odi.utils;


import java.util.ArrayList;
import java.util.List;

import lombok.Getter;

@Getter
public class PreClass {

	public static final PreClass NONE = null;

	private String className;
	private List<String> annotations = new ArrayList<>();

	public PreClass(AnnotationHarvester harvester) {
		this.className = harvester.getClassName();
		this.annotations = harvester.getAnnotations();
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

}
