package fr.glhez.jtools.jar;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toCollection;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
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

public class SPIServiceJARProcessor implements JARProcessor {
  private static final String SERVICES_DIRECTORY = "META-INF/services/";

  private final boolean all;
  private final Set<String> spiInterfaces;
  private transient final Set<String> spiInterfacesPath;
  private transient final Map<String, Set<AvailableImplementation>> services;

  public SPIServiceJARProcessor(final boolean all, final Set<String> spiInterfaces) {
    Objects.requireNonNull(spiInterfaces, "spiInterfaces");
    this.all = all;
    this.spiInterfaces = all || spiInterfaces.isEmpty() ? Collections.emptySet() : new LinkedHashSet<>(spiInterfaces);
    this.spiInterfacesPath = spiInterfaces.stream().map(path -> SERVICES_DIRECTORY + path)
        .collect(toCollection(LinkedHashSet::new));
    this.services = new LinkedHashMap<>();
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

    entries.forEach(entry -> process(context, jarFile, entry));
  }

  private void process(final ProcessorContext context, final JarFile jarFile, final JarEntry entry) {
    final String service = getServiceForEntry(entry);
    if (null == service) {
      return;
    }
    try (final InputStream is = jarFile.getInputStream(entry)) {
      services.computeIfAbsent(service, (key) -> new LinkedHashSet<>()).add(AvailableImplementation.parse(context.getSource(), is));
    } catch (final IOException e) {
      context.addError("Failed to read services definition [" + service + "]: " + e.getMessage());
    }
  }

  @Override
  public void finish() {
    final Set<String> spiInterfaces = all ? services.keySet() : this.spiInterfaces;

    for (final String spiInterface : spiInterfaces) {
      final Set<AvailableImplementation> implementations = services.get(spiInterface);
      if (null == implementations) {
        System.out.println("Service: " + spiInterface + " -> NO IMPLEMENTATION FOUND");
        continue;
      }
      System.out.println("Service: " + spiInterface + " -> " + implementations.size() + " IMPLEMENTATIONS FOUND");
      for (final AvailableImplementation availableImplementation : implementations) {
        System.out.println("  From: " + availableImplementation.source);
        for (final String impl : availableImplementation.implementations) {
          System.out.println("  Implementation: " + impl);
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
    final Path source;
    final List<String> implementations;

    public AvailableImplementation(final Path source, final List<String> implementation) {
      this.source = requireNonNull(source, "source");
      this.implementations = Collections.unmodifiableList(requireNonNull(implementation, "implementation"));
    }

    static AvailableImplementation parse(final Path source, final InputStream is) throws IOException {
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
        return new AvailableImplementation(source, implementations);
      }
    }

  }
}
