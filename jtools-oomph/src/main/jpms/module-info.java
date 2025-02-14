/**
 * @author gael.lhez
 */
module com.github.glhez.jtools.oomph {
  exports com.github.glhez.jtools.oomph;

  requires info.picocli;
  requires transitive java.xml;
  requires java.compiler;

  opens com.github.glhez.jtools.oomph to info.picocli;
}
