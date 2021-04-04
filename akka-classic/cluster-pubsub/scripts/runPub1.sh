#!/usr/bin/env bash

sbt -Dakka.cluster.roles.0="publisher" -DPORT=2551 -DTOPIC="content1" "akka-classic-cluster-pubsub/run"