name := "easy-rpc"

version := "1.0"

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  "ch.qos.logback" % "logback-classic" % "1.1.3",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.1.0",

  "com.typesafe.akka" %% "akka-stream-experimental" % "2.0.1",
  "org.scalatest" %% "scalatest" % "2.2.4",

  "com.lihaoyi" %% "autowire" % "0.2.5",
  "me.chrons" %% "boopickle" % "1.1.0"
)