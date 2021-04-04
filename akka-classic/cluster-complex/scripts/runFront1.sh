#!/usr/bin/env bash

sbt -Dakka.cluster.roles.0="frontend" -DPORT=2551 "akka-classic-cluster-complex/run"