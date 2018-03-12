package fr.glhez.jtools.jar;

import java.io.IOException;
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
    final Option deepFilterOpt = Option.builder("f").longOpt("filter").argName("pattern").hasArg(true)
      .valueSeparator()
      .desc("Filter embedded JAR/WAR. Path matched by the Pattern will be included."
          + "\n The pattern use java.util.regex.Pattern and can be added several times.")
      .build()
    ;
    final Option includeOpt = Option.builder("i").longOpt("include").argName("pattern").hasArg(true)
        .valueSeparator()
        .desc("Include file from the file system. File matched by the pattern will be added to any analysis."
            + "\n The pattern use java.util.regex.Pattern and can be added several times.")
        .build()
      ;
    final Option excludeOpt = Option.builder("x").longOpt("exclude").argName("pattern").hasArg(true)
      .valueSeparator()
      .desc("Exclude file from the file system. File matched by the pattern will be ignored from any analysis.\n"
          + "The pattern use java.util.regex.Pattern and can be added several times.")
      .build()
    ;
    final Option mavenOpt = Option.builder("m").longOpt("maven").optionalArg(true)
      .desc("For each arg, produce the groupId:artifactId:version if available. The option accept a value, which may be deploy to generate a deploy:deploy-file fragment.")
      .build()
    ;
    final Option permissionsOpt = Option.builder("p").longOpt("permission").optionalArg(true)
        .desc("Check for permissions codebase for JNLP")
        .build()
    ;
    final Option serviceOpt = Option.builder("s").longOpt("service").optionalArg(true)
        .desc("Search a service (SPI) file. A list of service (separated by space or ',') can be passed.")
        .build()
    ;
    final Option classPathOpt = Option.builder("c").longOpt("class-path").optionalArg(true)
        .desc("Check for Class-Path entries.")
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
    options.addOption(permissionsOpt);
    options.addOption(deepFilterOpt);
    options.addOption(excludeOpt);
    options.addOption(includeOpt);
    options.addOption(classPathOpt);

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

    final String[] includes = cmd.getOptionValues(includeOpt.getLongOpt());
    final String[] excludes = cmd.getOptionValues(excludeOpt.getLongOpt());
    final String[] deepInclude = cmd.getOptionValues(deepFilterOpt.getLongOpt());
    try (final JARFileLocator locator = new JARFileLocator(deepMode, includes, excludes, deepInclude)) {
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

      if (cmd.hasOption(permissionsOpt.getLongOpt())) {
        processors.add(new JNLPPermissionsJARProcessor());
      }

      if (cmd.hasOption(classPathOpt.getLongOpt())) {
        processors.add(new ClassPathJARProcessor());
      }

      final ListJARProcessor processor = new ListJARProcessor(processors);
      final MutableProcessorContext ctx = new MutableProcessorContext();
      processor.init();
      for (final JARInformation file : locator.getFiles()) {
        System.out.println("Processing file: " + file.source + " / ");
        ctx.setSource(file);
        try (JarFile jarFile = new JarFile(file.tmpPath.toFile())) {
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
    final Map<JARInformation, List<String>> errors = ctx.getErrors();
    if (!errors.isEmpty()) {
      System.err.println("There was some errors:");
      errors.forEach((information, messages) -> {
        final int n = messages.size();
        if (n == 1) {
          System.err.printf("%s: %s%n", information, messages.get(0));
        } else if (n > 1) {
          System.err.printf("%s:%n", information);
          messages.forEach(message -> System.err.printf("  %s%n", message));
        }
      });
    }
  }

}
