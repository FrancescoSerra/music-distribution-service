import sbt._

object Dependencies {

  object V {
    val cats = "2.13.0"
    val catsEffect = "3.6.3"
    val newtype = "0.4.4"
    val refined = "0.11.0"

    val betterMonadicFor = "0.3.1"
    val kindProjector = "0.13.3"
    val munit = "1.0.7"
    val scalacheckEffect = "1.0.4"
    val munitScalacheck = "0.7.29"
    val catsScalacheck = "0.3.2"

    val monocle = "3.1.0"
  }

  object Libraries {
    def monocle(artifact: String): ModuleID = "dev.optics"     %% s"monocle-$artifact"  % V.monocle
    def refined(artifact: String): ModuleID =
      "eu.timepit" %% s"refined${if (artifact.isEmpty) "" else s"-$artifact"}" % V.refined

    val cats = "org.typelevel"              %% "cats-core"           % V.cats
    val catsEffect = "org.typelevel"        %% "cats-effect"         % V.catsEffect
    val catsEffectTestkit = "org.typelevel" %% "cats-effect-testkit" % V.catsEffect

    val refinedCore = refined("")
    val refinedCats = refined("cats")

    val newtype = "io.estatico" %% "newtype" % V.newtype

    val refinedScalacheck = refined("scalacheck")
    val munitCats = "org.typelevel"             %% "munit-cats-effect-3"     % V.munit
    val disciplineMunit = "org.typelevel"       %% "discipline-munit"        % V.munit
    val munitScalaCheckEffect = "org.typelevel" %% "scalacheck-effect-munit" % V.scalacheckEffect
    val munitScalaCheck = "org.scalameta"       %% "munit-scalacheck"        % V.munitScalacheck
    val catsScalacheck = "io.chrisdavenport"    %% "cats-scalacheck"         % V.catsScalacheck

    val monocleCore = monocle("core")

    val apacheCommonsText = "org.apache.commons" % "commons-text" % "1.14.0"
  }


  object CompilerPlugin {
    val betterMonadicFor = compilerPlugin(
      "com.olegpy" %% "better-monadic-for" % V.betterMonadicFor
    )
    val kindProjector = compilerPlugin(
      "org.typelevel" % "kind-projector" % V.kindProjector cross CrossVersion.full
    )
  }
}
