val Http4sVersion = "0.20.0-RC1"
val CirceVersion = "0.11.1"
val Specs2Version = "4.1.0"
val LogbackVersion = "1.2.3"
val Log4CatsVersion = "0.3.0"

lazy val root = (project in file("."))
  .settings(
    organization := "com.github.rzeigler",
    name := "http4s-blaze-client-toy",
    version := "0.0.1-SNAPSHOT",
    scalaVersion := "2.12.8",
    scalacOptions ++= Seq("-Ypartial-unification"),
    libraryDependencies ++= Seq(
      "org.http4s"      %% "http4s-blaze-server" % Http4sVersion,
      "org.http4s"      %% "http4s-blaze-client" % Http4sVersion,
      "org.http4s"      %% "http4s-async-http-client" % Http4sVersion,
      "org.http4s"      %% "http4s-circe"        % Http4sVersion,
      "io.chrisdavenport" %% "log4cats-core"     % Log4CatsVersion,
      "io.chrisdavenport" %% "log4cats-extras"  % Log4CatsVersion,
      "io.chrisdavenport" %% "log4cats-slf4j"   % Log4CatsVersion,
      "io.circe"        %% "circe-generic"       % CirceVersion,
      "ch.qos.logback"  %  "logback-classic"     % LogbackVersion
    ),
    addCompilerPlugin("org.spire-math" %% "kind-projector"     % "0.9.6"),
    addCompilerPlugin("com.olegpy"     %% "better-monadic-for" % "0.2.4")
  )

scalacOptions ++= Seq(
  "-deprecation",
  "-encoding", "UTF-8",
  "-language:higherKinds",
  "-language:postfixOps",
  "-feature",
  "-Ypartial-unification",
  "-Xfatal-warnings",
)
