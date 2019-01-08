package fr.glhez.jtools.text;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.util.Objects;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

public class BiTokenTest {

  @Test
  public void test() {
    assertThatNullPointerException().isThrownBy(() -> BiToken.of(null, "a")).withMessage("start");
    assertThatNullPointerException().isThrownBy(() -> BiToken.of("a", null)).withMessage("end");
    assertThatNullPointerException().isThrownBy(() -> BiToken.of(null)).withMessage("start");
    assertThatNullPointerException().isThrownBy(() -> BiToken.string(null, "a")).withMessage("start");
    assertThatNullPointerException().isThrownBy(() -> BiToken.string("'", null)).withMessage("escape");

    assertThatIllegalArgumentException().isThrownBy(() -> BiToken.of("")).withMessage("start is empty");
    assertThatIllegalArgumentException().isThrownBy(() -> BiToken.of("a", "")).withMessage("end is empty");
    assertThatIllegalArgumentException().isThrownBy(() -> BiToken.string("'", "")).withMessage("escape is empty");

    assertThat(BiToken.of("a")).matches(expect("a", "a", null));
    assertThat(BiToken.of("a", "b")).matches(expect("a", "b", null));
    assertThat(BiToken.string("a", "b")).matches(expect("a", "a", "b"));
  }

  @Test
  public void test_hash_code_equals() {
    final BiToken a1 = BiToken.of("a");
    final BiToken a2 = BiToken.of("a");
    final BiToken b1 = BiToken.of("b", "k");
    final BiToken b2 = BiToken.of("b", "k");
    final BiToken c1 = BiToken.string("c", "m");
    final BiToken c2 = BiToken.string("c", "m");

    assertThat(a1).isEqualTo(a1).isEqualTo(a2).isNotEqualTo(b1).isNotEqualTo(b1).hasSameHashCodeAs(a2);
    assertThat(b1).isEqualTo(b1).isEqualTo(b2).isNotEqualTo(a1).isNotEqualTo(c1).hasSameHashCodeAs(b2);
    assertThat(c1).isEqualTo(c1).isEqualTo(c2).isNotEqualTo(a1).isNotEqualTo(b1).hasSameHashCodeAs(c2);

    assertThat(a2).isEqualTo(a1).hasSameHashCodeAs(a1);
    assertThat(b2).isEqualTo(b1).hasSameHashCodeAs(b1);
    assertThat(c2).isEqualTo(c1).hasSameHashCodeAs(c1);

    assertThat(a1).isNotEqualTo(null).isNotEqualTo("a");
    assertThat(b1).isNotEqualTo(null).isNotEqualTo("b");
    assertThat(c1).isNotEqualTo(null).isNotEqualTo("c");

    assertThat(BiToken.of("a", "b")).isNotEqualTo(BiToken.of("a", "c"));
    assertThat(BiToken.string("a", "b")).isNotEqualTo(BiToken.string("a", "c"));

  }

  @Test
  public void test_regionMatches() {
    final BiToken c = BiToken.of("/*", "*/");

    assertThat(c.regionMatches("/* THIS IS IT ", 0)).isEqualTo(-1);
    assertThat(c.regionMatches("   THIS IS IT */", 0)).isEqualTo(-1);
    //
    assertThat(c.regionMatches(" /*0123456789*/ ", 1)).isEqualTo(1 + "/*0123456789*/".length());

    final BiToken s = BiToken.string("'-", "@A");
    assertThat(s.regionMatches(" '- THIS IS IT ", 1)).isEqualTo(-1);
    assertThat(s.regionMatches(" '-0123456789'- ", 1)).isEqualTo(1 + "'-0123456789'-".length());
    assertThat(s.regionMatches(" '-0123456789@A'-0123456789'- ", 1)).isEqualTo(1 + "'-0123456789@A'-0123456789'-".length());

  }

  private Predicate<BiToken> expect(final String start, final String end, final String escape) {
    return token -> Objects.equals(start, token.start) && Objects.equals(end, token.end)
        && Objects.equals(escape, token.escape);
  }

}
