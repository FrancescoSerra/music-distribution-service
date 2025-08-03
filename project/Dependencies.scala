import sbt._

object Dependencies {

  object V {
    val cats = "2.13.0"
    val catsEffect = "3.6.3"
    val circe = "0.14.1"
    val circeEnum = "1.9.0"
    val circeOptics = "0.15.1"
    val circeFs2 = "0.14.0" // for reasons, this module is lagging behind in version
    val ciris = "2.2.0"
    val derevo = "0.12.6"
    val fs2 = "3.1.3"
    val http4s = "0.23.13"
    val log4cats = "2.5.0"
    val newtype = "0.4.4"
    val refined = "0.11.0"
    val doobie = "1.0.0-RC1"
    val alleyCats = "2.11.0"

    val betterMonadicFor = "0.3.1"
    val kindProjector = "0.13.3"
    val organizeImports = "0.6.0"
    val semanticDB = "4.9.2"
    val munit = "2.0.0"
    val scalacheckEffect = "1.0.4"
    val munitScalacheck = "1.1.0"
    val catsScalacheck = "0.3.2"
    val logBack = "1.4.4"

    val mccLogging = "1.7.0"
    val mAuthLibs = "16.0.1"

    val flyway = "9.8.2"
    val circeConfig = "0.10.2"
    val monocle = "3.1.0"
    val kamon = "2.7.1" // this should ideally be kept in sync with the kamon version in mcc-scala-logging

    val redis4cats = "1.3.0"
    val sqsAwsLib = "2.20.37"
    val pact4s = "0.9.0"
  }

  object Libraries {
    def circe(artifact: String): ModuleID = "io.circe"         %% s"circe-$artifact"    % V.circe
    def ciris(artifact: String): ModuleID = "is.cir"           %% artifact              % V.ciris
    def derevo(artifact: String): ModuleID = "tf.tofu"         %% s"derevo-$artifact"   % V.derevo
    def http4s(artifact: String): ModuleID = "org.http4s"      %% s"http4s-$artifact"   % V.http4s
    def doobie(artifact: String): ModuleID = "org.tpolecat"    %% s"doobie-$artifact"   % V.doobie
    def monocle(artifact: String): ModuleID = "dev.optics"     %% s"monocle-$artifact"  % V.monocle
    def fs2(artifact: String): ModuleID = "co.fs2"             %% s"fs2-$artifact"      % V.fs2
    def log4cats(artifact: String): ModuleID = "org.typelevel" %% s"log4cats-$artifact" % V.log4cats
    def refined(artifact: String): ModuleID =
      "eu.timepit" %% s"refined${if (artifact.isEmpty) "" else s"-$artifact"}" % V.refined
    def mccScalaLogging(artifact: String): ModuleID = "com.mdsol"   %% s"mcc-scala-logging-$artifact" % V.mccLogging
    def kamon(artifact: String): ModuleID = "io.kamon"              %% s"kamon-$artifact"             % V.kamon
    def pact4s(artifact: String): ModuleID = "io.github.jbwheatley" %% s"pact4s-$artifact"            % V.pact4s

    val cats = "org.typelevel"              %% "cats-core"           % V.cats
    val alleycats = "org.typelevel"         %% "alleycats-core"      % V.alleyCats
    val catsEffect = "org.typelevel"        %% "cats-effect"         % V.catsEffect
    val catsEffectTestkit = "org.typelevel" %% "cats-effect-testkit" % V.catsEffect
    val fs2core = fs2("core")
    val fs2io = fs2("io")
    val jawnFs2 = "org.typelevel" %% "jawn-fs2" % "2.4.0"

    val circeCore = circe("core")
    val circeEnum = "com.beachape" %% "enumeratum-circe" % V.circeEnum
    val circeConfig = "io.circe"   %% "circe-config"     % V.circeConfig
    val circeGeneric = circe("generic")
    val circeGenericExtras = circe("generic-extras")
    val circeParser = circe("parser")
    val circeRefined = circe("refined")
    val circeFs2 = "io.circe"      %% s"circe-fs2"                % V.circeFs2
    val circeMagnolia = "io.circe" %% "circe-magnolia-derivation" % "0.7.0"
    val circeOptics = "io.circe"   %% "circe-optics"              % V.circeOptics

    val cirisCore = ciris("ciris")
    val cirisEnum = ciris("ciris-enumeratum")
    val cirisRefined = ciris("ciris-refined")

    val derevoCore = derevo("core")
    val derevoCats = derevo("cats")
    val derevoCirce = derevo("circe-magnolia")

    val http4sDsl = http4s("dsl")
    val http4sServer = http4s("ember-server")
    val http4sClient = http4s("ember-client")
    val http4sCirce = http4s("circe")

    val refinedCore = refined("")
    val refinedCats = refined("cats")

    val log4catsSlf4j = log4cats("slf4j")
    val newtype = "io.estatico" %% "newtype" % V.newtype
    val doobieCore = doobie("core")
    val doobieRefined = doobie("refined")
    val doobiePostgres = doobie("postgres")
    val doobieHikari = doobie("hikari")
    val doobieCirce = doobie("postgres-circe")

    // Runtime
    val logback = "ch.qos.logback" % "logback-classic" % V.logBack

    // Test
    val log4catsNoOp = log4cats("noop")
    val refinedScalacheck = refined("scalacheck")
    val munitCats = "org.typelevel"             %% "munit-cats-effect"     % V.munit
    val disciplineMunit = "org.typelevel"       %% "discipline-munit"        % V.munit
    val munitScalaCheckEffect = "org.typelevel" %% "scalacheck-effect-munit" % V.scalacheckEffect
    val munitScalaCheck = "org.scalameta"       %% "munit-scalacheck"        % V.munitScalacheck
    val catsScalacheck = "io.chrisdavenport"    %% "cats-scalacheck"         % V.catsScalacheck

    // Scalafix rules
    val organizeImports = "com.github.liancheng" %% "organize-imports" % V.organizeImports

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
    val semanticDB = compilerPlugin(
      "org.scalameta" %% "semanticdb-scalac" % V.semanticDB cross CrossVersion.full
    )
  }
}
