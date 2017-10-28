package fr.glhez.jtools.jar;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class Main {

  public static void main(final String[] args) throws ParseException {
    // @formatter:off
    final Option directoryOpt = Option.builder("d").longOpt("directory").argName("directory")
      .hasArg()
      .desc("Find all jar in this directory")
      .build()
    ;
    final Option jarOpt = Option.builder("j").longOpt("jar").argName("jar")
      .hasArg()
      .desc("Add a specific jar")
      .build()
    ;
    final Option deepOpt = Option.builder("D").longOpt("deep").argName("filter").hasArg(true)
      .desc("Look for JAR in EAR/WAR files. The filter accepts value all (don't care about hierarchy) or the default 'std' (META-INF/lib/ only)")
      .build()
    ;
    final Option mavenOpt = Option.builder("m").longOpt("maven").optionalArg(true)
      .desc("For each arg, produce the groupId:artifactId:version if available. The option accept a value, which may be deploy to generate a deploy:deploy-file fragment.")
      .build()
    ;
    final Option serviceOpt = Option.builder("s").longOpt("service").optionalArg(true)
        .desc("Search a service (SPI) file. A list of service (separated by space or ',') can be passed.")
        .build()
      ;
    final Option helpOpt = Option.builder("h").longOpt("help")
      .desc("Display this help")
      .build()
    ;
    // @formatter:on

    final Options options = new Options();
    options.addOption(directoryOpt);
    options.addOption(jarOpt);
    options.addOption(serviceOpt);
    options.addOption(mavenOpt);
    options.addOption(helpOpt);
    options.addOption(deepOpt);

    final CommandLineParser parser = new DefaultParser();
    final CommandLine cmd = parser.parse(options, args);

    if (cmd.hasOption(helpOpt.getLongOpt())) {
      final HelpFormatter formatter = new HelpFormatter();
      formatter.printHelp("jartools", options);
      return;
    }

    final JARFileLocator.DeepMode deepMode = EnumParameter
        .parameter(JARFileLocator.DeepMode::valueOf, JARFileLocator.DeepMode::values, JARFileLocator.DeepMode.DISABLED)
        .parse(deepOpt, cmd.getOptionValue(deepOpt.getLongOpt()));

    try (final JARFileLocator locator = new JARFileLocator(deepMode)) {
      locator.addFiles(cmd.getOptionValues(jarOpt.getLongOpt()));
      locator.addDirectories(cmd.getOptionValues(directoryOpt.getLongOpt()));
      if (locator.hasErrors()) {
        System.err.println("Some file or directories could not be fetched:");
        locator.getErrors().forEach(System.err::println);
      }

      final List<JARProcessor> processors = new ArrayList<>();

      if (cmd.hasOption(mavenOpt.getLongOpt())) {
        final MavenArtifactsJARProcessor.OptionKind kind = EnumParameter
            .parameter(MavenArtifactsJARProcessor.OptionKind::valueOf, MavenArtifactsJARProcessor.OptionKind::values,
                MavenArtifactsJARProcessor.OptionKind.LIST)
            .parse(mavenOpt, cmd.getOptionValue(mavenOpt.getLongOpt()));
        processors.add(new MavenArtifactsJARProcessor(kind));
      }

      if (cmd.hasOption(serviceOpt.getLongOpt())) {
        final String userValue = cmd.getOptionValue(serviceOpt.getLongOpt());
        final boolean all = null == userValue;
        Set<String> spiInterfaces;
        if (null != userValue) {
          spiInterfaces = Stream.of(userValue.split(",+|\\s+")).filter(String::isEmpty).collect(Collectors.toSet());
        } else {
          spiInterfaces = Collections.emptySet();
        }
        processors.add(new SPIServiceJARProcessor(all, spiInterfaces));
      }

      final ListJARProcessor processor = new ListJARProcessor(processors);
      final MutableProcessorContext ctx = new MutableProcessorContext();
      processor.init();
      for (final Path file : locator.getFiles()) {
        ctx.setSource(file);
        try (JarFile jarFile = new JarFile(file.toFile())) {
          processor.process(ctx, jarFile);
        } catch (final IOException e) {
          ctx.addError(e);
        }
      }
      dumpErrors(ctx);
      processor.finish();
    }
  }

  private static void dumpErrors(final MutableProcessorContext ctx) {
    final Map<Path, List<String>> errors = ctx.getErrors();
    if (!errors.isEmpty()) {
      System.err.println("There was some errors:");
      errors.forEach((path, messages) -> {
        final int n = messages.size();
        if (n == 1) {
          System.err.printf("%s: %s%n", path, messages.get(0));
        } else if (n > 1) {
          System.err.printf("%s:%n", path);
          messages.forEach(message -> System.err.printf("  %s%n", message));
        }
      });
    }
  }
}
