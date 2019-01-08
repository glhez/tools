package fr.glhez.jtools.jar.internal;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.module.ModuleDescriptor;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import org.apache.commons.csv.CSVPrinter;

import fr.glhez.jtools.jar.internal.MavenArtifactsJARProcessor.GAV;

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
  protected void finish(final CSVPrinter printer) throws IOException {
    printer.printRecord("Module and Version", "Automatic", "Maven GAV", "File");
    for (final var entry : moduleDescriptors.entrySet()) {
      final var jarInformation = entry.getKey();
      final var module = entry.getValue();

      final String gav = mavenArtifactsProcessor.getGAV(jarInformation).map(GAV::toString).orElse("");

      printer.printRecord(module.toNameAndVersion(), module.isAutomatic() ? "Yes" : "No", gav, jarInformation);
    }
  }

  Optional<ModuleDescriptor> getModuleDescriptor(final JARInformation jarInformation) {
    return Optional.ofNullable(moduleDescriptors.get(jarInformation));
  }

  Optional<GAV> getGAV(final JARInformation jarInformation) {
    return mavenArtifactsProcessor.getGAV(jarInformation);
  }
}
