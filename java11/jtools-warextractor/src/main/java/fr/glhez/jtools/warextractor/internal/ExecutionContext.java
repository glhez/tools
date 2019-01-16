package fr.glhez.jtools.warextractor.internal;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.reducing;

import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import org.apache.commons.text.StringEscapeUtils;

public class ExecutionContext implements Iterable<ExecutionContext.Error> {
  private static final String NULL_RESOURCE = null;
  private static final Exception NULL_EXCEPTION = null;

  private final boolean dryRun;
  private final boolean inPlace;
  private final boolean verbose;
  private final Set<String> filtering;
  private final Predicate<NPath> pathFilter;

  private final List<Error> errors;

  public ExecutionContext(final boolean dryRun, final boolean inPlace, final boolean verbose,
      final List<String> includes, final List<String> excludes, final Set<String> filtering) {
    this.dryRun = dryRun;
    this.inPlace = inPlace;
    this.verbose = verbose;
    this.pathFilter = toPredicate(includes, true).and(toPredicate(excludes, false).negate());
    this.filtering = filtering;
    this.errors = new ArrayList<>();
  }

  public boolean accept(final Path path) {
    return this.pathFilter.test(new NPath(path));
  }

  public FileFilter getFilter(final Path source) {
    final Path fn = source.getFileName();
    if (fn == null) {
      return null;
    }
    final String s = fn.toString();
    if (filtering.contains("class") && s.endsWith(".class")) {
      return new ClassFileFilter(this, source);
    }
    if (filtering.contains("properties") && s.endsWith(".properties")) {
      return new PropertiesFileFilter(this, source);
    }
    return null;
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
  public Iterator<ExecutionContext.Error> iterator() {
    return Collections.unmodifiableList(errors).iterator();
  }

  public static class Error {
    private final String message;
    private final String resource;
    private final Exception exception;

    public Error(final String message, final String resource, final Exception exception) {
      this.message = message;
      this.resource = resource;
      this.exception = exception;
    }

    public String getMessage() {
      return message;
    }

    public String getResource() {
      return resource;
    }

    @Override
    public String toString() {
      if (resource == null) {
        return message;
      }
      return message + " [resource: '" + resource + "']";
    }

    public void printStackTrace() {
      if (null != exception) {
        exception.printStackTrace();
      }
    }

    public void printStackTrace(final PrintStream s) {
      if (null != exception) {
        exception.printStackTrace(s);
      }
    }

    public void printStackTrace(final PrintWriter s) {
      if (null != exception) {
        exception.printStackTrace(s);
      }
    }
  }

  private static Predicate<NPath> toPredicate(final List<String> filters, final boolean defaultValue) {
    if (null == filters || filters.isEmpty()) {
      return v -> defaultValue;
    }
    return filters.stream().map(ExecutionContext::pathPredicate).collect(reducing(Predicate::or))
        .orElse(v -> defaultValue);
  }

  private static Predicate<NPath> pathPredicate(final String pattern) {
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

  static class NPath {
    private final Path path;
    private final String fullPath;
    private final String fileName;

    public NPath(final Path path) {
      this.path = path;
      this.fullPath = ExecutionContext.pathToString(path);
      this.fileName = Objects.toString(path.getFileName(), "");
    }

    public Path getPath() {
      return path;
    }

    public String getFullPath() {
      return fullPath;
    }

    public String getFileName() {
      return fileName;
    }

  }

  @FunctionalInterface
  public interface IOOperation {
    void execute() throws IOException;
  }

}
