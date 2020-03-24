module com.github.glhez.tokengrep {
  exports com.github.glhez.tokengrep;

  opens com.github.glhez.tokengrep to info.picocli;

  requires info.picocli;
  requires com.github.glhez.fileset;

}
