package fr.glhez.jtools.warextractor.internal.filter;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toUnmodifiableList;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

import fr.glhez.jtools.warextractor.internal.ExecutionContext;
import fr.glhez.jtools.warextractor.internal.PathWrapper;

/**
 * Produce {@link InputStreamFilter}.
 *
 * @author gael.lhez
 */
public class InputStreamFilterChain {
  private final List<PredicateAndFilter> chain;

  private InputStreamFilterChain(final List<PredicateAndFilter> chain) {
    this.chain = chain;
  }

  /**
   * Apply filter to {@code path} if it match.
   *
   * @param context parent context
   * @param path a path
   * @return returns <code>null</code> if the builder does not filter the file. The caller is to use
   *         {@link Files#copy(java.nio.file.Path, java.nio.file.Path, java.nio.file.CopyOption...)}
   *         to copy the file.
   * @throws IOException
   */
  public InputStreamWithCharset filter(final ExecutionContext context, final PathWrapper path) throws IOException {
    final var chain = this.chain.stream().filter(filter(path)).map(PredicateAndFilter::getFilter)
        .toArray(InputStreamFilter[]::new);

    if (chain.length == 0) {
      return null;
    }

    var root = InputStreamWithCharset.open(path.getPath());
    for (final InputStreamFilter element : chain) {
      root = element.filter(context, root);
    }
    return root;

  }

  public static InputStreamFilterChain newInputStreamChain(final List<String> parameters) {
    requireNonNull(parameters, "parameters");
    return new InputStreamFilterChain(
        parameters.stream().map(InputStreamFilterChain::parse).collect(toUnmodifiableList()));
  }

  private static PredicateAndFilter parse(final String value) {
    Objects.requireNonNull(value, "value");

    final int n = value.indexOf('=');
    final String filter;
    final String pathFilter;
    if (n == -1) {
      filter = value;
      pathFilter = null;
    } else {
      filter = value.substring(0, n);
      pathFilter = value.substring(n + 1);
    }
    final var known = KnownFileFilters.valueOf(filter.toUpperCase());
    final var predicate = null == pathFilter ? known.predicate : ExecutionContext.pathPredicate(pathFilter);

    return new PredicateAndFilter(predicate, known.filter);
  }

  static Predicate<PredicateAndFilter> filter(final PathWrapper wrapper) {
    return paf -> paf.predicate.test(wrapper);
  }

  static class PredicateAndFilter {
    final Predicate<PathWrapper> predicate;
    final InputStreamFilter filter;

    public PredicateAndFilter(final Predicate<PathWrapper> wrapper, final InputStreamFilter filter) {
      this.predicate = wrapper;
      this.filter = filter;
    }

    public Predicate<PathWrapper> getPredicate() {
      return predicate;
    }

    public InputStreamFilter getFilter() {
      return filter;
    }

  }

  public enum KnownFileFilters {
    ASM("name:[.]class$", new ASMFileFilter()),
    CFR("name:[.]class$", new CFRFileFilter()),
    SQL("name:[.]sql$", new SQLFileFilter()),
    PROPERTIES(".properties", new PropertiesFileFilter());

    private final Predicate<PathWrapper> predicate;
    private final InputStreamFilter filter;

    private KnownFileFilters(final String defaultPredicate, final InputStreamFilter filter) {
      this.predicate = ExecutionContext.pathPredicate(defaultPredicate);
      this.filter = filter;
    }

    public Predicate<PathWrapper> getPredicate() {
      return predicate;
    }

    public InputStreamFilter getFilter() {
      return filter;
    }

  }
}
