package com.github.glhez.tokengrep;

import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.function.Predicate;

import com.github.glhez.fileset.ArchiveMode;
import com.github.glhez.fileset.CollectedFile;
import com.github.glhez.fileset.CollectedFilePredicateBuilder;
import com.github.glhez.fileset.FilesCollector;
import com.github.glhez.tokengrep.internal.Token;
import com.github.glhez.tokengrep.internal.TokenMatcher;
import com.github.glhez.tokengrep.internal.lexer.Lexer;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(mixinStandardHelpOptions = true, version = "1.0", description = { "Token Grep", "A token based grep." })
public class MainCommand implements Runnable {

  @Option(names = { "-f",
      "--file" }, description = "Get patterns from file. One par line, with comment support (#) and empty lines ignored.")
  private Path file;

  @Option(names = { "-a", "--archive" }, description = "Process archive (only ZIP based file) as well.")
  private boolean deepArchive;

  @Parameters(description = "Add a directory/files; directories are searched for supported file type (this depends on available filter).")
  private List<Path> fileset;

  @Option(names = { "-i", "--include", "--includes" }, description = {
      "Include file from the file system. File matched by the pattern will be added to any analysis.",
      "The pattern use java.util.regex.Pattern and can be added several times.",
      "By default the pattern match only the name of file; it may match the whole path by prefixing pattern by 'path:'" })
  private List<String> includes;

  @Option(names = { "-x", "--exclude", "--excludes" }, description = {
      "Exclude file from the file system. File matched by the pattern will be ignored from any analysis.",
      "The pattern use java.util.regex.Pattern and can be added several times.",
      "By default the pattern match only the name of file; it may match the whole path by prefixing pattern by 'path:'"

  })
  private List<String> excludes;

  @Option(names = { "-p",
      "--pattern" }, description = "Look for pattern. The pattern is composed of one or several token type and a java.util.regex.Pattern. Token type may be: \n"
          + "  string: any kind of string\n" + //
          "  identifier: an identifier, including keyword (note: some keyword are context specific, such as var).\n" + //
          "  comment: any kind of comment\n" + //
          "  comment-oneline: a one line comment\n" + //
          "  comment-multiline: a multi line comment\n" + //
          "  comment-javadoc: a javadoc comment\n" + //
          "The pattern will apply on the raw value." + //
          "Example: string,identifier: FOO.+B : match all identifier or string containing FOO.+B")
  private List<String> patterns;

  public static void main(final String[] args) {
    new picocli.CommandLine(new com.github.glhez.tokengrep.MainCommand()).execute(args);
  }

  @Override
  public void run() {
    /*
     * try to read pattern before big operation
     */
    final List<TokenMatcher> matchers = buildMatchers();
    final ArchiveMode mode = this.deepArchive ? ArchiveMode.SCAN_ALWAYS : ArchiveMode.DONT_SCAN;
    final var predicate = buildCollectedFilePredicate(mode);

    try (FilesCollector collector = FilesCollector.newFilesCollector(mode, predicate)) {
      collector.addEntries(fileset);

      grep(collector.getCollectedFiles(), matchers);

      final var errors = collector.getErrors();
      if (!errors.isEmpty()) {
        errors.forEach(System.err::println);
      }
    }

  }

  private void grep(final SortedSet<CollectedFile> files, final List<TokenMatcher> matchers) {
    files.forEach(file -> grep(file, matchers));
  }

  private void grep(final CollectedFile file, final List<TokenMatcher> matchers) {
    Lexer.createLexer(file).forEachRemaining(token -> grep(token, matchers));
  }

  private void grep(final Token token, final List<TokenMatcher> matchers) {
    matchers.stream().flatMap(matcher -> matcher.match(token)).forEach(result -> {
      System.out.println(token.location + ": " + result);
    });
  }

  private Predicate<CollectedFile> buildCollectedFilePredicate(final ArchiveMode mode) {
    final var builder = new CollectedFilePredicateBuilder();

    final var supportedFileFilter = builder.convert("[.]java$").or(mode);
    final var builtinFilter = builder.convert(".*/[.](svn|git)/.*$").negate();
    final var userPredicate = builder.convert(includes, excludes);

    final var predicate = supportedFileFilter.and(builtinFilter).and(userPredicate);
    return predicate;
  }

  private List<TokenMatcher> buildMatchers() {
    final var allPatterns = new ArrayList<String>();
    if (null != patterns) {
      allPatterns.addAll(patterns);
    }
    if (null != file) {
      try {
        // @formatter:off
        Files.readAllLines(file)
             .stream()
             .map(String::strip)
             .filter(s -> s.startsWith("#") || s.isEmpty())
             .forEach(allPatterns::add)
             ;
        // @formatter:on
      } catch (final IOException e) {
        System.err.println("Could not read patterns from " + file + ": " + e.getMessage());
        System.exit(1);
        throw new IllegalStateException(e); // should never happens.
      }
    }
    final List<TokenMatcher> matchers = allPatterns.stream().map(TokenMatcher::parse).collect(toList());
    if (matchers.isEmpty()) {
      throw new IllegalArgumentException("No matcher provided.");
    }
    return matchers;
  }

}
