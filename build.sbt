name := "easy-rpc"

organization := "me.archdev"

version := "1.0.1"

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  "com.typesafe.scala-logging" %% "scala-logging" % "3.1.0",

  "com.typesafe.akka" %% "akka-stream-experimental" % "2.0.3",
  "org.scalatest" %% "scalatest" % "2.2.4",

  "com.lihaoyi" %% "autowire" % "0.2.5",
  "me.chrons" %% "boopickle" % "1.1.0"
)

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}

publishMavenStyle := true

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

pomExtra in Global := (
  <url>https://github.com/ArchDev/easy-rpc</url>
    <licenses>
      <license>
        <name>MIT</name>
        <url>https://opensource.org/licenses/MIT</url>
        <distribution>repo</distribution>
      </license>
    </licenses>
    <scm>
      <url>git@github.com:ArchDev/easy-rpc.git</url>
      <connection>scm:git:git@github.com:ArchDev/easy-rpc.git</connection>
    </scm>
    <developers>
      <developer>
        <id>ArchDev</id>
        <name>Arthur Kushka</name>
        <url>http://archdev.me</url>
      </developer>
    </developers>)
