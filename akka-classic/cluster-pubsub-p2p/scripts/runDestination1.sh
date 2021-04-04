#!/usr/bin/env bash

sbt -Dakka.cluster.roles.0="destination" -DPORT=2552 "akka-classic-cluster-pubsub-p2p/run"