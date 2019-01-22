#!/bin/bash

# create new wrapper
declare -r HERE="$(realpath "${0%/*}")"

#  groupId
#  artifactId
#  version
#  package
#  module

declare groupId="$1"
declare artifactId="$2"
declare version="$3"
declare package="$4"
declare module="$5"

if [[ -z "$groupId"    ]]; then echo "missing groupId";    exit 1; fi
if [[ -z "$artifactId" ]]; then echo "missing artifactId"; exit 1; fi
if [[ -z "$version"    ]]; then echo "missing version";    exit 1; fi
if [[ -z "$package"    ]]; then echo "missing package";    exit 1; fi

if [[ -z "$module" ]]; then module="$package";   fi

declare cheat_classname="__Cheat__"
declare cheat_class="${package//.//}/${cheat_classname}.class"
declare cheat_java="${package//.//}/${cheat_classname}.java"

declare template='<?xml version="1.0" encoding="utf-8" ?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
  <modelVersion>4.0.0</modelVersion>

  <parent> <groupId>fr.glhez.jtools.jpms-wrapper</groupId> <artifactId>jpms-parent</artifactId> <version>1</version> </parent>

  <artifactId>%module%</artifactId>
  <version>%version%</version>

  <properties>
    <source.directory>${project.build.directory}/sources</source.directory>
  </properties>

  <build>
    <plugins>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-dependency-plugin</artifactId>
        <executions>
          <execution>
            <goals> <goal>unpack</goal> </goals>
            <configuration>
              <artifactItems>
                <artifactItem>
                  <groupId>%groupId%</groupId>
                  <artifactId>%artifactId%</artifactId>
                  <version>${project.version}</version>
                  <type>jar</type>
                  <overWrite>true</overWrite>
                  <outputDirectory>${project.build.outputDirectory}</outputDirectory>
                </artifactItem>
                <artifactItem>
                  <groupId>%groupId%</groupId>
                  <artifactId>%artifactId%</artifactId>
                  <version>${project.version}</version>
                  <type>jar</type>
                  <classifier>sources</classifier>
                  <overWrite>true</overWrite>
                  <outputDirectory>${source.directory}</outputDirectory>
                </artifactItem>
              </artifactItems>
            </configuration>
          </execution>
        </executions>
      </plugin>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-source-plugin</artifactId>
        <executions>
          <execution>
            <id>attach-sources</id>
            <phase>never</phase>
          </execution>
        </executions>
      </plugin>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-jar-plugin</artifactId>
        <executions>
          <execution>
            <id>makeshift-sources</id>
            <goals>
              <goal>jar</goal>
            </goals>
            <configuration>
              <classesDirectory>${source.directory}</classesDirectory>
              <classifier>sources</classifier>
              <skipIfEmpty>true</skipIfEmpty>
            </configuration>
          </execution>
        </executions>
        <configuration>
          <!-- keep the default manifest. -->
          <archive> <manifestFile>${project.build.outputDirectory}/META-INF/MANIFEST.MF</manifestFile> </archive>
          <excludes>
            <exclude>%cheat-class%</exclude>
            <exclude>%cheat-java%</exclude>
          </excludes>
        </configuration>
      </plugin>
    </plugins>
  </build>

</project>
'

tpl() {
  echo "$1" | sed -E \
    -e "s@%groupId%@${groupId}@g"         \
    -e "s@%artifactId%@${artifactId}@g"   \
    -e "s@%version%@${version}@g"         \
    -e "s@%module%@${module}@g"           \
    -e "s@%package%@${package}@g"         \
    -e "s@%cheat-class%@${cheat_class}@g" \
    -e "s@%cheat-java%@${cheat_java}@g"   \
    -e "s@%cheat-classname%@${cheat_classname}@g"
}


mkdir -pv "$HERE/$module/src/main/java/${cheat_java%/*}"
echo "creating $module/pom.xml"
tpl "$template" > "$HERE/$module/pom.xml"
echo "creating $module/src/main/java/module-info.java"
tpl 'module %module% {
  exports %package%;
}
' > "$HERE/$module/src/main/java/module-info.java"
echo "creating $module/src/main/java/[cheat file]"
tpl 'package %package%;
// required by java
public @interface %cheat-classname% {}
' > "$HERE/$module/src/main/java/${cheat_java}"













