import _root_.io.github.davidgregory084.ScalacOption

name := "elevator-simulation"

ThisBuild / organization := "elevator"
ThisBuild / scalaVersion := "3.2.2"

ThisBuild / scalafixDependencies += "com.github.liancheng" %% "organize-imports" % "0.6.0"

ThisBuild / testFrameworks += new TestFramework("munit.Framework")

val extraScalacOptions = Set(
  new ScalacOption("-indent", Nil, _ => true),
  new ScalacOption("-new-syntax", Nil, _ => true),
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

ThisBuild / githubWorkflowPublishTargetBranches := Seq() // disable publish

ThisBuild / githubWorkflowIncludeClean := false // publish disabled, clean is unnecessary

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
