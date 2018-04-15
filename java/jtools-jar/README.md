# Usage

This project contains an executable JAR which will scan other JARs:

## Options

**Selecting files:**
- `-d`, `--directory <directory>`: find all JAR in some directory.
- `-j`, `--jar <file>`: add some explicit jar.
- `-D`, `--deep <filter>`: deep scan of EAR/WAR. The default filter look for JAR in any place.
  If you use `std`, it will use `META-INF/lib`.
- `-f`, `--filter=pattern`: a `java.util.regex.Pattern` to filter entry in WAR/EAR.
- `-i`, `--include=pattern`: a `java.util.regex.Pattern` to include entries from the file system.
- `-x`, `--exclude=pattern`: a `java.util.regex.Pattern` to exclude entries from the file system.

**Actions:**
- `-m`, `--maven[=deploy]`: look for information about a mavenized file; if `deploy` is passed as option
  value, then it will generate a `deploy:deploy-file` command. 
- `-p`, `--permission`: search for JNLP permissions.
- `-s`, `--service=<services>`: search for SPI services (pre Java 9). A list of services, separated by space or comma (`,`)
  may be passed to restrict the list to these services.
- `-c`, `--class-path`: check for `Class-Path` entries in `MANIFEST.MF`.


## Examples

### Find all JAR with a `Class-Path` entry: 

    java -jar target/jtools-jar-1.jar -c -d ~/.m2/repostories





