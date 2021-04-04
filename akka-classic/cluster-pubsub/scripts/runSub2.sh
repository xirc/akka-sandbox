#!/usr/bin/env bash

sbt -Dakka.cluster.roles.0="subscriber" -DPORT=2562 -DTOPIC="content2" "akka-classic-cluster-pubsub/run"