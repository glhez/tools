package com.github.glhez.fileset;

import static java.util.stream.Collectors.reducing;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

/**
 * Build filter (includes, excludes).
 *
 * @author gael.lhez
 */
public class CollectedFilePredicateBuilder {
  private final Map<String, Pattern> patternCache;

  private final Map<String, Function<CollectedFile, String>> mapper = Map.of("complete:", CollectedFile::getCompletePath,
                                                                             "path:", CollectedFile::getPathAsString,
                                                                             "name:", CollectedFile::getFileName,
                                                                             "ext:", CollectedFile::getExtension);

  public CollectedFilePredicateBuilder() {
    this.patternCache = new HashMap<>();
  }

  private static Collection<String> emptyIfNull(final Collection<String> collection) {
    return collection == null ? Collections.emptyList() : collection;
  }

  /**
   * Convert a set of includes and excludes into a predicate.
   *
   * @param includes
   *          file to includes, can be <code>null</code> or empty (which is same as
   *          <code>true</code>)
   * @param excludes
   *          file to excludes, can be <code>null</code> or empty (which is same as
   *          <code>false</code>)
   * @return a predicate accepting file given includes and excludes.
   */
  public Predicate<CollectedFile> convert(final Collection<String> includes, final Collection<String> excludes) {
    final var inc = convert(includes);
    final var exc = convert(excludes).map(Predicate::negate);

    // if includes is present, then and it with exclude.
    final UnaryOperator<Predicate<CollectedFile>> iem = ip -> exc.map(ip::and).orElse(ip);
    // otherwise, get it or always return true.
    final Supplier<Predicate<CollectedFile>> eem = () -> exc.orElseGet(() -> f -> true);
    return inc.map(iem).orElseGet(eem);
  }

  /**
   * Convert several pattern by making a OR between each.
   *
   * @param patterns
   *          set of pattern, can be <code>null</code>.
   * @return a predicate wrapped in an {@link Optional}.
   */
  public Optional<Predicate<CollectedFile>> convert(final Collection<String> patterns) {
    return emptyIfNull(patterns).stream().map(this::convert).collect(reducing(Predicate::or));
  }

  /**
   * Convert a pattern into a {@link Predicate}.
   * <p>
   * A pattern may starts with a prefix (see {@link #mapper}} which apply only on part of
   * {@link CollectedFile}.
   *
   * @param pattern
   *          a pattern
   * @return some predicate
   */
  public Predicate<CollectedFile> convert(final String pattern) {
    for (final var entry : mapper.entrySet()) {
      final var prefix = entry.getKey();
      if (pattern.startsWith(prefix)) {
        return convert(pattern.substring(prefix.length()), entry.getValue());
      }
    }
    return convert(pattern, CollectedFile::getFileName);
  }

  private Predicate<CollectedFile> convert(final String pattern, final Function<CollectedFile, String> value) {
    final var predicate = patternCache.computeIfAbsent(pattern, Pattern::compile).asPredicate();
    return file -> predicate.test(value.apply(file));
  }

}
