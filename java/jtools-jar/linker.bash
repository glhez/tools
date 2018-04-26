#!/bin/bash

declare jmods=$JAVA_HOME/jmods

if [[ ! -d "$jmods" ]]; then
  echo "error: no jmods (${jmods})"
  exit 1
fi

if [[ "${OS,,}" == *windows* ]]; then
  declare S=';'
  declare jmods="$(cygpath -w "${jmods}")"
else
  declare S=':'
fi

declare root=jtools-jar
if [[ -d "${root}" ]]; then
  root=.
elif [[ -d "../${root}" ]]; then
  root=..
else
  echo 'error: not in valid root, try executing in the same directory than this file.'
  exit 1
fi

declare output="${root}/jtools-jar/target/jlink"

rm -Rfv "$output"
jlink --module-path "${jmods}${S}${root}/jtools-jar/target/jtools-jar-1.jar${S}${root}/jpms-enabler/org.apache.commons/commons-cli/target/org.apache.commons.commons-cli-1.4-java9.jar" \
      --compress 2 \
      --add-modules java.base,fr.glhez.jtools.jar,org.apache.commons.cli \
      --launcher jtools-jar=fr.glhez.jtools.jar/fr.glhez.jtools.jar.Main \
      --output "${output}" \
      --verbose