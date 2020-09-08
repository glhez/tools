package com.github.glhez.jtools.text;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.github.glhez.jtools.text.BiToken;
import com.github.glhez.jtools.text.LineSeparator;
import com.github.glhez.jtools.text.RightAlignNumber;
import com.github.glhez.jtools.text.TabulizerOptions;
import com.github.glhez.jtools.text.TabulizerOptions.Builder;

public class TabulizerOptionsTest {

  @Test
  public void test_default_values() {
    final TabulizerOptions b = TabulizerOptions.builder().build();

    assertThat(b.multilineComment).isNull();
    assertThat(b.lineComment).isNull();
    assertThat(b.string1).isNull();
    assertThat(b.string2).isNull();
    assertThat(b.xmlTags).isEmpty();
    assertThat(b.xmlTagsOrder).isEmpty();
    assertThat(b.alignXmlTags).isFalse();
    assertThat(b.keywords).isEmpty();
    assertThat(b.keywordCaseInsensitive).isFalse();
    assertThat(b.attachSingleOperator).isFalse();
    assertThat(b.detectInitialIndent).isFalse();
    assertThat(b.tabSize).isEqualTo(TabulizerOptions.DEFAULT_TABSIZE);
    assertThat(b.lineSeparator).isEqualTo(LineSeparator.LF);
    assertThat(b.rightAlignFirstColumn).isEmpty();
    assertThat(b.rightAlignFirstColumnCaseInsensitive).isFalse();
    assertThat(b.detectNumber).isFalse();
    assertThat(b.additionalNumberToken).isEmpty();
    assertThat(b.rightAlignNumber).isEqualTo(TabulizerOptions.DEFAULT_RIGHT_ALIGN_NUMBER);

    assertUnmodifiable(b.xmlTags);
    assertUnmodifiable(b.xmlTagsOrder);
    assertUnmodifiable(b.keywords);
    assertUnmodifiable(b.rightAlignFirstColumn);
    assertUnmodifiable(b.additionalNumberToken);
  }

  @Test
  public void test_customized() {
    final BiToken multilineComment = BiToken.of("/*", "*/");

    final String lineComment = "//";
    final BiToken string1 = BiToken.string("'", "\\");
    final BiToken string2 = BiToken.string("\"", "\\");
    final Set<String> xmlTags = Collections.singleton("groupId");
    final List<String> xmlTagsOrder = asList("groupId", "artifactId");
    final boolean keywordCaseInsensitive = true;
    final boolean alignXmlTags = true;
    final Set<String> keywords = singleton("as if");
    final boolean attachSingleOperator = true;
    final boolean detectInitialIndent = true;
    final int tabSize = 4;
    final LineSeparator lineSeparator = LineSeparator.CRLF;
    final Set<String> rightAlignFirstColumn = singleton(",");
    final boolean rightAlignFirstColumnCaseInsensitive = true;
    final boolean detectNumber = true;
    final Set<String> additionalNumberToken = Collections.singleton("A");
    final RightAlignNumber rightAlignNumber = RightAlignNumber.NUMBERS_ONLY;

    final Builder builder = TabulizerOptions.builder();
    builder.setMultilineComment(multilineComment);
    builder.setLineComment(lineComment);
    builder.setString1(string1);
    builder.setString2(string2);
    builder.setXmlTags(xmlTags);
    builder.setXmlTagsOrder(xmlTagsOrder);
    builder.setAlignXmlTags(alignXmlTags);
    builder.setKeywords(keywords);
    builder.setKeywordCaseInsensitive(keywordCaseInsensitive);
    builder.setAttachSingleOperator(attachSingleOperator);
    builder.setDetectInitialIndent(detectInitialIndent);
    builder.setTabSize(tabSize);
    builder.setLineSeparator(lineSeparator);
    builder.setRightAlignFirstColumn(rightAlignFirstColumn);
    builder.setRightAlignFirstColumnCaseInsensitive(rightAlignFirstColumnCaseInsensitive);
    builder.setDetectNumber(detectNumber);
    builder.setAdditionalNumberToken(additionalNumberToken);
    builder.setRightAlignNumber(rightAlignNumber);

    final TabulizerOptions b = builder.build();

    assertThat(b.multilineComment).isSameAs(multilineComment);
    assertThat(b.lineComment).isSameAs(lineComment);
    assertThat(b.string1).isSameAs(string1);
    assertThat(b.string2).isSameAs(string2);
    assertThat(b.xmlTags).isNotSameAs(xmlTags).isEqualTo(xmlTags);
    assertThat(b.xmlTagsOrder).isNotSameAs(xmlTagsOrder).isEqualTo(xmlTagsOrder);
    assertThat(b.alignXmlTags).isEqualTo(alignXmlTags);
    assertThat(b.keywords).isNotSameAs(keywords).isEqualTo(keywords);
    assertThat(b.keywordCaseInsensitive).isEqualTo(keywordCaseInsensitive);
    assertThat(b.attachSingleOperator).isEqualTo(attachSingleOperator);
    assertThat(b.detectInitialIndent).isEqualTo(detectInitialIndent);
    assertThat(b.tabSize).isEqualTo(tabSize);
    assertThat(b.lineSeparator).isEqualTo(lineSeparator);
    assertThat(b.rightAlignFirstColumn).isNotSameAs(rightAlignFirstColumn).isEqualTo(rightAlignFirstColumn);
    assertThat(b.rightAlignFirstColumnCaseInsensitive).isEqualTo(rightAlignFirstColumnCaseInsensitive);
    assertThat(b.detectNumber).isEqualTo(detectNumber);
    assertThat(b.additionalNumberToken).isNotSameAs(additionalNumberToken).isEqualTo(additionalNumberToken);
    assertThat(b.rightAlignNumber).isEqualTo(rightAlignNumber);

    assertUnmodifiable(b.xmlTags);
    assertUnmodifiable(b.xmlTagsOrder);
    assertUnmodifiable(b.keywords);
    assertUnmodifiable(b.rightAlignFirstColumn);
    assertUnmodifiable(b.additionalNumberToken);
  }

  static <E> void assertUnmodifiable(final Collection<? extends E> collection) {
    assertThat(collection.getClass().getName())
        .describedAs("collection (%s) should be unmodifiable", collection.getClass()).matches(
            s -> s.startsWith("java.util.Collections$Unmodifiable") || s.startsWith("java.util.Collections$Empty"));
  }
}
