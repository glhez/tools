package com.github.glhez.jtools.warextractor.internal;

import static java.util.stream.Collectors.reducing;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import com.github.glhez.jtools.warextractor.internal.filter.ChainFilter;

public class ExecutionContext {
  private static final List<String> DEFAULT_FILTERING = List.of("cfr", "properties", "sql");
  private static final List<String> DEFAULT_INCLUDES = List.of();
  private static final List<String> DEFAULT_EXCLUDES = List.of();

  private final FilesProxy filesProxy;
  private final boolean inPlace;
  private final Path cacheDirectory;
  private final Predicate<PathWrapper> fileMatcher;
  private final ChainFilter filterChain;
  private final SimpleEd archiveRenamer;

  public ExecutionContext(final FilesProxy filesProxy, final boolean inPlace, final Path cacheDirectory,
      final Predicate<PathWrapper> fileMatcher, final ChainFilter filterChain, final SimpleEd archiveRenamer) {
    this.filesProxy = filesProxy;
    this.inPlace = inPlace;
    this.cacheDirectory = cacheDirectory;
    this.fileMatcher = fileMatcher;
    this.filterChain = filterChain;
    this.archiveRenamer = archiveRenamer;
  }

  private static ExecutionContext build(final ExecutionContext.Builder builder) {
    // do some validation before
    final var filtering = ChainFilter.newChainFilterBuilder(Optional.ofNullable(builder.filtering)
                                                                    .orElse(DEFAULT_FILTERING));
    final var includes = Objects.requireNonNullElse(builder.includes, DEFAULT_INCLUDES);
    final var excludes = Objects.requireNonNullElse(builder.excludes, DEFAULT_EXCLUDES);
    final var fileMatcher = toPredicate(includes, true).and(toPredicate(excludes, false).negate());
    final var archiveRenamer = SimpleEd.newSimpleEd(Objects.requireNonNullElseGet(builder.renameLib, List::of));
    final var filesProxy = builder.dryRun ? new DryRunFilesProxy() : new JavaBaseFilesProxy();
    return new ExecutionContext(filesProxy, builder.inPlace, builder.cacheDirectory, fileMatcher, filtering,
        archiveRenamer);
  }

  public Path getCacheDirectory() {
    return this.cacheDirectory;
  }

  public FilesProxy getFilesProxy() {
    return filesProxy;
  }

  public Predicate<PathWrapper> getFileMatcher() {
    return fileMatcher;
  }

  public ChainFilter getFilterChain() {
    return filterChain;
  }

  public SimpleEd getArchiveRenamer() {
    return archiveRenamer;
  }

  public boolean isInPlace() {
    return this.inPlace;
  }

  private static Predicate<PathWrapper> toPredicate(final List<String> filters, final boolean defaultValue) {
    if (filters.isEmpty()) {
      return v -> defaultValue;
    }
    return filters.stream()
                  .map(ExecutionContext::pathPredicate)
                  .collect(reducing(Predicate::or))
                  .orElse(v -> defaultValue);
  }

  public static Predicate<PathWrapper> pathPredicate(final String pattern) {
    if (pattern.startsWith("path:")) {
      final var c = Pattern.compile(pattern.substring("path:".length())).asPredicate();
      return npath -> c.test(npath.getFullPath());
    }
    if (pattern.startsWith("name:")) {
      final var c = Pattern.compile(pattern.substring("name:".length())).asPredicate();
      return npath -> c.test(npath.getFileName().fileName);
    }
    if (pattern.startsWith("ext:")) {
      final var c = Pattern.compile(pattern.substring("ext:".length())).asPredicate();
      return npath -> c.test(npath.getFileName().extension);
    }
    if (pattern.startsWith("noext:")) {
      final var c = Pattern.compile(pattern.substring("noext:".length())).asPredicate();
      return npath -> c.test(npath.getFileName().fileNameWithoutExtension);
    }
    final var c = Pattern.compile(pattern).asPredicate();
    return npath -> c.test(npath.getFileName().fileName);
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
    boolean dryRun;
    boolean inPlace;
    List<String> filtering;
    List<String> includes;
    List<String> excludes;
    Path cacheDirectory;
    List<String> renameLib;

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

    public Builder setFiltering(final List<String> filtering) {
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
