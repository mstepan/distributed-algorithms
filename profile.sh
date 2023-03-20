#!/usr/bin/env bash

#
# async-profiler usage details be found here https://github.com/jvm-profiling-tools/async-profiler
#

profile_graph=profile-graph.html

rm -rf $profile_graph

#
# possible options described here: https://github.com/jvm-profiling-tools/async-profiler/blob/v2.8.3/src/arguments.cpp#L52
#
async_profiler_options=-agentpath:./async-profiler-2.9-macos/build/libasyncProfiler.so=start,file=$profile_graph,flamegraph,event=cpu
java_options="-XX:+UnlockDiagnosticVMOptions -XX:+DebugNonSafepoints $async_profiler_options"

java ${java_options} -jar target/distributed-algorithms-0.0.1-SNAPSHOT.jar || exit 1

open $profile_graph
