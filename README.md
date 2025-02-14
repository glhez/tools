# glhez' tools

This project contains various tools build for my own usage; there are no license for the moment.

# Java tools [![Java CI with Maven](https://github.com/glhez/tools/actions/workflows/build.yml/badge.svg)](https://github.com/glhez/tools/actions/workflows/build.yml)

## Building

Building  this  project  require  a  [Maven  toolchain][1]  containing Java 11 and Java 8. Some project (those
containing  a  "java11") are modular, other are still in Java 8 until the ecosystem is ready too (for example,
that all developers use Java 11++ for Eclipse, jEdit, ...).

The basic usage is to compile the project:

        ./mvnw clean install

Then, it may be imported in Eclipse (except for `jtools-jpms-wrapper` which can't work in Eclipse).

## jtools-jpms-wrapper

Dependencies  such as [Commons CSV][2] are not yet modular: this project fix that by "wrapping" the dependency
and making the necessary fix using a temporary `module-info.java`.

This  work  for  simple  project,  which  is  the  case  of  [Commons  CSV][2]: dependency with a lot of child
dependencies would needs a rewrite of **each** dependencies and is bound to fail.

Note that not doing that simply imply a warning.

# Other tools

If there is no README.md, consider the tool to be dead or non working:

- cpp-tools
  - device-checker: check some stuff on device; it was to ensure that a device was either 0, either 1.
  - difftool: don't remember if it works anymore
- python-tools
  - A small tool to compute checksum with ncurse. Might not be "good python" :)

[1]: https://maven.apache.org/guides/mini/guide-using-toolchains.html
[2]: https://commons.apache.org/proper/commons-csv/

