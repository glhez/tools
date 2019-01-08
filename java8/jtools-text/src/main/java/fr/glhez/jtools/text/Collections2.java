package fr.glhez.jtools.text;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyNavigableSet;
import static java.util.Collections.emptySet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;

/**
 * Some method to generate unmodifiable copy of {@link Collection}.
 *
 * @author gael.lhez
 */
class Collections2 {
  private Collections2() {
  }

  public static <E> Set<E> copyAsUnmodifiableSet(final Set<? extends E> set) {
    if (null == set || set.isEmpty()) {
      return emptySet();
    }
    return Collections.unmodifiableSet(new LinkedHashSet<>(set));
  }

  // no ? extends E because TreeSet does have 2 matching constructors:
  // TreeSet(Collection<? extends E>)
  // TreeSet(SortedSet<E>)
  // it seems that Java pick up the later (because NavigableSet extends SortedSet and is more specific
  // than Collection), but in a case where it should not.
  // Since we still want to keep the comparator, we remove the ? extends.
  public static <E> NavigableSet<E> copyAsUnmodifiableNavigableSet(final NavigableSet<E> set) {
    if (null == set || set.isEmpty()) {
      return emptyNavigableSet();
    }
    return Collections.unmodifiableNavigableSet(new TreeSet<>(set));
  }

  public static <E> List<E> copyAsUnmodifiableList(final List<? extends E> list) {
    if (null == list || list.isEmpty()) {
      return emptyList();
    }
    return Collections.unmodifiableList(new ArrayList<>(list));
  }
}
