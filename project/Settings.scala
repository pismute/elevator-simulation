import sbt.Keys.*
import sbt.*

object Settings {
  val coreTest: Seq[Setting[_]] = Seq.concat(
    Test / parallelExecution := true,
    Test / fork              := false,
    libraryDependencies ++= Seq(
      "org.scalameta" %% "munit"            % Versions.Munit           % Test,
      "org.scalameta" %% "munit-scalacheck" % Versions.Munit           % Test,
      "org.typelevel" %% "cats-laws"        % Versions.Cats            % Test,
      "org.typelevel" %% "discipline-munit" % Versions.DisciplineMunit % Test
    )
  )

  val core: Seq[Setting[_]] = Seq.concat(
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-core" % Versions.Cats,
      "org.typelevel" %% "cats-mtl"  % Versions.CatsMtl,
      "org.typelevel" %% "cats-time" % Versions.CatsTime,
      "org.typelevel" %% "kittens"   % Versions.Kittens
//      "org.typelevel" %% "shapeless3-deriving" % Versions.Shapeless,
//      "org.tpolecat"  %% "typename"  % Versions.Typename
    ),
    coreTest
  )

  val ItTest = "it,test"

  val shellTest: Seq[Setting[_]] = Seq(
    Test / parallelExecution            := true,
    Test / fork                         := false,
    IntegrationTest / parallelExecution := false,
    IntegrationTest / fork              := false,
    // turn off buffered logging
    IntegrationTest / testOptions += Tests.Argument(TestFrameworks.MUnit, "-b"),
    libraryDependencies ++= Seq(
      "org.scalameta" %% "munit"               % Versions.Munit           % ItTest,
      "org.scalameta" %% "munit-scalacheck"    % Versions.Munit           % ItTest,
      "org.typelevel" %% "cats-laws"           % Versions.Cats            % ItTest,
      "org.typelevel" %% "discipline-munit"    % Versions.DisciplineMunit % ItTest,
      "org.typelevel" %% "munit-cats-effect-3" % Versions.MunitCatsEffect % ItTest
    )
  )

  val shell: Seq[Setting[_]] = Seq.concat(
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-core"    % Versions.Cats,
      "org.typelevel" %% "cats-effect"  % Versions.CatsEffect,
      "org.typelevel" %% "cats-mtl"     % Versions.CatsMtl,
      "org.typelevel" %% "cats-testkit" % Versions.Cats,
      "org.typelevel" %% "kittens"      % Versions.Kittens,
      "co.fs2"        %% "fs2-core"     % Versions.Fs2
    ),
    shellTest
  )

}
