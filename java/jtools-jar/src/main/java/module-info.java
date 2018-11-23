/**
 * @author gael.lhez
 */
module fr.glhez.jtools.jar {
  exports fr.glhez.jtools.jar;

  requires info.picocli;
  requires jdk.compiler;

  opens fr.glhez.jtools.jar to info.picocli;

}
