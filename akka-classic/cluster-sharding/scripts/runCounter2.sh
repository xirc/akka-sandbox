#!/usr/bin/env bash

sbt -Dakka.cluster.roles.0="counter" -DPORT=2552 "akka-classic-cluster-sharding/run"