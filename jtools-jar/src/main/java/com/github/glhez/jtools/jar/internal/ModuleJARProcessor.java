package com.github.glhez.jtools.jar.internal;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.lang.module.ModuleDescriptor;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import org.apache.commons.csv.CSVPrinter;

public class ModuleJARProcessor extends ReportFileJARProcessor {
  private final MavenArtifactsJARProcessor mavenArtifactsProcessor;
  private final Map<JARInformation, ModuleDescriptor> moduleDescriptors;

  public ModuleJARProcessor(final Optional<ReportFile> reportFile,
      final MavenArtifactsJARProcessor mavenArtifactsProcessor) {
    super("Java Module", reportFile);
    this.mavenArtifactsProcessor = mavenArtifactsProcessor;
    this.moduleDescriptors = new LinkedHashMap<>();
  }

  @Override
  public void init() {
    moduleDescriptors.clear();
  }

  @Override
  public void process(final ProcessorContext context, final JarFile jarFile) {
    try {
      final var moduleInfo = jarFile.getEntry("module-info.class");

      if (null != moduleInfo) {
        handleModuleDescriptor(context, jarFile, moduleInfo);
        return;
      }

      Optional.ofNullable(jarFile.getManifest()).map(Manifest::getMainAttributes).map(attr -> attr.getValue("Automatic-Module-Name"))
              .ifPresent(name -> this.moduleDescriptors.put(context.getJARInformation(), ModuleDescriptor.newAutomaticModule(name).build()));

    } catch (final IOException | java.lang.module.InvalidModuleDescriptorException
        | java.lang.IllegalArgumentException e) {
      // thrown by module info.
      context.addError(e);
    }
  }

  private void handleModuleDescriptor(final ProcessorContext context, final JarFile jarFile,
      final ZipEntry moduleInfo) {
    try (var is = jarFile.getInputStream(moduleInfo); var bis = new BufferedInputStream(is)) {
      final var module = java.lang.module.ModuleDescriptor.read(bis);
      this.moduleDescriptors.put(context.getJARInformation(), module);
    } catch (final IOException | java.lang.SecurityException e) {
      context.addError("Failed to read module-info definition: " + e.getMessage());
    }
  }

  @Override
  protected void finish(final CSVPrinter printer) throws IOException {
    printer.printRecord("Module and Version", "Automatic", "Maven GAV", "File");
    for (final var entry : moduleDescriptors.entrySet()) {
      final var jarInformation = entry.getKey();
      final var module = entry.getValue();
      final var gav = mavenArtifactsProcessor.getGAVAsString(jarInformation);

      printer.printRecord(module.toNameAndVersion(), module.isAutomatic() ? "Yes" : "No", gav, jarInformation);
    }
  }

  Optional<ModuleDescriptor> getModuleDescriptor(final JARInformation jarInformation) {
    return Optional.ofNullable(moduleDescriptors.get(jarInformation));
  }

  String getModuleDescriptorAsString(final JARInformation jar) {
    return getModuleDescriptor(jar).map(module -> module.toNameAndVersion()
        + (module.isAutomatic() ? " (automatic)" : "")).orElse("");
  }

  String getGAVAsString(final JARInformation jarInformation) {
    return mavenArtifactsProcessor.getGAVAsString(jarInformation);
  }

}
