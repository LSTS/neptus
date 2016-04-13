#!/bin/bash

dune_build=$1

if [ ! -d "$dune_build" ]; then
  echo "DUNE build directory does not exist: $dune_build"
  exit 1
fi

# Function that terminates lingering processes after Ctrl-C is pressed to
# interrupt this script.
function finish {
  for pid in ${started_pids[@]}; do
    (kill -SIGINT $pid 2>&1) >/dev/null
  done
}

echo "Starting dune simulators: ${@:2}"

# Start the dune simulators each in a separate window.
# Note: If you want to see output during shutdown of the dune program, you must
#       type Ctrl-C in the created windows to quit.
#       Otherwise, if you type Ctrl-C in the window that has started this
#       script, all created X-terminal windows will be instantly closed.
for dune_platform in ${@:2}; do
  xterm -geometry 100x24+${x_offset}+0 -fg lightgray -bg black -T "DuneSim:${dune_platform}" \
      -e "$dune_build/dune -c ${dune_platform} -p Simulation"&
  bg_processes[${bg_proc_idx}]=$!
  ((x_offset+=40))
  ((bg_proc_idx++))
  sleep 1
done

sleep 4


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

echo "Done"
