val scala2Version = "2.13.11"

lazy val root = (project in file("."))
  .settings(
    name := "GeoLoc",
    version := "0.0.1",
    scalaVersion := scala2Version,
    libraryDependencies ++= Seq(
      "org.apache.spark" %% "spark-core" % "3.4.1",
      "org.apache.spark" %% "spark-sql" % "3.4.1",
      "org.postgresql" % "postgresql" % "42.1.1",

      "com.typesafe.akka" %% "akka-actor-typed" % "2.7.0",
      "com.typesafe.akka" %% "akka-stream" % "2.7.0",
      "com.typesafe.akka" %% "akka-http" % "10.5.2",
      "com.typesafe.akka" %% "akka-http-spray-json" % "10.5.2",

      "org.scalacheck" %% "scalacheck" % "1.14.1" % Test,
      "org.scalameta" %% "munit" % "0.7.29" % Test),
    javaOptions ++= Seq("--add-exports=java.base/sun.nio.ch=ALL-UNNAMED"),
    scalacOptions ++= Seq("-language:implicitConversions", "-deprecation"),
    fork := true,
    run / connectInput := true,
    dependencyOverrides += "org.scala-lang.modules" %% "scala-parser-combinators" % "2.1.1"
  )
