/**
 * @author gael.lhez
 */
module fr.glhez.jtools.jar {
    exports fr.glhez.jtools.jar;

    requires info.picocli;
    // requires jdk.compiler;

    requires transitive org.apache.commons.csv;
    requires static transitive org.objectweb.asm;
    requires transitive org.objectweb.asm.util;

    opens fr.glhez.jtools.jar to info.picocli;

    uses java.nio.file.spi.FileSystemProvider;

}
