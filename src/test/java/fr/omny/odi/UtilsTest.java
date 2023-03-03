package fr.omny.odi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.List;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

@SuppressWarnings("unused")
public class UtilsTest {

	@Test
	public void test_FindAllFields() {

		List<Field> allFields = Utils.findAllFields(NPC.class);

		Predicate<Predicate<Field>> searchField = predicate -> allFields.stream()
				.filter(predicate)
				.findFirst()
				.isPresent();

		assertEquals(2, allFields.size());
		assertTrue(searchField.test(field -> field.getName().equals("name")));
		assertTrue(searchField.test(field -> field.getName().equals("age")));

	}

	@Test
	public void test_FindAllFields_Superclass() {
		List<Field> allFields = Utils.findAllFields(Child.class);

		Predicate<Predicate<Field>> searchField = predicate -> allFields.stream()
				.filter(predicate)
				.findFirst()
				.isPresent();

		assertEquals(3, allFields.size());
		assertTrue(searchField.test(field -> field.getName().equals("name")));
		assertTrue(searchField.test(field -> field.getName().equals("age")));
		assertTrue(searchField.test(field -> field.getName().equals("favoriteToy")));
	}

	public static class NPC {

		private String name;
		private int age;

	}

	public static class Child extends NPC {

		private String favoriteToy;

	}

}
