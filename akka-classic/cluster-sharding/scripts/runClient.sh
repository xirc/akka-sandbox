#!/usr/bin/env bash

if [ "$#" -ne 3 ]; then
  echo "usage: $0 {PORT} {ID} {DELTA}"
  exit 1
fi
sbt -Dakka.cluster.roles.0="client" -DPORT="$1" -DCLIENT_ID="$2" -DCLIENT_DELTA="$3" "akka-classic-cluster-sharding/run"