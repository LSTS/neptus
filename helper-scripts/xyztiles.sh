#!/bin/bash

classpath=`find .. -name *.jar | tr '\n' ':'`

for f in `find $@ -name *.83P`; do 
  folder=$(dirname $f); 
  echo "processing $folder.";
  java -cp $classpath -Xms20m -Xmx4096m pt.lsts.neptus.plugins.bathym.BatchXyzExporter $folder;
done


#java -cp $classpath -Xms20m -Xmx4096m pt.lsts.neptus.plugins.bathym.BatchXyzExporter $@

