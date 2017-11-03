package fr.glhez.jtools.jar;

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

public class JNLPPermissionsJARProcessor implements JARProcessor {
  private final Map<JNLPPermissions, Map<Path, Set<Optional<Path>>>> result = new TreeMap<>();

  public JNLPPermissionsJARProcessor() {
  }

  @Override
  public void init() {
    result.clear();
  }

  @Override
  public void process(ProcessorContext context, JarFile jarFile) {
    try {
      Manifest manifest = jarFile.getManifest();
      Attributes ma = manifest.getMainAttributes();

      if (null == ma) {
        put(JNLPPermissions.EMPTY, context);
        return;
      }

      JNLPPermissions permissions = new JNLPPermissions(clean(ma.getValue("Permissions")),
          clean(ma.getValue("Codebase")), clean(ma.getValue("Caller-Allowable-Codebase")));

      if (permissions.equals(JNLPPermissions.VALID)) {
        return;
      }

      put(permissions, context);

    } catch (IOException e) {
      put(JNLPPermissions.EMPTY, context);
      context.addError(e);
    }
  }

  @Override
  public void finish() {
    System.out.println("Status: ");
    result.forEach((permissions, files) -> {
      System.out.println("Permissions: " + permissions);
      files.forEach((parent, children) -> {
        System.out.println("  " + parent);
        children.forEach(child -> {
          child.ifPresent(val -> System.out.println("    " + val));
        });
      });
    });
  }

  private void put(JNLPPermissions permissions, ProcessorContext context) {
    result.computeIfAbsent(permissions, p -> new TreeMap<>())
        .computeIfAbsent(context.getJARInformation().source, p -> new LinkedHashSet<>())
        .add(context.getJARInformation().realPath);
  }

  private String clean(String s) {
    return null == s ? "" : s.trim();
  }

  static class JNLPPermissions implements Comparable<JNLPPermissions> {
    static final JNLPPermissions EMPTY = new JNLPPermissions();
    static final JNLPPermissions VALID = new JNLPPermissions("all-permissions", "*", "*");

    public final String permissions;
    public final String codebase;
    public final String callerAllowableCodebase;

    public JNLPPermissions(String permissions, String codebase, String callerAllowableCodebase) {
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
      return "JNLP [Permissions: " + permissions + ", Codebase: " + codebase + ", Caller-Allowable-Codebase: "
          + callerAllowableCodebase + "]";
    }

    @Override
    public int hashCode() {
      return Objects.hash(permissions, codebase, callerAllowableCodebase);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == EMPTY || obj == null || getClass() != obj.getClass())
        return false;
      JNLPPermissions other = (JNLPPermissions) obj;
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

    // @formatter:off
    private static final Comparator<JNLPPermissions> COMPARATOR = Comparator
      .comparing(JNLPPermissions::getPermissions, String::compareTo)
      .thenComparing(JNLPPermissions::getCodebase, String::compareTo)
      .thenComparing(JNLPPermissions::getCallerAllowableCodebase, String::compareTo)
    ;
    // @formatter:on
    @Override
    public int compareTo(JNLPPermissions o) {
      return COMPARATOR.compare(this, o);
    }

  }

}
