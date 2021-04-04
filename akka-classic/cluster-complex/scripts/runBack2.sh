#!/usr/bin/env bash

sbt -Dakka.cluster.roles.0="backend" -DPORT=2562 "akka-classic-cluster-complex/run"