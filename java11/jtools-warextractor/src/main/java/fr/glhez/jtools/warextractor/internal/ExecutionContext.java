package fr.glhez.jtools.warextractor.internal;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.reducing;
import static java.util.stream.Collectors.toCollection;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import org.apache.commons.text.StringEscapeUtils;

public class ExecutionContext implements Iterable<Error> {
  private static final Set<FileFilterBuilder> DEFAULT_FILTERING = EnumSet.allOf(FileFilterBuilder.class);
  private static final List<String> DEFAULT_INCLUDES = List.of();
  private static final List<String> DEFAULT_EXCLUDES = List.of();

  private static final String NULL_RESOURCE = null;
  private static final Exception NULL_EXCEPTION = null;

  private final boolean dryRun;
  private final boolean inPlace;
  private final boolean verbose;
  private final Path cacheDirectory;
  private final Predicate<PathWrapper> fileMatcher;
  private final Set<FileFilterBuilder> fileFilterBuilders;
  private final SimpleEd archiveRenamer;

  private final List<Error> errors;

  public ExecutionContext(final boolean dryRun, final boolean inPlace, final boolean verbose, final Path cacheDirectory,
      final Predicate<PathWrapper> fileMatcher, final Set<FileFilterBuilder> fileFilterBuilders,
      final SimpleEd archiveRenamer) {
    this.dryRun = dryRun;
    this.inPlace = inPlace;
    this.verbose = verbose;
    this.cacheDirectory = cacheDirectory;
    this.fileMatcher = fileMatcher;
    this.fileFilterBuilders = fileFilterBuilders;
    this.archiveRenamer = archiveRenamer;
    this.errors = new ArrayList<>();
  }

  private static ExecutionContext build(final ExecutionContext.Builder builder) {
    // do some validation before
    final var fileFilterBuilders = Optional.ofNullable(builder.filtering).map(ExecutionContext::fileFilterBuilderMapper)
        .orElse(DEFAULT_FILTERING);
    final var includes = Objects.requireNonNullElse(builder.includes, DEFAULT_INCLUDES);
    final var excludes = Objects.requireNonNullElse(builder.excludes, DEFAULT_EXCLUDES);
    final var fileMatcher = toPredicate(includes, true).and(toPredicate(excludes, false).negate());
    final var archiveRenamer = SimpleEd.newSimpleEd(Objects.requireNonNullElseGet(builder.renameLib, List::of));

    return new ExecutionContext(builder.dryRun, builder.inPlace, builder.verbose, builder.cacheDirectory, fileMatcher,
        fileFilterBuilders, archiveRenamer);
  }

  private static Set<FileFilterBuilder> fileFilterBuilderMapper(final Set<String> s) {
    return s.stream().map(FileFilterBuilder::newBuilder)
        .collect(toCollection(() -> EnumSet.noneOf(FileFilterBuilder.class)));
  }

  public boolean accept(final PathWrapper path) {
    return this.fileMatcher.test(path);
  }

  public String rename(final PathWrapper pathWrapper) {
    return archiveRenamer.apply(Objects.toString(pathWrapper.getFileName(), ""));

  }

  public FileFilter getFilter(final PathWrapper source) {
    return fileFilterBuilders.stream().map(entry -> entry.accept(this, source)).filter(Objects::nonNull).findFirst()
        .orElse(null);
  }

  public Path getCacheDirectory() {
    return this.cacheDirectory;
  }

  public void cmd(final Object... args) {
    if (dryRun) {
      msg(() -> Arrays.stream(args).map(this::argToString).map(StringEscapeUtils::escapeXSI)
          .collect(joining(" ", "dry-run: ", "")));
    }
  }

  /**
   * Execute operation if dryRun is enabled.
   *
   * @param operation operation to run
   * @throws IOException thrown by operation.
   */
  public void execute(final IOOperation operation) throws IOException {
    Objects.requireNonNull(operation, "operation");
    if (!dryRun) {
      operation.execute();
    }
  }

