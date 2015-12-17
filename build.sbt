name := "mud"
version := "0.1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.4.1"
)

//fork in run := true

scalaVersion := "2.11.7"
scalacOptions ++= Seq("-Xlint", "-Xfatal-warnings", "-unchecked", "-feature", "-deprecation")