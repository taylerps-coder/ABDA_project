name := "Restaurant-Discovery"
version := "1.0.0"
scalaVersion := "3.3.3"

libraryDependencies ++= Seq(
  // Web framework (simple, beginner-friendly HTTP server)
  "com.lihaoyi" %% "cask" % "0.9.4",
  // JSON library
  "com.lihaoyi" %% "upickle" % "3.3.1",
  // MongoDB Official Scala Driver (using Scala 2.13 artifact compatible with Scala 3)
  ("org.mongodb.scala" %% "mongo-scala-driver" % "5.1.0").cross(CrossVersion.for3Use2_13),
  // Simple logging for MongoDB driver and server
  "org.slf4j" % "slf4j-simple" % "2.0.13",
  // Unit testing
  "org.scalameta" %% "munit" % "1.0.0" % Test
)

// Fork the process so sbt run handles the HTTP server cleanly
fork := true

ThisBuild / assemblyMergeStrategy := {
  case PathList("META-INF", "services", xs @ _*) => MergeStrategy.concat
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case x => MergeStrategy.first
}
