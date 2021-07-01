import _root_.io.github.davidgregory084.ScalacOption

name := "elevator-simulation"

ThisBuild / organization := "elevator"
ThisBuild / scalaVersion := "3.1.2"

ThisBuild / scalafixDependencies += "com.github.liancheng" %% "organize-imports" % "0.6.0"

ThisBuild / testFrameworks += new TestFramework("munit.Framework")

val extraScalacOptions = Set(
  new ScalacOption(List("-indent")),
  new ScalacOption(List("-new-syntax")),
  ScalacOptions.sourceFuture,
  ScalacOptions.languageFeatureOption("strictEquaility")
)

val core = project.settings(
  tpolecatScalacOptions ++= extraScalacOptions,
  Settings.core
)

val shell = project
  .dependsOn(core % s"$Compile->$Compile;$Test->$Test")
  .configs(IntegrationTest)
  .settings(
    tpolecatScalacOptions ++= extraScalacOptions,
    Defaults.itSettings,
    Settings.shell
  )

val root = (project in file("."))
  .aggregate(
    core,
    shell
  )
