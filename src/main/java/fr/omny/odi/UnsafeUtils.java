package fr.omny.odi;

import java.lang.reflect.Array;

public class UnsafeUtils {

	@SuppressWarnings("unchecked")
	public static <T> T[] concatenate(T[] a, T[] b) {
		if (a == null && b == null) {
			throw new NullPointerException();
		}
		if (b == null)
			return a;
		if (a == null)
			return b;
		int aLen = a.length;
		int bLen = b.length;

		T[] c = (T[]) Array.newInstance(a.getClass().getComponentType(), aLen + bLen);
		System.arraycopy(a, 0, c, 0, aLen);
		System.arraycopy(b, 0, c, aLen, bLen);

		return c;
	}

	public static <T> boolean allEquals(T[] a, T[] b) {
		if (a == null || b == null) {
			return false;
		}
		int minLength = Math.min(a.length, b.length);
		for (int i = 0; i < minLength; i++) {
			if (a[i] != b[i])
				return false;
		}
		return true;
	}

}