  /**
   * Execute operation if dryRun is enabled, otherwise {@code shadowOperation} which must act
   * similar to operation but without doing anything.
   *
   * @param operation operation to run
   * @param shadowOperation shadow operation
   * @throws IOException thrown by operation.
   */
  public void execute(final IOOperation operation, final Runnable shadowOperation) throws IOException {
    Objects.requireNonNull(operation, "operation");
    Objects.requireNonNull(shadowOperation, "shadowOperation");
    if (dryRun) {
      shadowOperation.run();
    } else {
      operation.execute();
    }
  }

  public void verbose(final Supplier<String> message) {
    if (verbose) {
      msg(message);
    }
  }

  public void msg(final Supplier<String> message) {
    System.out.println(message.get());
  }

  public static String pathToString(final Path path) {
    return pathToString(path.toString());
  }

  public static String pathToString(final String path) {
    return path.toString().replace('\\', '/');
  }

  public String argToString(final Object o) {
    if (o instanceof Path) {
      return ExecutionContext.pathToString((Path) o);
    }
    return Objects.toString(o, "");
  }

  public void addError(final String message) {
    this.addError(message, NULL_RESOURCE, NULL_EXCEPTION);
  }

  public void addError(final String message, final Exception exception) {
    this.addError(message, NULL_RESOURCE, exception);
  }

  public void addError(final String message, final Path resource) {
    this.addError(message, pathToString(resource));
  }

  public void addError(final String message, final Path resource, final Exception exception) {
    this.addError(message, pathToString(resource), exception);
  }

  public void addError(final String message, final String resource) {
    this.addError(message, resource, NULL_EXCEPTION);
  }

  public void addError(final String message, final String resource, final Exception exception) {
    this.errors.add(new Error(message, resource, exception));
  }

  public boolean isInPlace() {
    return inPlace;
  }

  @Override
  public Iterator<Error> iterator() {
    return Collections.unmodifiableList(errors).iterator();
  }

  private static Predicate<PathWrapper> toPredicate(final List<String> filters, final boolean defaultValue) {
    if (filters.isEmpty()) {
      return v -> defaultValue;
    }
    return filters.stream().map(ExecutionContext::pathPredicate).collect(reducing(Predicate::or))
        .orElse(v -> defaultValue);
  }

  private static Predicate<PathWrapper> pathPredicate(final String pattern) {
    if (pattern.startsWith("path:")) {
      final var c = Pattern.compile(pattern.substring("path:".length())).asPredicate();
      return npath -> c.test(npath.getFullPath());
    }
    if (pattern.startsWith("name:")) {
      final var c = Pattern.compile(pattern.substring("name:".length())).asPredicate();
      return npath -> c.test(npath.getFileName());
    }
    final var c = Pattern.compile(pattern).asPredicate();
    return npath -> c.test(npath.getFileName());
  }

  @FunctionalInterface
  public interface IOOperation {
    void execute() throws IOException;
  }

  /**
   * Creates builder to build {@link ExecutionContext}.
   *
   * @return created builder
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builder to build {@link ExecutionContext}.
   */
  public static final class Builder {
    private boolean dryRun;
    private boolean inPlace;
    private boolean verbose;
    private Set<String> filtering;
    private List<String> includes;
    private List<String> excludes;
    private Path cacheDirectory;
    private List<String> renameLib;

    private Builder() {
    }

    public Builder setDryRun(final boolean dryRun) {
      this.dryRun = dryRun;
      return this;
    }

    public Builder setInPlace(final boolean inPlace) {
      this.inPlace = inPlace;
      return this;
    }

    public Builder setVerbose(final boolean verbose) {
      this.verbose = verbose;
      return this;
    }

    public Builder setFiltering(final Set<String> filtering) {
      this.filtering = filtering;
      return this;
    }

    public Builder setRenameLib(final List<String> renameLib) {
      this.renameLib = renameLib;
      return this;
    }

    public Builder setIncludes(final List<String> includes) {
      this.includes = includes;
      return this;
    }

    public Builder setExcludes(final List<String> excludes) {
      this.excludes = excludes;
      return this;
    }

    public Builder setCacheDirectory(final Path cacheDirectory) {
      this.cacheDirectory = cacheDirectory;
      return this;
    }

    public ExecutionContext build() {
      return ExecutionContext.build(this);
    }

  }

}
