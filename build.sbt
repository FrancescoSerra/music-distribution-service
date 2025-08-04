import org.typelevel.sbt.tpolecat.*
import Dependencies.*

ThisBuild / organization := "com.iceservices"
ThisBuild / scalaVersion := "2.13.16"
ThisBuild / tpolecatDefaultOptionsMode := VerboseMode

lazy val root = (project in file(".")).settings(
  name := "music-distribution-service",
).aggregate(core, tests)

lazy val tests = (project in file("modules/tests"))
  .settings(
    libraryDependencies ++= Seq(
      CompilerPlugin.kindProjector,
      CompilerPlugin.betterMonadicFor,
      Libraries.catsEffectTestkit,
      Libraries.refinedScalacheck,
      Libraries.munitCats,
      Libraries.munitScalaCheck,
      Libraries.munitScalaCheckEffect,
      Libraries.catsScalacheck,
    )
  )
  .dependsOn(core)

lazy val core = (project in file("modules/core"))
  .settings(
    scalacOptions ++= List("-Ymacro-annotations", "-Wnonunit-statement", "-Wvalue-discard"),
    libraryDependencies ++= Seq(
      CompilerPlugin.kindProjector,
      CompilerPlugin.betterMonadicFor,
      Libraries.cats,
      Libraries.catsEffect,
      Libraries.newtype,
      Libraries.refinedCore,
      Libraries.refinedCats,
      Libraries.monocleCore,
      Libraries.apacheCommonsText
    )
  )
