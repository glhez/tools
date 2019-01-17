package fr.glhez.jtools.warextractor.internal;

import java.nio.file.Path;
import java.util.Objects;
import java.util.function.BiFunction;

/**
 * Produce {@link FileFilter}.
 *
 * @author gael.lhez
 */
public enum FileFilterBuilder {
  CLASS(".class", ClassFileFilter::new),
  PROPERTIES(".properties", PropertiesFileFilter::new);

  private final String fileNameSuffix;
  private final BiFunction<ExecutionContext, Path, FileFilter> builder;

  private FileFilterBuilder(final String fileNameSuffix, final BiFunction<ExecutionContext, Path, FileFilter> builder) {
    this.fileNameSuffix = fileNameSuffix;
    this.builder = builder;
  }

  /**
   * Apply filter to {@code path} if it match.
   *
   * @param context parent context
   * @param path a path
   * @return returns <code>null</code> if the builder does not filter the file.
   */
  public FileFilter accept(final ExecutionContext context, final PathWrapper path) {
    final String fileName = path.getFileName();
    if (null == fileName || !fileName.endsWith(fileNameSuffix)) {
      return null;
    }
    return builder.apply(context, path.getPath());
  }

  /**
   * Get a {@link FileFilterBuilder} given its name.
   *
   * @param value a filter name. Case insensitive.
   * @return a {@link FileFilterBuilder}
   * @throws IllegalArgumentException
   */
  public static FileFilterBuilder newBuilder(final String value) {
    Objects.requireNonNull(value, "value");
    return FileFilterBuilder.valueOf(value.toUpperCase());
  }
}
