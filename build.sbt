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

val AkkaVersion = "2.6.9"
lazy val akka = (project in file("akka")).settings(
  libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,
    "com.typesafe.akka" %% "akka-actor-testkit-typed" % AkkaVersion,
    "ch.qos.logback" % "logback-classic" % "1.2.3",
    "org.scalatest" %% "scalatest-wordspec" % "3.2.2" % Test,
    "org.scalatest" %% "scalatest-shouldmatchers" % "3.2.2" % Test,
  )
)
