#!/usr/bin/env bash

sbt -Dakka.cluster.roles.0="subscriber" -DPORT=2561 -DTOPIC="content1" "akka-classic-cluster-pubsub/run"