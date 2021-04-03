name := "akka-sandbox"
version := "0.1"
scalaVersion := "2.13.3"
scalacOptions ++= Seq(
  "-deprecation",
  "-feature",
  "-unchecked",
  "-Xlint",
  "-Xfatal-warnings"
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

lazy val `akka-serialization` =
  (project in file("akka-serialization")).settings(
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor" % AkkaVersion,
      "com.typesafe.akka" %% "akka-serialization-jackson" % AkkaVersion,
      "com.typesafe.akka" %% "akka-cluster" % AkkaVersion,
      "com.typesafe.akka" %% "akka-testkit" % AkkaVersion % Test,
      "org.scalatest" %% "scalatest" % ScalaTestVersion % Test
    )
  )

lazy val `akka-streams` = (project in file("akka-streams")).settings(
  libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
    "com.typesafe.akka" %% "akka-testkit" % AkkaVersion % Test,
    "com.typesafe.akka" %% "akka-stream-testkit" % AkkaVersion % Test,
    "org.scalatest" %% "scalatest" % ScalaTestVersion % Test
  )
)
