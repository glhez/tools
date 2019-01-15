package fr.glhez.jtools.jar.internal;

import static fr.glhez.jtools.jar.internal.JARInformation.newJARInformation;
import static java.util.stream.Collectors.reducing;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class JARFileLocator implements AutoCloseable {
  private final SortedSet<JARInformation> files;
  private final FileErrors errors;
  private final DeepMode deepMode;
  private final List<Path> tempFiles;
  private final Predicate<NPath> filter;
  private final Predicate<NPath> deepInclude;

  private JARFileLocator(final DeepMode deepMode, final Predicate<NPath> filter, final Predicate<NPath> deepInclude) {
    this.deepMode = Objects.requireNonNull(deepMode, "deepMode");
    this.files = new TreeSet<>();
    this.errors = new FileErrors();
    this.tempFiles = new ArrayList<>();
    this.filter = filter;
    this.deepInclude = deepInclude;
  }

  public JARFileLocator(final DeepMode deepMode, final List<String> includes, final List<String> excludes,
      final List<String> deepInclude) {
    this(deepMode, toPredicate(includes, true).and(toPredicate(excludes, false).negate()),
        toPredicate(deepInclude, true));
  }

  private static Predicate<NPath> toPredicate(final List<String> filters, final boolean defaultValue) {
    if (null == filters || filters.isEmpty()) {
      return v -> defaultValue;
    }
    return filters.stream().map(JARFileLocator::pathPredicate).collect(reducing(Predicate::or))
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

  public void addFileset(final List<Path> files) {
    if (null != files) {
      for (final var file : files) {
        try {
          final var attributes = Files.getFileAttributeView(file, BasicFileAttributeView.class).readAttributes();
          addEntry(attributes, file);
        } catch (final IOException e) {
          errors.addError(file, e);
          continue;
        }
      }
    }
  }

  private void addEntry(final BasicFileAttributes attributes, final Path file) {
    if (attributes.isRegularFile()) {
      addFile(file);
    } else if (attributes.isDirectory()) {
      addDirectory(file);
    } else {
      errors.addError(file, "Not a regular file or directory");
    }
  }

  private void addFile(final Path entry) {
    try {
      deepAdd(entry.toRealPath());
    } catch (final IOException e) {
      errors.addError(entry, e);
    }
  }

  private void addDirectory(final Path entry) {
    try {
      filter(this.filter, Files.find(entry.toRealPath(), Integer.MAX_VALUE, deepMode)).forEach(this::deepAdd);
    } catch (final IOException e) {
      errors.addError(entry, e);
    }
  }

  private Stream<Path> filter(final Predicate<NPath> filter, final Stream<Path> source) {
    return source.map(NPath::new).filter(filter).map(NPath::getPath);
  }

  private void deepAdd(final Path path) {
    this.files.add(newJARInformation(path));
    if (deepMode.shouldDescendIntoFile(path)) {
      processArchive(path);
    }
  }

  private void processArchive(final Path realPath) {
    try (final FileSystem fs = FileSystems.newFileSystem(realPath, null)) {
      fs.getRootDirectories().forEach(root -> {
        try (var fileset = Files.find(root, Integer.MAX_VALUE, DeepMode.DISABLED)) {
          filter(deepInclude, fileset.filter(deepMode::isArchivePath)).forEach(child -> {
            try {
              final Path tempFile = Files.createTempFile("jarfile-" + child.getFileName().toString(), ".jar");
              Files.copy(child, tempFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
              tempFiles.add(tempFile);
              files.add(newJARInformation(realPath, child, tempFile));
            } catch (final IOException e) {
              errors.addError(root, e);
            }
          });
        } catch (final IOException e) {
          errors.addError(root, e);
        }
      });
    } catch (final IOException e) {
      errors.addError(realPath, e);
    }
  }

  public Set<JARInformation> getFiles() {
    return files;
  }

  public FileErrors getErrors() {
    return errors;
  }

  public boolean hasErrors() {
    return !errors.isEmpty();
  }

  public enum DeepMode implements BiPredicate<Path, BasicFileAttributes> {
    ALL {
      @Override
      public boolean isArchivePath(final Path path) {
        return true;
      }
    },
    STD {
      @Override
      public boolean isArchivePath(final Path path) {
        return path.startsWith("/META-INF/lib");
      }
    },
    DISABLED {

      @Override
      protected boolean shouldDescendIntoFile(final String file) {
        return false;
      }

      @Override
      public boolean isArchivePath(final Path path) {
        return false;
      }

    };

    public boolean shouldDescendIntoFile(final Path file) {
      return shouldDescendIntoFile(file.toString().toLowerCase());
    }

    /**
     * Returns <code>true</code> if the path inside an archive should be considered.
     * <p>
     * The method may for example check if file starts with META-INF/lib.
     *
     * @param file
     * @return
     */
    public abstract boolean isArchivePath(final Path file);

    /**
     * Implmementation of {@link BiPredicate} for
     * {@link Files#find(Path, int, BiPredicate, java.nio.file.FileVisitOption...)}.
     */
    @Override
    public boolean test(final Path file, final BasicFileAttributes attrs) {
      return isValidFile(file.toString().toLowerCase());
    }

    /**
     * Determine if the file should be accepted as a deep file.
     * <p>
     * By default, only EAR and WAR may contains additional JAR file to be processed.
     *
     * @param file
     *          path to be tested (file system dependent)
     * @return <code>true</code> if the file is valid, eg: can be processed.
     */
    protected boolean shouldDescendIntoFile(final String file) {
      return file.endsWith(".ear") || file.endsWith(".war");
    }

    /**
     * Determine if the file should be accepted.
     * <p>
     * The default method delegate to {@link #shouldDescendIntoFile(String)} and accept by default
     * any JAR file not
     * being a sources JAR.
     *
     * @param file
     *          path to be tested (file system dependent)
     * @return <code>true</code> if the file is valid, eg: can be processed.
     */
    protected boolean isValidFile(final String file) {
      return shouldDescendIntoFile(file) || file.endsWith(".jar") && !file.endsWith("-sources.jar");
    }
  }

  @Override
  public void close() {
    for (final Path path : tempFiles) {
      try {
        Files.deleteIfExists(path);
      } catch (@SuppressWarnings("unused") final IOException e) {
        // ignored.
      }
    }

  }

  /**
   * Normalized path, for use with filters.
   *
   * @author gael.lhez
   */
  static class NPath {
    private final Path path;
    private final String fullPath;
    private final String fileName;

    public NPath(final Path path) {
      this.path = path;
      this.fullPath = path.toString().replace("\\", "/");
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

}
