#!/bin/bash

necsave_build=$1

if [ ! -d "$necsave_build" ]; then
  echo "Necsave build directory does not exist: $necsave_build"
  exit 1
fi

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# Function that terminates lingering processes after Ctrl-C is pressed to
# interrupt this script.
function finish {
  for pid in ${started_pids[@]}; do
    (kill -SIGINT $pid 2>&1) >/dev/null
  done
}

necsave_cfgs=${@:2}

nec_platf_idx=0
x_offset=0

echo -n "Starting necsave platforms:"
for plat in ${necsave_cfgs[@]}; do
  platName=`basename $plat`
  echo -n " $platName"
done
echo -e "\n"

for cfg in ${necsave_cfgs[@]}; do
  if [[ $cfg == *"dummy"* ]] || [[ $cfg == *"Dummy"* ]] ; then
    n=`basename $cfg`
    xterm -geometry 100x24+${x_offset}+40 -fg lightgray -bg black -T "Necsave: $n" \
      -e "$DIR/launch-one-platform.sh $necsave_build ${cfg} platform 0 2>&1 | tee ${cfg}.log" &  
  else
    xterm -geometry 100x24+${x_offset}+40 -fg lightgray -bg black -T "Necsave:${cfg}" \
      -e "$DIR/launch-one-platform.sh $necsave_build ${cfg} nec_dune 0 2>&1 | tee ${cfg}.log" &
  fi

  bg_processes[${bg_proc_idx}]=$!
  ((x_offset+=310))
  ((bg_proc_idx++))
  ((nec_platf_idx++))

  sleep 1
done


sleep 3

# Get the child process ids of the xterm windows to know which processes that
# should be killed in the finish function.
pid_idx=0
for ppid in ${bg_processes[@]}; do
    sub_pids=`pgrep -P $ppid`

    for pid in ${sub_pids[@]}; do
        started_pids[${pid_idx}]=${pid}
        ((pid_idx++))
    done
done

trap finish 2

wait

sleep 2

# Remove the socket files that might have been left by processes that didn't finish gracefully.
find . -type s -exec rm -v {} \;

echo "Done"
