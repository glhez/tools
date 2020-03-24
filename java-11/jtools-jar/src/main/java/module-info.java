/**
 * @author gael.lhez
 */
@SuppressWarnings("module")
module com.github.glhez.jtools.jar {
  exports com.github.glhez.jtools.jar;

  requires info.picocli;


  requires commons.csv;
  requires java.sql; // due to commons.csv
  // requires transitive org.apache.commons.csv;

  opens com.github.glhez.jtools.jar to info.picocli;

  uses java.nio.file.spi.FileSystemProvider;

}
