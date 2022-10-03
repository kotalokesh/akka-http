ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.8"
val AkkaVersion = "2.6.8"
val AkkaHttpVersion = "10.2.9"
lazy val root = (project in file("."))
  .settings(
    name := "akka",

  )
libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,
  "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
  "com.typesafe.akka" %% "akka-http" % AkkaHttpVersion,
  "com.typesafe.akka" % "akka-http-spray-json_2.13" % AkkaHttpVersion,
  "org.scalaj" %% "scalaj-http" % "2.4.2",
  "ch.megard" % "akka-http-cors_2.13" % "1.1.1",
  "io.jsonwebtoken" % "jjwt" % "0.9.1",
  "com.github.jwt-scala" %% "jwt-core" % "9.1.1"
)