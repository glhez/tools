package com.github.glhez.jtools.text;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

class Collections2Test {

  @Test
  void testCopyAsUnmodifiableSet() {
    final Set<Object> empty = Collections.emptySet();

    assertThat(Collections2.copyAsUnmodifiableSet(null)).isSameAs(empty);
    assertThat(Collections2.copyAsUnmodifiableSet(new HashSet<>())).isSameAs(empty);

    final HashSet<String> set = new HashSet<>(asList("A", "B"));
    assertThat(Collections2.copyAsUnmodifiableSet(set)).isEqualTo(set);
  }

  @Test
  void testCopyAsUnmodifiableNavigableSet() {
    final NavigableSet<Object> empty = Collections.emptyNavigableSet();
    assertThat(Collections2.copyAsUnmodifiableNavigableSet(null)).isSameAs(empty);
    assertThat(Collections2.copyAsUnmodifiableNavigableSet(new TreeSet<>())).isSameAs(empty);

    final TreeSet<String> set = new TreeSet<>(asList("A", "B"));
    assertThat(Collections2.copyAsUnmodifiableNavigableSet(set)).isEqualTo(set);
  }

  @Test
  void testCopyAsUnmodifiableList() {
    final List<Object> empty = Collections.emptyList();

    assertThat(Collections2.copyAsUnmodifiableList(null)).isSameAs(empty);
    assertThat(Collections2.copyAsUnmodifiableList(new ArrayList<>())).isSameAs(empty);

    final List<String> set = asList("A", "B");
    assertThat(Collections2.copyAsUnmodifiableList(set)).isEqualTo(set);
  }

}
