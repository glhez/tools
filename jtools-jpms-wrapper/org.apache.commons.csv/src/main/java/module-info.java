/**
 * This makeshift module allows our tool to access Commons CSV as a Java Module without being
 * burdened by subtle warning.
 * <p>
 * This can't work without cheating Java a little.
 */
module org.apache.commons.csv {
  exports org.apache.commons.csv;

  requires java.sql;
}
