# Building

Building this project require a [Maven toolchain][1] containing Java 11 and Java 8.

The basic usage is to compile the project:

    ./mvnw clean install

Then, it may be imported in Eclipse (except for `jtools-jpms-wrapper` which can't work in Eclipse).


# jtools-jpms-wrapper

Dependency  such  as  [Commons CSV][2] are not yet modular: this project fix that by "wrapping" the dependency
and making the necessary fix.

This  work  for  simple  project,  which  is  the  case  of  [Commons  CSV][2]: dependency with a lot of child
dependencies would needs a rewrite of **each** dependency and is bound to fail.


[1]: https://maven.apache.org/guides/mini/guide-using-toolchains.html
[2]: https://commons.apache.org/proper/commons-csv/