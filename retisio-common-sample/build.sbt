ThisBuild / organization := "com.retisio.arc"
ThisBuild / scalaVersion  := "2.13.11"
ThisBuild / name := "retisio-common-sample"

lazy val `retisio-common-sample` = (project in file("."))
  .aggregate(`retisio-common`)

lazy val `retisio-common` = (project in file("retisio-common"))
  .settings(common)
  .settings(
    libraryDependencies ++= Seq(
      lombok,
      guice,
      commonslang3,
      jacksonCsv,
      commonsIo
    )
  )

val lombok = "org.projectlombok" % "lombok" % "1.18.2"
val commonslang3 = "org.apache.commons" % "commons-lang3" % "3.9"
val jacksonCsv = "com.fasterxml.jackson.dataformat" % "jackson-dataformat-csv" % "2.11.0"
val commonsIo = "commons-io" % "commons-io" % "2.11.0"

def common = Seq(
  Compile / doc / sources := Seq.empty,
  Compile / javacOptions := Seq("-g", "-encoding", "UTF-8", "-Xlint:unchecked", "-Xlint:deprecation", "-parameters"),
  libraryDependencies ++= Dependencies.dependencies
)