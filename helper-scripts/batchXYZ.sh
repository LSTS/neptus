#!/bin/bash
SCRIPTDIR=$(dirname "$0")
java -cp $SCRIPTDIR/../bin/neptus.jar:$SCRIPTDIR/../plugins/*:$SCRIPTDIR/../lib/*:$SCRIPTDIR/../lib/dom4j/* pt.lsts.neptus.plugins.bathym.BatchXyzExporter $1
ROOTDIR=$(dirname $1)
#for f in `find "$ROOTDIR" -name bathymetry.xyz.zip`; do
#	LOGDIR=$(dirname $f);
#	unzip -p $f bathymetry.xyz | tail -n +2 | sort -n -t ',' -k2 -k1 > "$LOGDIR/bathymetry.xyz"; 
#done


