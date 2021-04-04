#!/usr/bin/env bash

sbt -Dakka.cluster.roles.0="publisher" -DPORT=2552 -DTOPIC="content2" "akka-classic-cluster-pubsub/run"