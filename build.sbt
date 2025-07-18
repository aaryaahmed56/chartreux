ThisBuild / tlBaseVersion := "0.0" // your current series x.y

ThisBuild / organization := "com.aahmed"
ThisBuild / organizationName := "Aarya Ahmed"
ThisBuild / startYear := Some(2023)
ThisBuild / licenses := Seq(License.Apache2)
ThisBuild / developers := List(
  tlGitHubDev("aaryaahmed56", "Aarya Ahmed")
)

ThisBuild / tlSonatypeUseLegacyHost := false

val scala212 = "2.12.10"
val scala213 = "2.13.16"

val catsVersion = "2.9.0"
val munitVersion = "1.0.0-M7"
val disciplineMunitVersion = "2.0.0-M3"
val kindProjectorVersion = "0.13.2"
val shapeless2Version = "2.3.10"
val akkaVersion = "2.8.0"

ThisBuild / crossScalaVersions := Seq(scala213, scala212)
ThisBuild / scalaVersion := scala213

lazy val commonSettings = Seq(
  scalacOptions ++= Seq(
    "-feature",
    "-language:higherKinds",
    "-language:implicitConversions",
    "-unchecked",
    "-deprecation",
    "-Xfatal-warnings"
  ),
  scalacOptions ++= CrossVersion.partialVersion(scalaVersion.value).toList.flatMap {
    case (2, 12) => "-Ypartial-unification" :: Nil
    case _ => Nil
  },
  resolvers ++= Resolver.sonatypeOssRepos("releases"),
  resolvers ++= Resolver.sonatypeOssRepos("snapshots"),
  libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-stream" % akkaVersion,
    "com.typesafe.akka" %% "akka-stream-typed" % akkaVersion,
    "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % Test,
    "org.mongodb.scala" %% "mongo-scala-driver" % "4.9.0",
    "org.typelevel" %% "cats-core" % catsVersion,
    "org.typelevel" %% "alleycats-core" % catsVersion,
    "org.typelevel" %% "cats-testkit" % catsVersion % Test,
    "org.typelevel" %% "discipline-munit" % disciplineMunitVersion % Test,
    "org.scalameta" %% "munit" % munitVersion % Test
  ),
  libraryDependencies ++= (CrossVersion.partialVersion(scalaVersion.value) match {
    case _ =>
      Seq(
        "org.scala-lang" % "scala-reflect" % scalaVersion.value % Test,
        compilerPlugin(("org.typelevel" %% "kind-projector" % kindProjectorVersion).cross(CrossVersion.full))
      )
  }),
  Test / parallelExecution := false
)

lazy val root = tlCrossRootProject.aggregate(core)

lazy val core = crossProject(JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("core"))
  .settings(name := "chartreux")
  .settings(commonSettings *)
