#!/bin/bash

classpath=`find .. -name *.jar | tr '\n' ':'`

for f in `find $@ -name *.mjpg | sort -u`; do 
  folder=$(dirname $f);
  echo "Processing $folder.";
  java -cp $classpath -Xms20m -Xmx4096m \
	pt.lsts.neptus.mra.exporters.BatchMraExporter\
	pt.lsts.neptus.plugins.mjpeg.VideoHudExporter $folder;
done

