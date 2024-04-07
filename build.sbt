ThisBuild / version := "0.1.1-SNAPSHOT"
// ThisBuild / name := "typelevel-project"

lazy val rockthejvm = "com.rockthejvm"
lazy val scala3Version = "3.4.0"

///////////////////////////////////////////////////////////////////////////////////////////////////////////
// Common - contains domain model
///////////////////////////////////////////////////////////////////////////////////////////////////////////

lazy val catsEffectVersion = "3.5.3"
lazy val http4sVersion = "0.23.25"
lazy val http4sBlazeVersion = "0.23.16"
lazy val http4sJDKClientVersion = "0.9.1"
lazy val circeVersion = "0.14.0"
lazy val jsoniterVersion = "2.28.4"
lazy val tapirVersion = "1.9.6"
lazy val monocleVersion = "3.2.0"
lazy val spireVersion = "0.18.0"
lazy val fs2Version = "3.10.2"

lazy val common = (crossProject(JSPlatform, JVMPlatform) in file("common"))
  .settings(
    name := "common",
    scalaVersion := scala3Version,
    organization := rockthejvm,
    scalacOptions ++= Seq("-source:future", "-no-indent", "-Vprofile"),
    libraryDependencies ++= Seq(
      "org.typelevel" %%% "cats-effect" % catsEffectVersion,
      "org.http4s" %%% "http4s-client" % http4sVersion,
      "io.circe" %%% "circe-core" % circeVersion,
      "io.circe" %%% "circe-generic" % circeVersion,
      "com.github.plokhotnyuk.jsoniter-scala" %%% "jsoniter-scala-circe" % jsoniterVersion,
      "co.fs2" %%% "fs2-core" % fs2Version,
      "co.fs2" %%% "fs2-scodec" % fs2Version,
      "org.gnieh" %%% "fs2-data-json" % "1.10.0",
      "org.gnieh" %%% "fs2-data-json-circe" % "1.10.0",
      "com.softwaremill.sttp.tapir" %%% "tapir-core" % tapirVersion,
      "com.softwaremill.sttp.tapir" %%% "tapir-json-circe" % tapirVersion,
      "dev.optics" %%% "monocle-core" % monocleVersion,
      "dev.optics" %%% "monocle-macro" % monocleVersion,
      "org.typelevel" %%% "spire" % spireVersion,
      "io.bullet" %%% "borer-core" % "1.14.0",
      "io.bullet" %%% "borer-derivation" % "1.14.0",
      "io.bullet" %%% "borer-compat-scodec" % "1.14.0"
    ),
    semanticdbEnabled := true,
    autoAPIMappings := true,
    Compile / tpolecatExcludeOptions ++= Set(
      ScalacOptions.warnUnusedImports,
      ScalacOptions.warnUnusedPrivates,
      ScalacOptions.warnUnusedParams,
      ScalacOptions.warnUnusedLocals,
      ScalacOptions.warnUnusedExplicits,
      ScalacOptions.fatalWarnings
    )
  )
  .jvmSettings(
  )
  .jsSettings(
    // Add JS-specific settings here
  )

///////////////////////////////////////////////////////////////////////////////////////////////////////////
// Frontend
///////////////////////////////////////////////////////////////////////////////////////////////////////////

lazy val tyrianVersion = "0.10.0"
lazy val fs2DomVersion = "0.1.0"
lazy val laikaVersion = "0.19.0"

lazy val app = (project in file("app"))
  .enablePlugins(ScalaJSPlugin)
  .settings(
    name := "app",
    scalaVersion := scala3Version,
    organization := rockthejvm,
    scalacOptions ++= Seq("-Vprofile"),
    libraryDependencies ++= Seq(
      "io.indigoengine" %%% "tyrian-io" % tyrianVersion,
      "com.armanbilge" %%% "fs2-dom" % fs2DomVersion,
      "org.planet42" %%% "laika-core" % laikaVersion,
      "io.circe" %%% "circe-parser" % circeVersion,
      "org.http4s" %%% "http4s-dom" % "0.2.11"
    ),
    scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.CommonJSModule) },
    semanticdbEnabled := true,
    autoAPIMappings := true,
    Compile / tpolecatExcludeOptions ++= Set(
      ScalacOptions.warnUnusedImports,
      ScalacOptions.warnUnusedPrivates,
      ScalacOptions.warnUnusedParams,
      ScalacOptions.warnUnusedLocals,
      ScalacOptions.warnUnusedExplicits,
      ScalacOptions.fatalWarnings
    )
  ).dependsOn(common.js)

lazy val munitCEVersion = "2.0.0-M4"
lazy val doobieVersion = "1.0.0-RC5"
lazy val chimneyVersion = "0.8.3"
lazy val pureConfigVersion = "0.17.4"
lazy val log4catsVersion = "2.4.0"
lazy val munitVersion = "0.7.29"
lazy val weaverTestVersion = "0.8.3"
lazy val scalaTestVersion = "3.2.12"
lazy val scalaTestCatsEffectVersion = "1.4.0"
lazy val testContainerVersion = "1.17.3"
lazy val logbackVersion = "1.4.0"
lazy val slf4jVersion = "2.0.0"
lazy val javaMailVersion = "1.6.2"
lazy val catsMtlVersion = "1.3.0"
lazy val apacheCommonsVersion = "1.8.0"
lazy val monixNewtypesVersion = "0.2.3"
lazy val sttpClient3Version = "3.9.2"
lazy val embeddedPostgresVersion = "2.0.6"
lazy val catsRetryVersion = "3.1.0"
lazy val kittensVersion = "3.2.0"

