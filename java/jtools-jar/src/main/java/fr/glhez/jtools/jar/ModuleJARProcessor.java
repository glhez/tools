package fr.glhez.jtools.jar;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.module.ModuleDescriptor;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import fr.glhez.jtools.jar.MavenArtifactsJARProcessor.GAV;

public class ModuleJARProcessor implements JARProcessor {
  private final MavenArtifactsJARProcessor mavenArtifactsProcessor;
  private final Map<JARInformation, ModuleDescriptor> moduleDescriptors;
  private final boolean silent;

  public ModuleJARProcessor(final MavenArtifactsJARProcessor mavenArtifactsProcessor, final boolean silent) {
    this.mavenArtifactsProcessor = mavenArtifactsProcessor;
    this.moduleDescriptors = new LinkedHashMap<>();
    this.silent = silent;
  }

  @Override
  public void init() {
    moduleDescriptors.clear();
  }

  @Override
  public void process(final ProcessorContext context, final JarFile jarFile) {
    try {
      final ZipEntry moduleInfo = jarFile.getEntry("module-info.class");

      if (null != moduleInfo) {
        try (InputStream is = jarFile.getInputStream(moduleInfo);
            BufferedInputStream bis = new BufferedInputStream(is)) {
          final java.lang.module.ModuleDescriptor module = java.lang.module.ModuleDescriptor.read(bis);
          this.moduleDescriptors.put(context.getJARInformation(), module);
        } catch (final IOException | java.lang.SecurityException e) {
          context.addError("Failed to read module-info definition: " + e.getMessage());
        }
        return;
      }

      final Optional<String> automaticModuleName = Optional.ofNullable(jarFile.getManifest())
          .map(Manifest::getMainAttributes).map(attr -> attr.getValue("Automatic-Module-Name"));

      automaticModuleName.ifPresent(name -> {
        this.moduleDescriptors.put(context.getJARInformation(), ModuleDescriptor.newAutomaticModule(name).build());
      });

    } catch (final IOException | java.lang.module.InvalidModuleDescriptorException
        | java.lang.IllegalArgumentException e) {
      // thrown by module info.
      context.addError(e);
    }
  }

  @Override
  public void finish() {
    if (silent) {
      return;
    }
    final String isAutomatic = " [automatic]";
    final String isNotAutomatic = "            ";
    final int oi = moduleDescriptors.values().stream().mapToInt(module -> module.toNameAndVersion().length()).max()
        .orElse(0);
    final int goi = moduleDescriptors.keySet().stream().map(mavenArtifactsProcessor::getGAV).flatMap(Optional::stream)
        .map(Objects::toString).mapToInt(String::length).max().orElse(0);

    System.out.println("---- [Java Module] ----");
    moduleDescriptors.forEach((jarInformation, module) -> {
      final StringBuilder sb = new StringBuilder("  ").append(StringUtils.rightPad(module.toNameAndVersion(), oi));
      if (module.isAutomatic()) {
        sb.append(isAutomatic);
      } else {
        sb.append(isNotAutomatic);
      }
      final Optional<GAV> gav = mavenArtifactsProcessor.getGAV(jarInformation);
      gav.ifPresentOrElse(g -> sb.append(" [").append(StringUtils.rightPad(g.toString(), goi)).append("]"), //
          () -> sb.append("  ").append(StringUtils.rightPad("", goi)).append(" "));
      sb.append(" => ").append(jarInformation);
      System.out.println(sb.toString());
    });
  }

  Optional<ModuleDescriptor> getModuleDescriptor(final JARInformation jarInformation) {
    return Optional.ofNullable(moduleDescriptors.get(jarInformation));
  }

  Optional<GAV> getGAV(final JARInformation jarInformation) {
    return mavenArtifactsProcessor.getGAV(jarInformation);
  }
}
