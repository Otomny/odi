package fr.omny.odi.utils;

import java.util.function.Predicate;

public final class Predicates {
	
	private Predicates(){}

	/**
	 * Always true
	 * @param <T>
	 * @return
	 */
	public static <T> Predicate<T> alwaysTrue(){
		return (object) -> true;
	}

	/**
	 * Always false
	 * @param <T>
	 * @return
	 */
	public static <T> Predicate<T> alwaysFalse(){
		return (object) -> false;
	}

}