import org.typelevel.scalacoptions.ScalacOptions

lazy val server = (project in file("server"))
  .configs(IntegrationTest)
  .enablePlugins(JavaAppPackaging, DockerPlugin)
  .settings(
    name := "typelevel-project",
    scalaVersion := scala3Version,
    organization := rockthejvm,
    scalacOptions ++= Seq("-source:future", "-no-indent", "-Vprofile"),
    Defaults.itSettings,
    libraryDependencies ++= Seq(
      "org.typelevel" %% "kittens" % kittensVersion,
      "org.http4s" %% "http4s-dsl" % http4sVersion,
      "org.http4s" %% "http4s-ember-server" % http4sVersion,
      "org.http4s" %% "http4s-blaze-core" % http4sBlazeVersion,
      "org.http4s" %% "http4s-blaze-server" % http4sBlazeVersion,
      "org.http4s" %% "http4s-ember-client" % http4sVersion,
      "org.http4s" %% "http4s-jdk-http-client" % http4sJDKClientVersion,
      "org.http4s" %% "http4s-circe" % http4sVersion,
      "org.http4s" %% "http4s-prometheus-metrics" % "0.24.6",
      "com.softwaremill.sttp.tapir" %% "tapir-http4s-server" % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-bundle" % tapirVersion,
      "org.tpolecat" %% "doobie-core" % doobieVersion,
      "org.tpolecat" %% "doobie-hikari" % doobieVersion,
      "org.tpolecat" %% "doobie-postgres" % doobieVersion,
      "org.tpolecat" %% "doobie-weaver" % doobieVersion % Test,
      "io.scalaland" %% "chimney" % chimneyVersion,
      "io.scalaland" %% "chimney-cats" % chimneyVersion,
      "com.github.pureconfig" %% "pureconfig-core" % pureConfigVersion,
      "org.typelevel" %% "log4cats-slf4j" % log4catsVersion,
      // "org.slf4j" % "slf4j-simple" % slf4jVersion,
      "org.typelevel" %% "log4cats-noop" % log4catsVersion % Test,
      // "org.scalameta" %% "munit" % munitVersion % Test,
      // "org.typelevel" %%% "munit-cats-effect" % munitCEVersion % "test",
      "com.disneystreaming" %% "weaver-cats" % weaverTestVersion % "it,test",
      "org.typelevel" %% "cats-effect-testing-scalatest" % scalaTestCatsEffectVersion % Test,
      "org.testcontainers" % "testcontainers" % testContainerVersion % Test,
      "org.testcontainers" % "postgresql" % testContainerVersion % Test,
      "ch.qos.logback" % "logback-classic" % logbackVersion,
      "org.typelevel" %% "cats-mtl" % catsMtlVersion,
      "commons-validator" % "commons-validator" % apacheCommonsVersion,
      "io.monix" %% "newtypes-core" % monixNewtypesVersion,
      "io.monix" %% "newtypes-circe-v0-14" % monixNewtypesVersion,
      "com.github.jwt-scala" %% "jwt-circe" % "9.4.5",
      "com.softwaremill.sttp.client3" %% "http4s-backend" % sttpClient3Version,
      "io.zonky.test" % "embedded-postgres" % embeddedPostgresVersion,
      "software.amazon.awssdk" % "aws-sdk-java" % "2.24.0",
      "com.github.cb372" %% "cats-retry" % catsRetryVersion,
      "org.typelevel" %% "cats-effect-testkit" % catsEffectVersion % Test
    ),
    semanticdbEnabled := true,
    autoAPIMappings := true,
    Compile / tpolecatExcludeOptions ++= Set(
      ScalacOptions.warnUnusedImports,
      ScalacOptions.warnUnusedPrivates,
      ScalacOptions.warnUnusedParams,
      ScalacOptions.warnUnusedLocals,
      ScalacOptions.warnUnusedExplicits,
      ScalacOptions.fatalWarnings
    ),
    dockerExposedPorts ++= Seq(4041),
    dockerBaseImage := "sbtscala/scala-sbt:eclipse-temurin-focal-11.0.22_7_1.9.8_3.3.1",
    Docker / daemonUserUid := None,
    Docker / daemonUser := "daemon",
    Docker / dockerRepository := Some("905418066033.dkr.ecr.eu-north-1.amazonaws.com"),
    Docker / dockerAlias := com
      .typesafe.sbt.packager.docker.DockerAlias(
        registryHost = Some("905418066033.dkr.ecr.eu-north-1.amazonaws.com"),
        username = None,
        name = "typelevel-project-backend",
        tag = Some("anotherversion")
      ),
    Docker / dockerUpdateLatest := true
  ).dependsOn(common.jvm)

enablePlugins(RevolverPlugin)

mainClass in reStart := Some("Main")
