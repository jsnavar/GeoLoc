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
      "org.scalameta" %% "munit" % "0.7.29" % Test),
    javaOptions ++= Seq("--add-exports=java.base/sun.nio.ch=ALL-UNNAMED"),
    scalacOptions ++= Seq("-language:implicitConversions", "-deprecation"),
    fork := true
  )
