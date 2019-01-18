package fr.glhez.jtools.warextractor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import fr.glhez.jtools.warextractor.internal.ExecutionContext;
import fr.glhez.jtools.warextractor.internal.Extractor;
import fr.glhez.jtools.warextractor.internal.FileDeletor;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(mixinStandardHelpOptions = true, version = "JAR Tool")
public class MainCommand implements Runnable {

  @Parameters(description = "Input directories or archives (if supported by Java zip/jar FileSystemProvider). If there is no --output directory, there must be exactly two parameters, the second being the output directory.")
  private List<Path> input;

  @Option(names = { "-o", "--output" }, description = { "Output directory.",
      "Unlike two arg invocation, add the filename of each input directory/file to the output path." })
  private Path output;

  @Option(names = { "-i", "--include" }, description = {
      "Include file from the file system. File matched by the pattern will be added to any analysis.",
      "The pattern use java.util.regex.Pattern and can be added several times.",
      "By default the pattern match only the name of file; it may match the whole path by prefixing pattern by 'path:'" })
  private List<String> includes;

  @Option(names = { "-x", "--exclude" }, description = {
      "Exclude file from the file system. File matched by the pattern will be ignored from any analysis.",
      "The pattern use java.util.regex.Pattern and can be added several times.",
      "By default the pattern match only the name of file; it may match the whole path by prefixing pattern by 'path:'"

  })
  private List<String> excludes;

  @Option(names = { "-f", "--force" }, description = { "Clean output directory before." })
  private boolean force;

  @Option(names = { "-n", "--dry-run" }, description = { "Do nothing; print operation on stdout." })
  private boolean dryRun;

  @Option(names = { "-N", "--in-place" }, description = {
      "Export WEB-INF/lib/ archives to their own directory rather than WEB-INF/lib/" })
  private boolean inPlace;

  @Option(names = { "--filtering" }, description = {
      "Run filtering on resource (rewrite resource in such a way that differences are lessened)",
      "This option takes a parameter which is split in two part: a filter to apply on a file (order matters!) and a pattern to select files to filter.",
      " - asm: use ASM TextPrinter to reformat class file. Applies on *.class by default.",
      " - cfr: use CFR decompiler to decompile class file. Applies on *.class by default.",
      " - properties: use Java 11 to provide consistent properties (remove comments). Applies on *.properties by default.",
      " - sql: remove ` from mysql queries",
      "properties=name:.*[.](properties): apply filtering on a complex mapping using a regex as seen in includes/excludes."

  })
  private List<String> filtering;

  @Option(names = { "--no-filtering" }, description = {
      "Disable all filtering (by default, class and properties are enabled)", })
  private boolean noFiltering;

  @Option(names = { "-v", "--verbose" }, description = { "Print extra message." })
  private boolean verbose;

  @Option(names = { "-c", "--cache" }, description = { "Cache directory for embedded JARs.",
      "The default value is the system temporary directory." })
  private Path cacheDirectory;

  @Option(names = { "--rename-lib" }, description = { "sed like pattern to rename libraries when --in-place is used.",
      "The expression s/-SNAPSHOT//i will rename the archive a-SNAPSHOT.jar to a.jar.",
      "The '/' is the only possible delimiter (but filename does not contains /).",
      "The expression accept two option: i for case insensitivity and g to replace all." })
  private List<String> renameLib;

  public static void main(final String[] args) {
    picocli.CommandLine.run(new fr.glhez.jtools.warextractor.MainCommand(), System.out, args);
  }

  @Override
  public void run() {
    boolean multipleInput = true;
    if (output == null) {
      if (this.input.size() != 2) {
        throw new IllegalArgumentException("missing output directory; use --help to see usage.");
      }
      this.output = input.get(1);
      this.input = List.of(input.get(0));
      multipleInput = false;
    }

    final var builder = ExecutionContext.builder();
    builder.setDryRun(dryRun);
    builder.setInPlace(inPlace);
    builder.setVerbose(verbose);
    builder.setIncludes(includes);
    builder.setExcludes(excludes);
    builder.setFiltering(filtering);
    builder.setRenameLib(renameLib);
    builder.setCacheDirectory(cacheDirectory);

    final ExecutionContext ctx = builder.build();
    for (final var src : input) {
      final var output = multipleInput ? this.output.resolve(Objects.toString(src.getFileName(), "--XX--"))
          : this.output;

      try (var extractor = Extractor.newExtractor(ctx, src, output)) {
        prepareOutputDirectory(ctx, output);
        extractor.execute();

      } catch (final IOException e) {
        ctx.addError("Could not process input file", src, e);
      }
    }

    for (final var error : ctx) {
      System.err.println("Error: " + error.toString());
      error.printStackTrace(System.err);
    }

  }

  private void prepareOutputDirectory(final ExecutionContext ctx, final Path output) throws IOException {
    if (Files.exists(output)) {
      if (force) {
        ctx.cmd("rm", "-Rv", output);
        ctx.execute(() -> Files.walkFileTree(output, new FileDeletor(ctx)));
      } else {
        throw new IOException("Output [" + output + "] already exists; try --force");
      }
    }
  }

}
