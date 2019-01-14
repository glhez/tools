package fr.glhez.jtools.jar.internal;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.apache.commons.csv.CSVPrinter;

public class JNLPPermissionsJARProcessor extends ReportFileJARProcessor {
  private final Map<JNLPPermissions, Map<Path, Set<Optional<Path>>>> result = new TreeMap<>();

  public JNLPPermissionsJARProcessor(final ReportFile reportFile) {
    super("JNLP Permissiosn", reportFile);
  }

  @Override
  public void init() {
    result.clear();
  }

  @Override
  public void process(final ProcessorContext context, final JarFile jarFile) {
    try {
      final Manifest manifest = jarFile.getManifest();
      final Attributes ma = null == manifest ? null : manifest.getMainAttributes();

      if (null == ma) {
        put(JNLPPermissions.EMPTY, context);
        return;
      }

      final JNLPPermissions permissions = new JNLPPermissions(clean(ma.getValue("Permissions")),
          clean(ma.getValue("Codebase")), clean(ma.getValue("Caller-Allowable-Codebase")));

      if (permissions.equals(JNLPPermissions.VALID)) {
        return;
      }

      put(permissions, context);

    } catch (final IOException e) {
      put(JNLPPermissions.EMPTY, context);
      context.addError(e);
    }
  }

  private void put(final JNLPPermissions permissions, final ProcessorContext context) {
    result.computeIfAbsent(permissions, p -> new TreeMap<>())
        .computeIfAbsent(context.getJARInformation().archivePath, p -> new LinkedHashSet<>())
        .add(context.getJARInformation().pathInArchive);
  }

  private String clean(final String s) {
    return null == s ? "" : s.trim();
  }

  @Override
  protected void finish(final CSVPrinter printer) throws IOException {
    printer.printRecord("Permissions", "Codebase", "Caller-Allowable-Codebase", "Parent", "Child");

    for (final var entry : result.entrySet()) {
      final var jnlpPermissions = entry.getKey();

      final var permissions = jnlpPermissions.getReformattedPermissions();
      final var codebase = jnlpPermissions.getReformattedCodebase();
      final var callerAllowableCodebase = jnlpPermissions.getReformattedCallerAllowableCodebase();

      for (final var parentAndChildren : entry.getValue().entrySet()) {
        final var parent = parentAndChildren.getKey();
        for (final var child : parentAndChildren.getValue()) {
          printer.printRecord(permissions, codebase, callerAllowableCodebase, parent,
              child.map(Path::toString).orElse(""));
        }
      }
    }
  }

  static class JNLPPermissions implements Comparable<JNLPPermissions> {
    static final JNLPPermissions EMPTY = new JNLPPermissions();
    static final JNLPPermissions VALID = new JNLPPermissions("all-permissions", "*", "*");

    public final String permissions;
    public final String codebase;
    public final String callerAllowableCodebase;

    public JNLPPermissions(final String permissions, final String codebase, final String callerAllowableCodebase) {
      this.permissions = permissions;
      this.codebase = codebase;
      this.callerAllowableCodebase = callerAllowableCodebase;
    }

    public JNLPPermissions() {
      this("", "", "");
    }

    @Override
    public String toString() {
      if (this == JNLPPermissions.EMPTY) {
        return "JNLP <missing>";
      }
      return "JNLP [Permissions: " + reformatAttribute(permissions) + ", Codebase: " + reformatAttribute(codebase)
          + ", Caller-Allowable-Codebase: " + reformatAttribute(callerAllowableCodebase) + "]";
    }

    private String reformatAttribute(final String s) {
      if (s == null) {
        return "<null>";
      }
      if (s.isEmpty()) {
        return "<empty>";
      }
      return s;
    }

    @Override
    public int hashCode() {
      return Objects.hash(permissions, codebase, callerAllowableCodebase);
    }

    @Override
    public boolean equals(final Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == EMPTY || obj == null || getClass() != obj.getClass()) {
        return false;
      }
      final JNLPPermissions other = (JNLPPermissions) obj;
      return Objects.equals(permissions, other.permissions) && Objects.equals(codebase, other.codebase)
          && Objects.equals(callerAllowableCodebase, other.callerAllowableCodebase);
    }

    public String getPermissions() {
      return permissions;
    }

    public String getCodebase() {
      return codebase;
    }

    public String getCallerAllowableCodebase() {
      return callerAllowableCodebase;
    }

    public String getReformattedPermissions() {
      return this == JNLPPermissions.EMPTY ? "<missing>" : reformatAttribute(permissions);
    }

    public String getReformattedCodebase() {
      return this == JNLPPermissions.EMPTY ? "<missing>" : reformatAttribute(codebase);
    }

    public String getReformattedCallerAllowableCodebase() {
      return this == JNLPPermissions.EMPTY ? "<missing>" : reformatAttribute(callerAllowableCodebase);
    }

    // @formatter:off
    private static final Comparator<JNLPPermissions> COMPARATOR = Comparator
      .comparing(JNLPPermissions::getPermissions, String::compareTo)
      .thenComparing(JNLPPermissions::getCodebase, String::compareTo)
      .thenComparing(JNLPPermissions::getCallerAllowableCodebase, String::compareTo)
    ;
    // @formatter:on
    @Override
    public int compareTo(final JNLPPermissions o) {
      return COMPARATOR.compare(this, o);
    }

  }

}
