ThisBuild / organization := "com.retisio.arc"
ThisBuild / scalaVersion  := "2.13.11"
ThisBuild / name := "catalog-ext"

lazy val `retisio-client-ext` = (project in file("."))
  .enablePlugins(PlayMinimalJava, LauncherJarPlugin)
  .settings(common)
  .aggregate(`catalog-ext-api`, `catalog-ext-impl`)
  .dependsOn(`catalog-ext-impl`)

lazy val `catalog-ext-api` = (project in file("catalog-ext-api"))
  .settings(common)
  .settings(
    libraryDependencies ++= Seq(
      lombok,
      catalogImpl
    )
  )

lazy val `catalog-ext-impl` = (project in file("catalog-ext-impl"))
  .enablePlugins(JavaAgent)
  .settings(common)
  .settings(
    libraryDependencies ++= Seq(
      guice
    )
  )
  .dependsOn(`catalog-ext-api`)

val lombok = "org.projectlombok" % "lombok" % "1.18.2"
val catalogImpl = "com.retisio.arc" %% "catalog-impl" % "1.0.0-SNAPSHOT"

def common = Seq(
  dockerExposedPorts := Seq(9000, 8558, 9091, 10001),
  dockerBaseImage := "openjdk:11.0.11-jre-slim",
  Compile / doc / sources := Seq.empty,
  Compile / javacOptions := Seq("-g", "-encoding", "UTF-8", "-Xlint:unchecked", "-Xlint:deprecation", "-parameters"),
  libraryDependencies ++= Dependencies.dependencies
)
Global / excludeLintKeys += dockerBaseImage
Global / excludeLintKeys += dockerExposedPorts