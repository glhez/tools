/**
 * @author gael.lhez
 */
module fr.glhez.jtools.jar {
  exports fr.glhez.jtools.jar;

  requires info.picocli;


  requires commons.csv;
  requires java.sql; // due to commons.csv
  // requires transitive org.apache.commons.csv;

  opens fr.glhez.jtools.jar to info.picocli;

  uses java.nio.file.spi.FileSystemProvider;

}
