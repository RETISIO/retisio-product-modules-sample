ThisBuild / organization := "com.retisio.arc"
ThisBuild / scalaVersion  := "2.13.11"
ThisBuild / name := "retisio-product-sample"

lazy val `retisio-product-sample` = (project in file("."))
  .enablePlugins(PlayMinimalJava, LauncherJarPlugin)
  .settings(common)
  .aggregate(`catalog-api`, `catalog-impl`)
  .dependsOn(`catalog-impl`)

lazy val `catalog-api` = (project in file("catalog-api"))
  .settings(common)
  .settings(
    libraryDependencies ++= Seq(
      lombok,
      retisioCommon
    )
  )

lazy val `catalog-impl` = (project in file("catalog-impl"))
  .enablePlugins(JavaAgent)
  .settings(common)
  .settings(
    libraryDependencies ++= Seq(
      guice
    )
  )
  .dependsOn(`catalog-api`)

val lombok = "org.projectlombok" % "lombok" % "1.18.2"
val retisioCommon = "com.retisio.arc" %% "retisio-common" % "1.0.0-SNAPSHOT"


def common = Seq(
  dockerExposedPorts := Seq(9000, 8558, 9091, 10001),
  dockerBaseImage := "openjdk:11.0.11-jre-slim",
  Compile / doc / sources := Seq.empty,
  Compile / javacOptions := Seq("-g", "-encoding", "UTF-8", "-Xlint:unchecked", "-Xlint:deprecation", "-parameters"),
  libraryDependencies ++= Dependencies.dependencies
)
Global / excludeLintKeys += dockerBaseImage
Global / excludeLintKeys += dockerExposedPorts