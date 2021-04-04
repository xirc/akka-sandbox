ThisBuild / version := "0.1"
ThisBuild / scalaVersion := "2.13.3"
ThisBuild / scalacOptions ++= Seq(
  "-deprecation",
  "-feature",
  "-unchecked",
  "-Xlint",
  "-Werror"
)

addCommandAlias(
  name = "ciFormat",
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
    "test:compile",
    "test"
  ).mkString(";")
)

// ===

val AkkaVersion = "2.6.9"
val AkkaHttpVersion = "10.1.13"
val ScalaTestVersion = "3.2.2"
val LogBackVersion = "1.2.3"

lazy val akka = (project in file("akka")).settings(
  libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,
    "com.typesafe.akka" %% "akka-actor-testkit-typed" % AkkaVersion,
    "ch.qos.logback" % "logback-classic" % LogBackVersion,
    "org.scalatest" %% "scalatest-wordspec" % ScalaTestVersion % Test,
    "org.scalatest" %% "scalatest-shouldmatchers" % ScalaTestVersion % Test
  )
)

lazy val akkaClassicActor = (project in file("akka-classic/actor")).settings(
  name := "akka-classic-actor",
  libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-actor" % AkkaVersion,
    "com.typesafe.akka" %% "akka-testkit" % AkkaVersion % Test,
    "org.scalatest" %% "scalatest" % ScalaTestVersion % Test
  )
)

lazy val akkaClassicFSM = (project in file("akka-classic/fsm")).settings(
  name := "akka-classic-fsm",
  libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-actor" % AkkaVersion,
    "com.typesafe.akka" %% "akka-testkit" % AkkaVersion % Test,
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
      "org.scalatest" %% "scalatest" % ScalaTestVersion % Test
    )
  )

lazy val akkaStreams = (project in file("akka-streams")).settings(
  name := "akka-streams",
  libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
    "com.typesafe.akka" %% "akka-testkit" % AkkaVersion % Test,
    "com.typesafe.akka" %% "akka-stream-testkit" % AkkaVersion % Test,
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
    "ch.qos.logback" % "logback-classic" % LogBackVersion,
    "com.typesafe.akka" %% "akka-http-testkit" % AkkaHttpVersion % Test,
    "com.typesafe.akka" %% "akka-actor-testkit-typed" % AkkaVersion % Test,
    "org.scalatest" %% "scalatest" % ScalaTestVersion % Test
  )
)
