#! /bin/bash

export c=500;

while [ "$c" -gt 0 ] ; do
  echo '<log link="download/20010322/101030_mcrt_nominal/Fx'$c'.llf" name="20010322/101030_mcrt_nominal/Fx'$c'.llf" size="68417"/>'
  c=$((c-1))
done