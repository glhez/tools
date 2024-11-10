package com.github.glhez.jtools.warextractor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.github.glhez.jtools.warextractor.internal.ExecutionContext;
import com.github.glhez.jtools.warextractor.internal.Extractor;
import com.github.glhez.jtools.warextractor.internal.PathWrapper;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(mixinStandardHelpOptions = true, version = "JAR Tool")
public class MainCommand implements Runnable {
  /** Logger */
  private static final org.apache.logging.log4j.Logger logger = org.apache.logging.log4j.LogManager.getLogger(MainCommand.class);

  @Parameters(description = "Input directories or archives (if supported by Java zip/jar FileSystemProvider). If there is no --output directory, there must be exactly two parameters, the second being the output directory.")
  private List<Path> input;

  @Option(names = { "-o", "--output" },
          description = { "Output directory.",
              "Unlike two arg invocation, add the filename of each input directory/file to the output path." })
  private Path output;

  @Option(names = { "-i", "--include" },
          description = {
              "Include file from the file system. File matched by the pattern will be added to any analysis.",
              "The pattern use java.util.regex.Pattern and can be added several times.",
              "The pattern may be prefix by a selector:", //
              "- name: filter will apply on filename (default)", //
              "- path: filter will apply on full path", //
              "- ext: filter will apply on extension", //
              "- noext: filter will apply on filename without extension"//
          })
  private List<String> includes;

  @Option(names = { "-x", "--exclude" },
          description = {
              "Exclude file from the file system. File matched by the pattern will be ignored from any analysis.",
              "The pattern use java.util.regex.Pattern and can be added several times.", "See --include for pattern format."

          })
  private List<String> excludes;

  @Option(names = { "-f", "--force" }, description = { "Clean output directory before." })
  private boolean force;

  @Option(names = { "-n", "--dry-run" },
          description = { //
              "Do not copy or filter anything.", //
              "This will not:", //
              "- clean the output directory if it exists and --force is used", //
              "- copy any files", //
              "- filter any files" })
  private boolean dryRun;

  @Option(names = { "-N", "--in-place" },
          description = {
              "Export WEB-INF/lib/ archives to their own directory rather than WEB-INF/lib/" })
  private boolean inPlace;

  @Option(names = { "--filtering" },
          description = {
              "Rewrite copied resource in such a way that differences are mitigated", //
              "The filtering is run on a copied files, in the same order than defined: some filter will only work with binary files, while other expect some charset.",
              "This option takes a parameter which is split in two part: a filter to apply on a file (order matters!) and a pattern to select files to filter.",
              " - asm: use ASM TextPrinter to reformat class file. Applies on *.class by default.",
              " - cfr: use CFR decompiler to decompile class file. Applies on *.class by default.",
              " - properties: use Java 11 to provide consistent properties (remove comments). Applies on *.properties by default.",
              " - sql: remove ` from mysql queries",
              "properties=name:.*[.](properties): apply filtering on a complex mapping using a regex as seen in includes/excludes.",
              "asm and cfr read binary content and produce UTF-8 content.",
              "properties produce UTF-8 content but read ISO-8859-1 content unless it was already filtered, in which case it expect UTF-8.",
              "sql read and produce utf-8 content.", "NOTE: filter are applied in memory."

          })
  private List<String> filtering;

  @Option(names = { "--no-filtering" },
          description = {
              "Disable all filtering (by default, class and properties are enabled)", })
  private boolean noFiltering;

  @Option(names = { "-c", "--cache" },
          description = { "Cache directory for embedded JARs.",
              "The default value is the system temporary directory." })
  private Path cacheDirectory;

  @Option(names = { "--rename-lib" },
          description = { "sed like pattern to rename libraries when --in-place is used.",
              "The expression s/-SNAPSHOT//i will rename the archive a-SNAPSHOT.jar to a.jar.",
              "The '/' is the only possible delimiter (but filename does not contains /).",
              "The expression accept two option: i for case insensitivity and g to replace all." })
  private List<String> renameLib;

  public static void main(final String[] args) {
    new picocli.CommandLine(new com.github.glhez.jtools.warextractor.MainCommand()).execute(args);
  }

  @Override
  public void run() {
    boolean multipleInput = true;
    if (this.input == null || this.input.isEmpty()) {
      throw new IllegalArgumentException("missing output directory; use --help to see usage.");
    }
    if (this.output == null) {
      if (this.input.size() != 2) {
        throw new IllegalArgumentException("missing output directory; use --help to see usage.");
      }
      this.output = this.input.get(1);
      this.input = List.of(this.input.get(0));
      multipleInput = false;
    }

    final var builder = ExecutionContext.builder();
    builder.setDryRun(this.dryRun);
    builder.setInPlace(this.inPlace);
    builder.setIncludes(this.includes);
    builder.setExcludes(this.excludes);
    builder.setFiltering(this.filtering);
    builder.setRenameLib(this.renameLib);
    builder.setCacheDirectory(this.cacheDirectory);

    final ExecutionContext ctx = builder.build();

    for (final var src : this.input) {
      final var output = multipleInput ? this.output.resolve(PathWrapper.getFileNameNoExtension(src)) : this.output;

      try (var extractor = Extractor.newExtractor(ctx, src, output)) {
        prepareOutputDirectory(ctx, output);
        extractor.execute();

      } catch (final IOException e) {
        logger.error("Could not process input file: {}", src, e);
      }
    }

  }

  private void prepareOutputDirectory(final ExecutionContext ctx, final Path output) throws IOException {
    if (Files.exists(output)) {
      if (this.force) {
        ctx.getFilesProxy().recursiveDelete(output);
      } else {
        throw new IOException("Output [" + output + "] already exists; try --force");
      }
    }
    ctx.getFilesProxy().createDirectories(output);
  }

}
