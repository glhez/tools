# Java tools [![Java CI with Maven](https://github.com/glhez/tools/actions/workflows/build.yml/badge.svg)](https://github.com/glhez/tools/actions/workflows/build.yml)

This project contains various tools build for my own usage; there are no license for the moment.

## Building

Building  this  project  require  a  [Maven  toolchain][1]  containing Java 11 and Java 8. Some project (those
containing  a  "java11") are modular, other are still in Java 8 until the ecosystem is ready too (for example,
that all developers use Java 11++ for Eclipse, jEdit, ...).

The basic usage is to compile the project:

```bash
./mvnw verify
```

[1]: https://maven.apache.org/guides/mini/guide-using-toolchains.html

