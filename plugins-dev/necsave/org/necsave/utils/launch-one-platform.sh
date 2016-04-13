#!/bin/bash

set -e

if [[ $# -ne 4 ]]; then
  echo "Usage: ./launch-one-platform.sh <bin_folder> <config> <platform_module> <launch_replay>"
  exit 1
fi

bin_folder=$1
cfg=$2
platform_module=$3
launch_replay=$4

if [ ! -d "$bin_folder" ]; then
  echo "Directory does not exist: $bin_folder"
  exit 1
fi

# Set which programs should run.
programs="necrouter mission_planner vehicle_planner nec_perception $platform_module"
if [ "$launch_replay" -eq "1" ]; then
  programs="$programs nec_replay"
fi

echo "LOP: $programs"

# Enable core dumps, to have a stack trace when things go wrong.
ulimit -c unlimited

for program in $programs; do
  echo "Starting $program with config $cfg..."
  $bin_folder/$program $cfg &
done

#valgrind $bin_folder/vehicle_planner  $cfg &

trap '' 2

wait
