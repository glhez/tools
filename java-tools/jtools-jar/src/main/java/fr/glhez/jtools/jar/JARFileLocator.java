package fr.glhez.jtools.jar;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.BiPredicate;

public class JARFileLocator implements AutoCloseable {
  private final SortedSet<Path> files;
  private final FileErrors errors;
  private final DeepMode deepMode;
  private final List<Path> tempFiles;

  public JARFileLocator(final DeepMode deepMode) {
    this.deepMode = Objects.requireNonNull(deepMode, "deepMode");
    this.files = new TreeSet<>();
    this.errors = new FileErrors();
    this.tempFiles = new ArrayList<>();
  }

  public void addFiles(final String[] files) {
    if (null == files) {
      return;
    }
    for (final String value : files) {
      final Path entry = Paths.get(value);
      if (!Files.isRegularFile(entry)) {
        errors.addError(entry, "Not a regular file");
      } else {
        try {
          deepAdd(entry.toRealPath());
        } catch (final IOException e) {
          errors.addError(entry, e);
        }
      }
    }
  }

  public void addDirectories(final String[] directories) {
    if (null == directories) {
      return;
    }
    for (final String value : directories) {
      final Path entry = Paths.get(value);
      if (!Files.isDirectory(entry)) {
        errors.addError(entry, "Not a directory");
      } else {
        try {
          Files.find(entry.toRealPath(), Integer.MAX_VALUE, deepMode).forEach(this::deepAdd);
        } catch (final IOException e) {
          errors.addError(entry, e);
        }
      }
    }
  }

  private void deepAdd(final Path realPath) {
    this.files.add(realPath);
    if (deepMode.shouldDescendIntoFile(realPath)) {
      processArchive(realPath);
    }
  }

  /**
   * Process a path as being an archive.
   * <P>
   * Each archive will be
   * 
   * @param realPath
   */
  private void processArchive(final Path realPath) {
    try (final FileSystem fs = FileSystems.newFileSystem(realPath, null)) {
      fs.getRootDirectories().forEach(root -> {
        try {
          Files.find(root, Integer.MAX_VALUE, DeepMode.DISABLED).filter(deepMode::isArchivePath).forEach(child -> {
            try {
              final Path tempFile = Files.createTempFile("jarfile-" + child.getFileName().toString(), ".jar");
              Files.copy(child, tempFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
              tempFiles.add(tempFile);
              files.add(tempFile);
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

  public Set<Path> getFiles() {
    return files;
  }

  public FileErrors getErrors() {
    return errors;
  }

  public boolean hasErrors() {
    return !errors.isEmpty();
  }

  enum DeepMode implements BiPredicate<Path, BasicFileAttributes> {
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
     * @param file path to be tested (file system dependent)
     * @return <code>true</code> if the file is valid, eg: can be processed.
     */
    protected boolean shouldDescendIntoFile(final String file) {
      return file.endsWith(".ear") || file.endsWith(".war");
    }

    /**
     * Determine if the file should be accepted.
     * <p>
     * The default method delegate to {@link #shouldDescendIntoFile(String)} and accept by default
     * any JAR file not being a sources JAR.
     * 
     * @param file path to be tested (file system dependent)
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
}
