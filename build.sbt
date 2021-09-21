ThisBuild / version := "0.1"
ThisBuild / scalaVersion := "2.13.6"
ThisBuild / scalacOptions ++= Seq(
  "-deprecation",
  "-feature",
  "-unchecked",
  "-Xlint",
  "-Werror"
)

addCommandAlias(
  "ciFormat",
  Seq(
    "scalafmtSbt",
    "scalafmtAll"
  ).mkString(";")
)

addCommandAlias(
  "ciCheck",
  Seq(
    "clean",
    "scalafmtSbtCheck",
    "scalafmtCheckAll",
    "Test / compile",
    "test"
  ).mkString(";")
)

// ===

val AkkaVersion = "2.6.16"
val AkkaHttpVersion = "10.2.6"
val ScalaTestVersion = "3.2.10"
val LogBackVersion = "1.2.6"

lazy val akka = (project in file("akka")).settings(
  libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,
    "com.typesafe.akka" %% "akka-actor-testkit-typed" % AkkaVersion,
    "ch.qos.logback" % "logback-classic" % LogBackVersion,
    "org.scalactic" %% "scalactic" % ScalaTestVersion,
    "org.scalatest" %% "scalatest" % ScalaTestVersion % Test
  )
)

lazy val akkaPersistence = (project in file("akka-persistence")).settings(
  name := "akka-persistence",
  libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-actor-testkit-typed" % AkkaVersion,
    "com.typesafe.akka" %% "akka-persistence-typed" % AkkaVersion,
    "com.typesafe.akka" %% "akka-persistence-testkit" % AkkaVersion % Test,
    "com.typesafe.akka" %% "akka-serialization-jackson" % AkkaVersion,
    "ch.qos.logback" % "logback-classic" % LogBackVersion,
    "org.scalactic" %% "scalactic" % ScalaTestVersion,
    "org.scalatest" %% "scalatest" % ScalaTestVersion % Test
  )
)

lazy val akkaClassicActor = (project in file("akka-classic/actor")).settings(
  name := "akka-classic-actor",
  libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-actor" % AkkaVersion,
    "com.typesafe.akka" %% "akka-testkit" % AkkaVersion % Test,
    "org.scalactic" %% "scalactic" % ScalaTestVersion,
    "org.scalatest" %% "scalatest" % ScalaTestVersion % Test
  )
)

lazy val akkaClassicClusterSimple =
  (project in file("akka-classic/cluster-simple")).settings(
    name := "akka-classic-cluster-simple",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-cluster" % AkkaVersion
    )
  )

lazy val akkaClassicClusterComplex =
  (project in file("akka-classic/cluster-complex")).settings(
    name := "akka-classic-cluster-complex",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-cluster" % AkkaVersion,
      "com.typesafe.akka" %% "akka-testkit" % AkkaVersion % Test,
      "com.typesafe.akka" %% "akka-multi-node-testkit" % AkkaVersion % Test,
      "org.scalactic" %% "scalactic" % ScalaTestVersion,
      "org.scalatest" %% "scalatest" % ScalaTestVersion % Test
    )
  )

lazy val akkaClassicClusterSingleton =
  (project in file("akka-classic/cluster-singleton")).settings(
    name := "akka-classic-cluster-singleton",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-cluster" % AkkaVersion,
      "com.typesafe.akka" %% "akka-cluster-tools" % AkkaVersion
    )
  )

lazy val akkaClassicClusterPubSub =
  (project in file("akka-classic/cluster-pubsub")).settings(
    name := "akka-classic-cluster-pubsub",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-cluster" % AkkaVersion,
      "com.typesafe.akka" %% "akka-cluster-tools" % AkkaVersion
    )
  )

lazy val akkaClassicClusterPubSub2PP =
  (project in file("akka-classic/cluster-pubsub-p2p")).settings(
    name := "akka-classic-cluster-pubsub-p2p",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-cluster" % AkkaVersion,
      "com.typesafe.akka" %% "akka-cluster-tools" % AkkaVersion
    )
  )

lazy val akkaClassicClusterSharding =
  (project in file("akka-classic/cluster-sharding")).settings(
    name := "akka-classic-cluster-sharding",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-cluster" % AkkaVersion,
      "com.typesafe.akka" %% "akka-cluster-sharding" % AkkaVersion
    )
  )

lazy val akkaClassicFSM = (project in file("akka-classic/fsm")).settings(
  name := "akka-classic-fsm",
  libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-actor" % AkkaVersion,
    "com.typesafe.akka" %% "akka-testkit" % AkkaVersion % Test,
    "org.scalactic" %% "scalactic" % ScalaTestVersion,
    "org.scalatest" %% "scalatest" % ScalaTestVersion % Test
  )
)

lazy val akkaClassicRouter = (project in file("akka-classic/router")).settings(
  name := "akka-classic-router",
  libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-actor" % AkkaVersion
  )
)

lazy val akkaSerialization =
  (project in file("akka-serialization")).settings(
    name := "akka-serialization",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor" % AkkaVersion,
      "com.typesafe.akka" %% "akka-serialization-jackson" % AkkaVersion,
      "com.typesafe.akka" %% "akka-cluster" % AkkaVersion,
      "com.typesafe.akka" %% "akka-testkit" % AkkaVersion % Test,
      "org.scalactic" %% "scalactic" % ScalaTestVersion,
      "org.scalatest" %% "scalatest" % ScalaTestVersion % Test
    )
  )

lazy val akkaStreams = (project in file("akka-streams")).settings(
  name := "akka-streams",
  libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
    "com.typesafe.akka" %% "akka-testkit" % AkkaVersion % Test,
    "com.typesafe.akka" %% "akka-stream-testkit" % AkkaVersion % Test,
    "org.scalactic" %% "scalactic" % ScalaTestVersion,
    "org.scalatest" %% "scalatest" % ScalaTestVersion % Test
  )
)

lazy val akkaHttp = (project in file("akka-http")).settings(
  name := "akka-http",
  libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-http" % AkkaHttpVersion,
    "com.typesafe.akka" %% "akka-http-spray-json" % AkkaHttpVersion,
    "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,
    "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
    "com.typesafe.akka" %% "akka-http-testkit" % AkkaHttpVersion % Test,
    "com.typesafe.akka" %% "akka-actor-testkit-typed" % AkkaVersion % Test,
    "ch.qos.logback" % "logback-classic" % LogBackVersion,
    "org.scalactic" %% "scalactic" % ScalaTestVersion,
    "org.scalatest" %% "scalatest" % ScalaTestVersion % Test
  )
)
