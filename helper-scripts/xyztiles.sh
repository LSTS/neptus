#!/bin/bash

classpath=`find .. -name *.jar | tr '\n' ':'`

java -cp $classpath -Xms20m -Xmx4096m pt.lsts.neptus.plugins.bathym.BatchXyzExporter $@

