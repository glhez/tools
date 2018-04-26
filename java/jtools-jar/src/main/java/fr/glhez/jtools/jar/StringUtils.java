package fr.glhez.jtools.jar;

public class StringUtils {

  public static String rightPad(final String source, final int limit) {
    return rightPad(source, limit, " ");
  }

  public static String rightPad(final String source, final int limit, final String padWith) {
    final int n = source.length();
    if (n > limit) {
      return source;
    }
    final StringBuilder sb = new StringBuilder(limit);
    sb.append(source);
    final int k = padWith.length();
    for (int i = n; i < limit; i += k) {
      sb.append(padWith);
    }
    return sb.toString();

  }
}
