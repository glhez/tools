package fr.glhez.jtools.jar.internal;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toCollection;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Provides;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

import org.apache.commons.csv.CSVPrinter;

public class SPIServiceJARProcessor extends ReportFileJARProcessor {
  private static final String SERVICES_DIRECTORY = "META-INF/services/";

  private final ModuleJARProcessor moduleJARProcessor;
  private final boolean all;
  private final Set<String> spiInterfaces;
  private transient final Set<String> spiInterfacesPath;
  private transient final Map<String, Set<AvailableImplementation>> services;
  private final boolean moduleOnly;

  public SPIServiceJARProcessor(final ReportFile reportFile, final ModuleJARProcessor moduleJARProcessor,
      final boolean all, final Set<String> spiInterfaces, final boolean moduleOnly) {
    super("SPI Service", reportFile);
    Objects.requireNonNull(spiInterfaces, "spiInterfaces");
    this.moduleJARProcessor = requireNonNull(moduleJARProcessor, "moduleJARProcessor");
    this.all = all;
    this.spiInterfaces = all || spiInterfaces.isEmpty() ? Collections.emptySet() : new LinkedHashSet<>(spiInterfaces);
    this.spiInterfacesPath = spiInterfaces.stream().map(path -> SERVICES_DIRECTORY + path)
        .collect(toCollection(LinkedHashSet::new));
    this.services = new LinkedHashMap<>();
    this.moduleOnly = moduleOnly;
  }

  @Override
  public void init() {
    services.clear();
  }

  @Override
  public void process(final ProcessorContext context, final JarFile jarFile) {
    Stream<JarEntry> entries;
    if (all) {
      entries = jarFile.stream().filter(SPIServiceJARProcessor::isCandidateForServices);
    } else {
      entries = spiInterfacesPath.stream().map(jarFile::getJarEntry).filter(Objects::nonNull);
    }

    if (!moduleOnly) {
      entries.forEach(entry -> process(context, jarFile, entry));
    }
    moduleJARProcessor.getModuleDescriptor(context.getJARInformation())
        .ifPresent(descriptor -> process(context, descriptor));

  }

  private Set<AvailableImplementation> providersFor(final String service) {
    return services.computeIfAbsent(service, (key) -> new LinkedHashSet<>());
  }

  private void process(final ProcessorContext context, final JarFile jarFile, final JarEntry entry) {
    final String service = getServiceForEntry(entry);
    if (null == service) {
      return;
    }
    try (final InputStream is = jarFile.getInputStream(entry)) {
      providersFor(service).add(AvailableImplementation.parse(context.getJARInformation(), is));
    } catch (final IOException | java.lang.SecurityException e) {
      context.addError("Failed to read services definition [" + service + "]: " + e.getMessage());
    }
  }

  private boolean isRequestedServices(final Provides provides) {
    return all || spiInterfaces.contains(provides.service());
  }

  private void process(final ProcessorContext context, final ModuleDescriptor descriptor) {
    descriptor.provides().stream().filter(this::isRequestedServices).forEach(provides -> {
      providersFor(provides.service())
          .add(new AvailableImplementation(context.getJARInformation(), provides.providers(), true));
    });
  }

  @Override
  protected void finish(final CSVPrinter printer) throws IOException {
    final Set<String> spiInterfaces = all ? services.keySet() : this.spiInterfaces;

    printer.printRecord("Interface", "Implementation count (Fileset)", "Implementation count (JAR)", "Implementation",
        "Maven", "Java Module", "JAR");

    for (final String spiInterface : spiInterfaces) {
      final Set<AvailableImplementation> implementations = services.get(spiInterface);
      if (null == implementations) {
        printer.printRecord(spiInterface, "0", "0", "(none)", "", "", "");
        continue;
      }

      final int implementationFound = implementations.stream().mapToInt(ai -> ai.implementations.size()).sum();
      for (final AvailableImplementation availableImplementation : implementations) {
        final String gav = moduleJARProcessor.getGAVAsString(availableImplementation.jarInformation);
        final String desc = moduleJARProcessor.getModuleDescriptorAsString(availableImplementation.jarInformation);
        final var jarImplementationFound = availableImplementation.implementations.size();
        for (final String impl : availableImplementation.implementations) {
          printer.printRecord(spiInterface, implementationFound, jarImplementationFound, impl, gav, desc,
              availableImplementation.jarInformation);
        }
      }
    }
  }

  private static boolean isCandidateForServices(final JarEntry jarEntry) {
    return isCandidateForServices(jarEntry.getName());
  }

  private static boolean isCandidateForServices(final String jarEntryName) {
    return !SERVICES_DIRECTORY.equals(jarEntryName) && jarEntryName.startsWith(SERVICES_DIRECTORY);
  }

  private String getServiceForEntry(final JarEntry entry) {
    final String service = entry.getName().substring(SERVICES_DIRECTORY.length());
    if (service.contains("/")) {
      return null;
    }
    return service;
  }

  static final class AvailableImplementation {
    final JARInformation jarInformation;
    final List<String> implementations;
    final boolean fromModuleInfo;

    public AvailableImplementation(final JARInformation jarInformation, final List<String> implementation,
        final boolean fromModuleInfo) {
      this.jarInformation = requireNonNull(jarInformation, "jarInformation");
      this.implementations = Collections.unmodifiableList(requireNonNull(implementation, "implementation"));
      this.fromModuleInfo = fromModuleInfo;
    }

    static AvailableImplementation parse(final JARInformation source, final InputStream is) throws IOException {
      final List<String> implementations = new ArrayList<>();
      try (final BufferedReader reader = new BufferedReader(new InputStreamReader(is, "utf-8"))) {
        for (String line = null; null != (line = reader.readLine());) {
          final int ci = line.indexOf('#');
          if (ci >= 0) {
            line = line.substring(0, ci);
          }
          line = line.trim();
          if (!line.isEmpty()) {
            implementations.add(line);
          }
        }
        return new AvailableImplementation(source, implementations, false);
      }
    }

  }
}
