package fr.glhez.jtools.text;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

import org.junit.jupiter.api.Test;
import static fr.glhez.jtools.text.Tabulize.*;

public class TabulizeTest {

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
