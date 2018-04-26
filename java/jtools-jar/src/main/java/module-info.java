/**
 * @author gael.lhez
 */
module fr.glhez.jtools.jar {
  exports fr.glhez.jtools.jar;

  requires transitive org.apache.commons.cli;
  requires info.picocli;
  requires jdk.compiler;
  
  opens fr.glhez.jtools.jar.picocli to info.picocli; 
  
}