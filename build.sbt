scalaVersion := "2.11.1"

resolvers ++= Seq(
  "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases",
  "Anormcypher Repository" at "http://repo.anormcypher.org/"
)

libraryDependencies ++= Seq(
  "joda-time" % "joda-time" % "2.3",
  "net.databinder.dispatch" %% "dispatch-core" % "0.11.1",
  "net.sourceforge.htmlunit" % "htmlunit" % "2.15",
  "org.json4s" %% "json4s-native" % "3.2.10",
  "com.github.tototoshi" %% "scala-csv" % "1.0.0",
  "org.anormcypher" %% "anormcypher" % "0.5.1"
)
