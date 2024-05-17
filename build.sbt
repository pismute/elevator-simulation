import scala.Ordering.Implicits._

import org.typelevel.scalacoptions.*
import org.typelevel.scalacoptions.ScalaVersion.*

name := "elevator-simulation"

ThisBuild / organization := "elevator"
ThisBuild / scalaVersion := "3.4.2"

ThisBuild / scalafixDependencies += "com.github.liancheng" %% "organize-imports" % "0.6.0"
ThisBuild / scalafixDependencies += "org.typelevel" %% "typelevel-scalafix" % "0.1.6"
ThisBuild / scalafixDependencies += "org.typelevel" %% "typelevel-scalafix-cats" % "0.1.6"
ThisBuild / scalafixDependencies += "org.typelevel" %% "typelevel-scalafix-cats-effect" % "0.1.6"
ThisBuild / scalafixDependencies += "org.typelevel" %% "typelevel-scalafix-fs2" % "0.1.6"
ThisBuild / scalafixDependencies += "org.typelevel" %% "typelevel-scalafix-http4s" % "0.1.6"

ThisBuild / testFrameworks += new TestFramework("munit.Framework")

val extraScalacOptions = Set(
  ScalacOption("-indent", Nil, _ >= V3_0_0),
  ScalacOption("-new-syntax", Nil, _ >= V3_0_0),
  ScalacOptions.sourceFuture,
  ScalacOptions.languageFeatureOption("strictEquaility")
)

val core = project
  .settings(
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

ThisBuild / githubWorkflowPublishTargetBranches := Seq() // disable publish

ThisBuild / githubWorkflowTargetBranches := Seq("master")

val scalaSteward = "pismute-steward[bot]"

ThisBuild / mergifyStewardConfig := Some(
  MergifyStewardConfig(
    author = scalaSteward,
    mergeMinors = true,
    action = MergifyAction.Merge(Some("squash"))
  )
)

ThisBuild / mergifyPrRules += {
  val authorCondition = MergifyCondition.Custom("author=pismute-steward[bot]")
  MergifyPrRule(
    "label scala-steward's PRs",
    List(authorCondition),
    List(MergifyAction.Label(List("dependency-update")))
  )
}
