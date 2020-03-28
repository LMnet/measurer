scalaVersion := "2.13.1"

name := "measurer"

organization := "org.measurer"

version := "0.0.1-SNAPSHOT"

libraryDependencies ++= Seq(
  "io.dropwizard.metrics" % "metrics-core" % "4.1.2"
)
