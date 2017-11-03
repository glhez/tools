package fr.glhez.jtools.jar;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

/**
 * Information about a JAR file such as its origin and its child content (for deep archive).
 * 
 * @author gael.lhez
 */
public class JARInformation implements Comparable<JARInformation> {
  /**
   * Path of the archive being opened.
   */
  public final Path source;

  /**
   * Path in the archive being opened.
   */
  public final Optional<Path> realPath;

  /**
   * Path to the content. Same as jarInformation unless there is a {@link #realPath}.
   */
  public final Path tmpPath;

  private JARInformation(Path source, Optional<Path> realPath, Path tmpPath) {
    this.source = Objects.requireNonNull(source, "jarInformation");
    this.realPath = Objects.requireNonNull(realPath, "realPath");
    this.tmpPath = Objects.requireNonNull(tmpPath, "tmpPath");
  }

  public static JARInformation newJARInformation(Path source) {
    return new JARInformation(source, Optional.empty(), source);
  }

  public static JARInformation newJARInformation(Path source, Path realPath, Path tmpPath) {
    return new JARInformation(source, Optional.of(realPath), tmpPath);
  }

  @Override
  public int hashCode() {
    return source.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (null == obj || obj.getClass() != this.getClass())
      return false;
    JARInformation other = ((JARInformation) obj);
    return source.equals(other.source) && realPath.equals(other.realPath);
  }

  @Override
  public String toString() {
    return source.toString() + realPath.filter(p -> !p.equals(source)).map(p -> " [" + p + "]").orElse("");
  }

  @Override
  public int compareTo(JARInformation o) {
    int n = source.compareTo(o.source);
    if (n == 0) {
      return realPath.map(Object::toString).orElse("").compareTo(o.realPath.map(Object::toString).orElse(""));
    }
    return n;
  }
}