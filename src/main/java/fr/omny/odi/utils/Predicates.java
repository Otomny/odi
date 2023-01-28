package fr.omny.odi.utils;

import java.util.function.Predicate;

public class Predicates {
	
	private Predicates(){}

	/**
	 * 
	 * @param <T>
	 * @return
	 */
	public static <T> Predicate<T> alwaysTrue(){
		return (object) -> true;
	}

	/**
	 * 
	 * @param <T>
	 * @return
	 */
	public static <T> Predicate<T> alwaysFalse(){
		return (object) -> false;
	}

}
