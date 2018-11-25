package fr.glhez.jtools.text;

import java.util.Set;
import java.util.Collections;

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
public class TabulizeOptions {
  /**
   * Detect multiline comment.
   */
  public final BiToken multilineComment;

  /**
   * Detect single line comment.
   */
  public final String lineComment;

  /**
   * Detect string using some method (ex: "a").
   */
  public final BiToken string1;

  /**
   * Detect string using some method (ex: 'a').
   */
  public final BiToken string2;

  /**
   * Detect simple valued XML tags.
   * <p>
   * For example:
   *
   * <pre>
   * new HashSet<>(asList("groupId", "artifactId", "version"))
   * </pre>
   *
   * The tag must not have an attribute.
   * <p>
   * Case is sensitive.
   */
  public final Set<String> xmlTags;

  /**
   * Detect keywords.
   * <p>
   * Space are ignored: matching <code>union all</code> is the same as matching
   * <code>union[TAB]all</code>.
   */
  public final Set<String> keywords;

  /**
   * Toggle case insensitivity of keywords.
   */
  public final boolean keywordCaseInsensitive;

  /**
   * Attach single operator such as ',', ';' or ':' to the column on the left of token.
   */
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
   * A list of token to align on right for the first column only.
   */
  public final Set<String> rightAlignFirstColumn;

  /**
   * Toggle case insensitivity of right alignment.
   */
  public final boolean rightAlignFirstColumnCaseInsensitive;

  /**
   * Try to detect <i>number</i>.
   * <p>
   * Number are matched using the same convention than Java.
   */
  public final boolean detectNumber;

  /**
   * Provide a set of token that should be considered as <i>number</i>.
   * <p>
   * This will only work if {@link #detectNumber} is also enabled.
   */
  public final Set<String> additionalNumberToken;

  /**
   * Determine if we should align column containing numbers.
   * <ol>
   * <li>{@link RightAlignNumber#ALL}: always align on right as long as a number is found.</li>
   * <li>{@link RightAlignNumber#NUMBERS_ONLY}: always align on right numbers.</li>
   * <li>{@link RightAlignNumber#NONE}: never align on right.</li>
   * </ol>
   */
  public final RightAlignNumber rightAlignNumber;

  private TabulizeOptions(final Builder builder) {
    this.multilineComment = builder.multilineComment;
    this.lineComment = builder.lineComment;
    this.string1 = builder.string1;
    this.string2 = builder.string2;
    this.xmlTags = builder.xmlTags;
    this.keywords = builder.keywords;
    this.keywordCaseInsensitive = builder.keywordCaseInsensitive;
    this.attachSingleOperator = builder.attachSingleOperator;
    this.detectInitialIndent = builder.detectInitialIndent;
    this.tabSize = builder.tabSize;
    this.rightAlignFirstColumn = builder.rightAlignFirstColumn;
    this.rightAlignFirstColumnCaseInsensitive = builder.rightAlignFirstColumnCaseInsensitive;
    this.detectNumber = builder.detectNumber;
    this.additionalNumberToken = builder.additionalNumberToken;
    this.rightAlignNumber = builder.rightAlignNumber;
  }

  /**
   * Creates builder to build {@link TabulizeOptions}.
   *
   * @return created builder
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Creates a builder to build {@link TabulizeOptions} and initialize it with the given object.
   *
   * @param tabulizeOptions to initialize the builder with
   * @return created builder
   */
  public static Builder builderFrom(final TabulizeOptions tabulizeOptions) {
    return new Builder(tabulizeOptions);
  }

  /**
   * Builder to build {@link TabulizeOptions}.
   */
  public static final class Builder {
    private BiToken multilineComment;
    private String lineComment;
    private BiToken string1;
    private BiToken string2;
    private Set<String> xmlTags = Collections.emptySet();
    private Set<String> keywords = Collections.emptySet();
    private boolean keywordCaseInsensitive;
    private boolean attachSingleOperator;
    private boolean detectInitialIndent;
    private int tabSize;
    private Set<String> rightAlignFirstColumn = Collections.emptySet();
    private boolean rightAlignFirstColumnCaseInsensitive;
    private boolean detectNumber;
    private Set<String> additionalNumberToken = Collections.emptySet();
    private RightAlignNumber rightAlignNumber;

    private Builder() {
    }

    private Builder(final TabulizeOptions tabulizeOptions) {
      this.multilineComment = tabulizeOptions.multilineComment;
      this.lineComment = tabulizeOptions.lineComment;
      this.string1 = tabulizeOptions.string1;
      this.string2 = tabulizeOptions.string2;
      this.xmlTags = tabulizeOptions.xmlTags;
      this.keywords = tabulizeOptions.keywords;
      this.keywordCaseInsensitive = tabulizeOptions.keywordCaseInsensitive;
      this.attachSingleOperator = tabulizeOptions.attachSingleOperator;
      this.detectInitialIndent = tabulizeOptions.detectInitialIndent;
      this.tabSize = tabulizeOptions.tabSize;
      this.rightAlignFirstColumn = tabulizeOptions.rightAlignFirstColumn;
      this.rightAlignFirstColumnCaseInsensitive = tabulizeOptions.rightAlignFirstColumnCaseInsensitive;
      this.detectNumber = tabulizeOptions.detectNumber;
      this.additionalNumberToken = tabulizeOptions.additionalNumberToken;
      this.rightAlignNumber = tabulizeOptions.rightAlignNumber;
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

    public TabulizeOptions build() {
      return new TabulizeOptions(this);
    }
  }

}