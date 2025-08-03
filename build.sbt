import org.typelevel.sbt.tpolecat.*
import Dependencies.*

ThisBuild / organization := "com.iceservices"
ThisBuild / scalaVersion := "2.13.16"

// This disables fatal-warnings for local development. To enable it in CI set the `SBT_TPOLECAT_CI` environment variable in your pipeline.
// See https://github.com/typelevel/sbt-tpolecat/?tab=readme-ov-file#modes
ThisBuild / tpolecatDefaultOptionsMode := VerboseMode

lazy val root = (project in file(".")).settings(
  name := "music-distribution-service",
)
                                      .aggregate(core, tests)

lazy val tests = (project in file("modules/tests"))
  .settings(
    libraryDependencies ++= Seq(
      CompilerPlugin.kindProjector,
      CompilerPlugin.betterMonadicFor,
      Libraries.catsEffectTestkit,
      Libraries.log4catsNoOp,
      Libraries.refinedScalacheck,
      Libraries.munitCats,
      Libraries.munitScalaCheck,
      Libraries.munitScalaCheckEffect,
      Libraries.catsScalacheck,
      Libraries.disciplineMunit
    )
  )
  .dependsOn(core)

lazy val core = (project in file("modules/core"))
  .settings(
    scalacOptions ++= List("-Ymacro-annotations", "-Wnonunit-statement", "-Wvalue-discard"),
    Compile / mainClass := Some("com.mdsol.mdr.MdrApp"),
    libraryDependencies ++= Seq(
      CompilerPlugin.kindProjector,
      CompilerPlugin.betterMonadicFor,
      Libraries.cats,
      Libraries.catsEffect,
      Libraries.circeCore,
      Libraries.circeEnum,
      Libraries.circeGeneric,
      Libraries.circeParser,
      Libraries.circeRefined,
      Libraries.circeFs2,
      Libraries.circeOptics,
      Libraries.cirisCore,
      Libraries.cirisEnum,
      Libraries.cirisRefined,
      Libraries.fs2core,
      Libraries.fs2io,
      Libraries.jawnFs2,
      Libraries.http4sDsl,
      Libraries.http4sServer,
      Libraries.http4sClient,
      Libraries.http4sCirce,
      Libraries.log4catsSlf4j,
      Libraries.logback % Runtime,
      Libraries.newtype,
      Libraries.refinedCore,
      Libraries.refinedCats,
      Libraries.circeConfig,
      Libraries.monocleCore,
      Libraries.apacheCommonsText
    )
  )
