/**
 * This makeshift module allows our tool to access Picocli as a Java Module without being
 * burdened by subtle warning.
 * <p>
 * This can't work without cheating Java a little.
 */
module info.picocli {
  exports picocli;
}
