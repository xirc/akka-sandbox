#!/usr/bin/env bash

sbt -Dakka.cluster.roles.0="backend" -DPORT=2561 "akka-classic-cluster-complex/run"