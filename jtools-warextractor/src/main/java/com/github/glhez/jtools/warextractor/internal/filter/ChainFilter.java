package com.github.glhez.jtools.warextractor.internal.filter;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toUnmodifiableList;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

import com.github.glhez.jtools.warextractor.internal.ExecutionContext;
import com.github.glhez.jtools.warextractor.internal.PathWrapper;

/**
 * Produce {@link Filter}.
 *
 * @author gael.lhez
 */
public class ChainFilter {
  /** Logger */
  private static final org.apache.logging.log4j.Logger logger = org.apache.logging.log4j.LogManager.getLogger(ChainFilter.class);

  private final List<PredicateAndFilter> filters;

  private ChainFilter(final List<PredicateAndFilter> chain) {
    this.filters = chain;
  }

  public void filter(final Set<Path> copiedFiles) {
    // first regroup files per chain, this is to display statistics
    final var map = copiedFiles.stream()
                               .map(PathWrapper::new)
                               .map(this::map)
                               .collect(groupingBy(Map.Entry::getKey, mapping(Map.Entry::getValue, toList())));

    for (final var group : map.entrySet()) {
      final var filters = group.getKey().array;
      final var fileset = group.getValue();

      if (filters.length > 0) {
        final var total = fileset.size();
        logger.info("filtering {} files using {}", () -> total, () -> Arrays.toString(filters));
        var index = 0;
        for (final var path : fileset) {
          filter(index, total, path, filters);
          ++index;
        }
      }
    }
  }

  private Map.Entry<ArrayWrapper<Filter>, Path> map(final PathWrapper wrapper) {
    final var filters = this.filters.stream()
                                    .filter(paf -> paf.predicate.test(wrapper))
                                    .map(PredicateAndFilter::getFilter)
                                    .toArray(Filter[]::new);
    return Map.entry(new ArrayWrapper<>(filters), wrapper.getPath());
  }

  private void filter(final int index, final int total, final Path file, final Filter[] filters) {
    logger.debug("filtering {} ({}): {}", () -> index, () -> String.format("%3.2f%%", 100.0
        * (index / (double) total)), () -> file);
    try {
      var root = InputStreamWithCharset.open(file);
      for (final Filter element : filters) {
        root = element.filter(root);
      }
      Files.copy(root.getBufferedStream(), file, StandardCopyOption.REPLACE_EXISTING);
    } catch (final IOException e) {
      logger.error("unable to filter file: {}", file, e);
    }
  }

  public static ChainFilter newChainFilterBuilder(final List<String> parameters) {
    requireNonNull(parameters, "parameters");
    return new ChainFilter(parameters.stream().map(ChainFilter::parse).collect(toUnmodifiableList()));
  }

  private static PredicateAndFilter parse(final String value) {
    Objects.requireNonNull(value, "value");

    final var n = value.indexOf('=');
    final String filter;
    final String pathFilter;
    if (n == -1) {
      filter = value;
      pathFilter = null;
    } else {
      filter = value.substring(0, n);
      pathFilter = value.substring(n + 1);
    }
    final var known = KnownFileFilter.valueOf(filter.toUpperCase());
    final var predicate = null == pathFilter ? known.predicate : ExecutionContext.pathPredicate(pathFilter);

    return new PredicateAndFilter(predicate, known.filter);
  }

  static class PredicateAndFilter {
    final Predicate<PathWrapper> predicate;
    final Filter filter;

    public PredicateAndFilter(final Predicate<PathWrapper> wrapper, final Filter filter) {
      this.predicate = wrapper;
      this.filter = filter;
    }

    public Predicate<PathWrapper> getPredicate() {
      return this.predicate;
    }

    public Filter getFilter() {
      return this.filter;
    }

  }

  public enum KnownFileFilter {
    ASM("name:[.]class$", ASMFileFilter.INSTANCE),
    CFR("name:[.]class$", CFRFileFilter.INSTANCE),
    SQL("name:[.]sql$", SQLFileFilter.INSTANCE),
    PROPERTIES(".properties", PropertiesFileFilter.INSTANCE);

    private final Predicate<PathWrapper> predicate;
    private final Filter filter;

    KnownFileFilter(final String defaultPredicate, final Filter filter) {
      this.predicate = ExecutionContext.pathPredicate(defaultPredicate);
      this.filter = filter;
    }

    public Predicate<PathWrapper> getPredicate() {
      return this.predicate;
    }

    public Filter getFilter() {
      return this.filter;
    }

  }

  static class ArrayWrapper<E> {
    private final E[] array;

    public ArrayWrapper(final E[] array) {
      this.array = array;
    }

    @Override
    public int hashCode() {
      final var prime = 31;
      var result = 1;
      result = prime * result + Arrays.deepHashCode(array);
      return result;
    }

    @Override
    public boolean equals(final Object obj) {
      if (this == obj) {
        return true;
      }
      if ((obj == null) || (getClass() != obj.getClass())) {
        return false;
      }
      final ArrayWrapper<?> other = (ArrayWrapper<?>) obj;
      return Arrays.deepEquals(array, other.array);
    }

  }
}
