package fr.glhez.jtools.text;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Determine options for
 *
 * <ol>
 * <li>parsing lines in search of columns.</li>
 * <li>initial indent.</li>
 * <li>right alignment.</li>
 * </ol>
 *
 * @author gael.lhez
 *
 */
public class TabulizerOptions {
  static final RightAlignNumber DEFAULT_RIGHT_ALIGN_NUMBER = RightAlignNumber.NONE;

  static final int DEFAULT_TABSIZE = 2;

  /**
   * Detect multiline comment.
   */
  @NotImplemented
  @TabulizeColumnFinder("append a comment match 'rule' to the lexer using multiline delimiters")
  public final BiToken multilineComment;

  /**
   * Detect single line comment.
   */
  @NotImplemented
  @TabulizeColumnFinder("append a comment match 'rule' to the lexer using line comment")
  public final String lineComment;

  /**
   * Detect string using some method (ex: "a").
   */
  @NotImplemented
  @TabulizeColumnFinder("append a string match 'rule' to the lexer using string1 delimiters")
  public final BiToken string1;

  /**
   * Detect string using some method (ex: 'a').
   */
  @NotImplemented
  @TabulizeColumnFinder("append a string match 'rule' to the lexer using string2 delimiters")
  public final BiToken string2;

  /**
   * Detect simple valued XML tags.
   * <p>
   * For example:
   *
   * <pre>
   * Set.of("groupId", "artifactId", "version") // Java 11
   * </pre>
   *
   * The tag must not have an attribute.
   * <p>
   * Case is sensitive.
   */
  @NotImplemented
  @TabulizeColumnFinder("toggle XML tags matcher.")
  public final Set<? extends String> xmlTags;

  /**
   * Order XML tags.
   * <p>
   * The column will be re-ordered by tag order. For example:
   *
   * <pre>
   * asList("groupId", "artifactId", "version", "classifier", "type", "scope")
   * </pre>
   *
   */
  @NotImplemented
  @TabulizeOutput
  public final List<? extends String> xmlTagsOrder;

  /**
   * XML Tags will be aligned.
   * <p>
   * The tabulizer will create additional column if tag is found in a row, but not in other.
   */
  @NotImplemented
  @TabulizeOutput
  public final boolean alignXmlTags;

  /**
   * Detect keywords.
   * <p>
   * Space are ignored: matching <code>union all</code> is the same as matching
   * <code>union[TAB]all</code>.
   */
  @NotImplemented
  @TabulizeColumnFinder("add keywords 'rule' to the lexer.")
  public final Set<? extends String> keywords;

  /**
   * Toggle case insensitivity of keywords.
   */
  @NotImplemented
  @TabulizeColumnFinder("toggle (?i) flag to the keywords 'rule' in the lexer.")
  public final boolean keywordCaseInsensitive;

  /**
   * Attach single operator such as ',', ';' or ':' to the column on the left of token.
   */
  @NotImplemented
  public final boolean attachSingleOperator;

  /**
   * Detect initial indent.
   */
  public final boolean detectInitialIndent;

  /**
   * Size of tabs in spaces.
   * <p>
   * Default to 2 if not initialized (0).
   */
  public final int tabSize;

  /**
   * Change the endline delimiter in the output.
   * <p>
   *
   */
  @TabulizeOutput
  public final LineSeparator lineSeparator;

  /**
   * A list of token to align on right for the first column only.
   */
  @NotImplemented
  public final Set<? extends String> rightAlignFirstColumn;

  /**
   * Toggle case insensitivity of right alignment.
   */
  @NotImplemented
  public final boolean rightAlignFirstColumnCaseInsensitive;

  /**
   * Try to detect <i>number</i>.
   * <p>
   * Number are matched using the same convention than Java.
   */
  @NotImplemented
  @TabulizeColumnFinder("add a number 'rule' to the lexer.")
  public final boolean detectNumber;

  /**
   * Provide a set of token that should be considered as <i>number</i>.
   * <p>
   * This will only work if {@link #detectNumber} is also enabled.
   */
  @NotImplemented
  @TabulizeColumnFinder("these tokens will be added to the lexer.")
  public final Set<? extends String> additionalNumberToken;

  /**
   * Determine if we should align column containing numbers.
   * <ol>
   * <li>{@link RightAlignNumber#ALL}: always align on right as long as a number is found.</li>
   * <li>{@link RightAlignNumber#NUMBERS_ONLY}: always align on right numbers.</li>
   * <li>{@link RightAlignNumber#NONE}: never align on right.</li>
   * </ol>
   */
  public final RightAlignNumber rightAlignNumber;

