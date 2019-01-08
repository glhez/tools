package fr.glhez.jtools.text;

import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

import org.junit.jupiter.api.Test;
import static fr.glhez.jtools.text.Tabulizer.*;

public class TabulizerTest {

  @Test
  public void test_static_tabulize() {
    assertThatNullPointerException().isThrownBy(() -> new Tabulizer(null)).withMessage("options");
    assertThatNullPointerException().isThrownBy(() -> tabulize(null, new String[0])).withMessage("options");
    assertThatNullPointerException().isThrownBy(() -> tabulize((String[]) null)).withMessage("lines");
    assertThatNullPointerException().isThrownBy(() -> tabulize((String) null)).withMessage("lines[0]");

    assertThat(tabulize()).isEmpty();
    assertThat(tabulize("A")).isEqualTo("A");
    final TabulizerOptions opt1 = TabulizerOptions.builder().setDetectInitialIndent(true)
        .setLineSeparator(LineSeparator.LF).build();
    assertThat(tabulize(opt1, "    A", "B")).isNotNull().isEqualTo("    A\n    B\n");

    assertThat(tabulize(TabulizerOptions.builder().setLineSeparator(LineSeparator.CRLF).build(), "    A", "B"))
        .isNotNull().isEqualTo("A\r\nB\r\n");

  }

  @Test
  public void test_detect_columns() {
    // additionalNumberToken
    // detectNumber
    // keywordCaseInsensitive
    // keywords
    // lineComment
    // multilineComment
    // string1
    // string2
    // xmlTags

    final Tabulizer noOpt = TabulizerOptions.builder().build().toTabulizer();

    {
      final Row row = noOpt.detectColumns("A B C D");
      assertThat(row).isNotNull().contains(new DefaultColumn("A"), new DefaultColumn("B"), new DefaultColumn("C"),
          new DefaultColumn("D"));
      assertThat(row.columnCount()).isEqualTo(4);
    }
    {
      // this should still work (we ignore space before/after)
      final Row row = noOpt.detectColumns("   A B C D    ");
      assertThat(row).isNotNull().contains(new DefaultColumn("A"), new DefaultColumn("B"), new DefaultColumn("C"),
          new DefaultColumn("D"));
      assertThat(row.columnCount()).isEqualTo(4);
    }


    final Tabulizer stringOpt = TabulizerOptions.builder()
        .setString1(BiToken.string("\"", "\\"))
        .setString2(BiToken.string("'", "\\"))
        .build().toTabulizer();
    {
      final String a = "A";
      final String b = "'B A'";
      final String c = "'C $' D'".replace("'", "\"").replace("$", "\\");
      final Row row = stringOpt.detectColumns(a + b + c);
      assertThat(row).isNotNull().contains(new DefaultColumn(a), new DefaultColumn(b), new DefaultColumn(c));
      assertThat(row.columnCount()).isEqualTo(3);
    }

  }

  @Test
  public void test_detect_initial_indent() {
    assertThat(detectInitialIndent("  A", 0)).isEqualTo(2);
    assertThat(detectInitialIndent("A", 0)).isEqualTo(0);
    assertThat(detectInitialIndent("\t\tA", 2)).isEqualTo(4);

    // detect empty lines
    assertThat(detectInitialIndent("      ", 0)).isEqualTo(0);

    assertThat(detectInitialIndent(lines( //
        "      ",  // 0
        "  A",     // 2
        "\tA",     // 0 (tabSize = 0)
        "\t A"     // 1 (tabSize = 0)
    ), 0)).isEqualTo(2);
    assertThat(detectInitialIndent(lines( //
        "  A",     // 2
        "\tA",     // 4 (tabSize = 4)
        "      ",  // 0
        "\t A"     // 5 (tabSize = 4)
    ), 4)).isEqualTo(5);

  }

  @Test
  public void test_trim_lines() {
    assertThat(trimLines(lines( //
        "     AAAA     ", // AAAA
        "   \tBBBB",      // BBBB
        "CCCC \t \t"      // CCCC
    ))).containsExactly("AAAA", "BBBB", "CCCC");
  }

  private static String[] lines(final String... lines) {
    return lines;
  }
}
