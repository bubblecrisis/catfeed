package utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

public class F {

	public static <E,G> Collection<G> each(Collection<E> e, Function<E,G> f) {
		Collection<G> g = new ArrayList();
		for (E element: e) {
			g.add( f.apply(element));
		}
		return g;
	}

	public static interface Function<E,G> {
		public G apply(E e);
	}
}
