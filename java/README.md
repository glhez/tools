# Building

This project a JDK9 toolchain.

The build is two step for the moment:

    mvn clean install
    ./jtools-jar/linker.bash
    
The second would build a stripped Java with the `jtools-jar` module.