  private TabulizerOptions(final Builder builder) {
    this.multilineComment = builder.multilineComment;
    this.lineComment = builder.lineComment;
    this.string1 = builder.string1;
    this.string2 = builder.string2;
    this.xmlTags = Collections2.copyAsUnmodifiableSet(builder.xmlTags);
    this.xmlTagsOrder = Collections2.copyAsUnmodifiableList(builder.xmlTagsOrder);
    this.alignXmlTags = builder.alignXmlTags;
    this.keywords = Collections2.copyAsUnmodifiableSet(builder.keywords);
    this.keywordCaseInsensitive = builder.keywordCaseInsensitive;
    this.attachSingleOperator = builder.attachSingleOperator;
    this.detectInitialIndent = builder.detectInitialIndent;
    this.tabSize = builder.tabSize <= 0 ? DEFAULT_TABSIZE : builder.tabSize;
    this.lineSeparator = builder.lineSeparator == null ? LineSeparator.LF : builder.lineSeparator;
    this.rightAlignFirstColumn = Collections2.copyAsUnmodifiableSet(builder.rightAlignFirstColumn);
    this.rightAlignFirstColumnCaseInsensitive = builder.rightAlignFirstColumnCaseInsensitive;
    this.detectNumber = builder.detectNumber;
    this.additionalNumberToken = Collections2.copyAsUnmodifiableSet(builder.additionalNumberToken);
    this.rightAlignNumber = builder.rightAlignNumber == null ? DEFAULT_RIGHT_ALIGN_NUMBER : builder.rightAlignNumber;
  }

  /**
   * Create a new {@link Tabulizer}.
   *
   * @return a tabulizer (not null).
   */
  public Tabulizer toTabulizer() {
    return new Tabulizer(this);
  }

  /**
   * Creates builder to build {@link TabulizerOptions}.
   *
   * @return created builder
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builder to build {@link TabulizerOptions}.
   */
  public static final class Builder {
    private BiToken multilineComment;
    private String lineComment;
    private BiToken string1;
    private BiToken string2;
    private Set<? extends String> xmlTags = Collections.emptySet();
    private List<? extends String> xmlTagsOrder = Collections.emptyList();
    private boolean alignXmlTags;
    private Set<? extends String> keywords = Collections.emptySet();
    private boolean keywordCaseInsensitive;
    private boolean attachSingleOperator;
    private boolean detectInitialIndent;
    private int tabSize;
    private LineSeparator lineSeparator;
    private Set<? extends String> rightAlignFirstColumn = Collections.emptySet();
    private boolean rightAlignFirstColumnCaseInsensitive;
    private boolean detectNumber;
    private Set<? extends String> additionalNumberToken = Collections.emptySet();
    private RightAlignNumber rightAlignNumber;

    private Builder() {
    }

    public Builder setMultilineComment(final BiToken multilineComment) {
      this.multilineComment = multilineComment;
      return this;
    }

    public Builder setLineComment(final String lineComment) {
      this.lineComment = lineComment;
      return this;
    }

    public Builder setString1(final BiToken string1) {
      this.string1 = string1;
      return this;
    }

    public Builder setString2(final BiToken string2) {
      this.string2 = string2;
      return this;
    }

    public Builder setXmlTags(final Set<String> xmlTags) {
      this.xmlTags = xmlTags;
      return this;
    }

    public Builder setXmlTagsOrder(final List<String> xmlTagsOrder) {
      this.xmlTagsOrder = xmlTagsOrder;
      return this;
    }

    public Builder setAlignXmlTags(final boolean alignXmlTags) {
      this.alignXmlTags = alignXmlTags;
      return this;
    }

    public Builder setKeywords(final Set<String> keywords) {
      this.keywords = keywords;
      return this;
    }

    public Builder setKeywordCaseInsensitive(final boolean keywordCaseInsensitive) {
      this.keywordCaseInsensitive = keywordCaseInsensitive;
      return this;
    }

    public Builder setAttachSingleOperator(final boolean attachSingleOperator) {
      this.attachSingleOperator = attachSingleOperator;
      return this;
    }

    public Builder setDetectInitialIndent(final boolean detectInitialIndent) {
      this.detectInitialIndent = detectInitialIndent;
      return this;
    }

    public Builder setTabSize(final int tabSize) {
      this.tabSize = tabSize;
      return this;
    }

    public Builder setLineSeparator(final LineSeparator lineSeparator) {
      this.lineSeparator = lineSeparator;
      return this;
    }

    public Builder setRightAlignFirstColumn(final Set<String> rightAlignFirstColumn) {
      this.rightAlignFirstColumn = rightAlignFirstColumn;
      return this;
    }

    public Builder setRightAlignFirstColumnCaseInsensitive(final boolean rightAlignFirstColumnCaseInsensitive) {
      this.rightAlignFirstColumnCaseInsensitive = rightAlignFirstColumnCaseInsensitive;
      return this;
    }

    public Builder setDetectNumber(final boolean detectNumber) {
      this.detectNumber = detectNumber;
      return this;
    }

    public Builder setAdditionalNumberToken(final Set<String> additionalNumberToken) {
      this.additionalNumberToken = additionalNumberToken;
      return this;
    }

    public Builder setRightAlignNumber(final RightAlignNumber rightAlignNumber) {
      this.rightAlignNumber = rightAlignNumber;
      return this;
    }

    public TabulizerOptions build() {
      return new TabulizerOptions(this);
    }

  }

}