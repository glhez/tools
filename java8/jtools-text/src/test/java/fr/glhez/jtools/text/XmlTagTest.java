package fr.glhez.jtools.text;

import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.IntStream.Builder;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

public class XmlTagTest {

  private Set<String> of(final String... values) {
    return Stream.of(values).collect(toSet());
  }

  @Test
  public void test_is_empty() {
    assertThat(new XmlTag(emptySet()).isEmpty()).isTrue();
    assertThat(new XmlTag(of("groupId", "artifactId")).isEmpty()).isFalse();
  }

  @Test
  public void test_region_matches() {
    final XmlTag tag = new XmlTag(of("groupId", "artifactId"));

    assertThat(tag.regionMatches(" <groupId>org.apache</groupId>", 0)).isEqualTo(-1);
    assertThat(tag.regionMatches(" <", 1)).isEqualTo(-1);
    // this one is not in isNameStart
    assertThat(tag.regionMatches(" <\u037E", 1)).isEqualTo(-1);
    assertThat(tag.regionMatches(" < ", 1)).isEqualTo(-1);
    assertThat(tag.regionMatches(" <1", 1)).isEqualTo(-1);
    assertThat(tag.regionMatches(" <gro ", 1)).isEqualTo(-1);
    assertThat(tag.regionMatches(" <gro", 1)).isEqualTo(-1);
    assertThat(tag.regionMatches(" <gro>", 1)).isEqualTo(-1);
    assertThat(tag.regionMatches(" <groupId>", 1)).isEqualTo(-1);
    assertThat(tag.regionMatches(" <groupId>AAAA", 1)).isEqualTo(-1);


    final String s1 = "<groupId>AAAA</groupId>";
    final String s2 = "<artifactId>AAAA</artifactId>";
    assertThat(tag.regionMatches(s1 + s2, 0)).isEqualTo(s1.length());
    assertThat(tag.regionMatches(s1 + s2, s1.length())).isEqualTo(s1.length() + s2.length());

  }

  @Test
  public void test_xml_name() {
    final Builder nameStartBuilder = IntStream.builder();

    IntStream.of(':').forEach(nameStartBuilder);
    IntStream.of('A', 'Z').forEach(nameStartBuilder);
    IntStream.of('_').forEach(nameStartBuilder);
    IntStream.of('a', 'z').forEach(nameStartBuilder);
    IntStream.of(0x000000C0, 0x000000D6).forEach(nameStartBuilder);
    IntStream.of(0x000000D8, 0x000000F6).forEach(nameStartBuilder);
    IntStream.of(0x000000F8, 0x000002FF).forEach(nameStartBuilder);
    IntStream.of(0x00000370, 0x0000037D).forEach(nameStartBuilder);
    IntStream.of(0x0000037F, 0x00001FFF).forEach(nameStartBuilder);
    IntStream.of(0x0000200C, 0x0000200D).forEach(nameStartBuilder);
    IntStream.of(0x00002070, 0x0000218F).forEach(nameStartBuilder);
    IntStream.of(0x00002C00, 0x00002FEF).forEach(nameStartBuilder);
    IntStream.of(0x00003001, 0x0000D7FF).forEach(nameStartBuilder);
    IntStream.of(0x0000F900, 0x0000FDCF).forEach(nameStartBuilder);
    IntStream.of(0x0000FDF0, 0x0000FFFD).forEach(nameStartBuilder);
    IntStream.of(0x00010000, 0x000EFFFF).forEach(nameStartBuilder);

    final Builder namePartBuilder = IntStream.builder();
    IntStream.of('-').forEach(namePartBuilder);
    IntStream.of('.').forEach(namePartBuilder);
    IntStream.of('0', '9').forEach(namePartBuilder);
    IntStream.of(0x000000B7).forEach(namePartBuilder);
    IntStream.of(0x00000300, 0x0000036F).forEach(namePartBuilder);
    IntStream.of(0x00000203F, 0x00002040).forEach(namePartBuilder);

    final int[] nameStart = nameStartBuilder.build().toArray();
    final int[] namePart = namePartBuilder.build().toArray();
    final int[] invalidInBoth = IntStream.of(0, ~0, 0x00FFFFFF).toArray();

    Arrays.stream(nameStart).forEach(n -> assertThat(XmlTag.isNameStart(n)).isTrue());
    Arrays.stream(namePart).forEach(n -> assertThat(XmlTag.isNameStart(n)).isFalse());
    Arrays.stream(invalidInBoth).forEach(n -> assertThat(XmlTag.isNameStart(n)).isFalse());

    Arrays.stream(nameStart).forEach(n -> assertThat(XmlTag.isNamePart(n)).isTrue());
    Arrays.stream(namePart).forEach(n -> assertThat(XmlTag.isNamePart(n)).isTrue());
    Arrays.stream(invalidInBoth).forEach(n -> assertThat(XmlTag.isNamePart(n)).isFalse());

  }

}
