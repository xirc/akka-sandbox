#!/usr/bin/env bash

sbt -Dakka.cluster.roles.0="sender" -DPORT=2551 "akka-classic-cluster-pubsub-p2p/run"