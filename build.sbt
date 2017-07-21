name := "bug-service"

version := "1.0"

scalaVersion := "2.11.8"

mainClass in Compile := Some("bugapp.BugApp")

libraryDependencies ++= {
  val akkaVersion         = "2.4.17"
  val akkaHttpVersion     = "10.0.6"
  val circeVersion        = "0.7.0"
  val scalaTestVersion    = "2.2.6"
  val scalaAsyncVersion   = "0.9.5"
  val scalaXmlVersion     = "1.0.4"
  val scalazVersion       = "7.2.5"
  Seq(
    "com.typesafe.akka"      %% "akka-actor"                        % akkaVersion,
    "com.typesafe.akka"      %% "akka-http-spray-json"              % akkaHttpVersion,
    "com.typesafe.akka"      %% "akka-http"                         % akkaHttpVersion,
    "com.typesafe.akka"      %% "akka-slf4j"                        % akkaVersion,
    "com.typesafe.akka"      %% "akka-stream"                       % akkaVersion,
    "com.typesafe.akka"      %% "akka-testkit"                      % akkaVersion,

    "com.enragedginger"      %% "akka-quartz-scheduler"             % "1.6.0-akka-2.4.x",

    "ch.qos.logback"          % "logback-classic"                   % "1.1.3",

    "de.heikoseeberger"      %% "akka-http-circe"                   % "1.12.0",

    "io.circe"               %% "circe-core"                        % circeVersion,
    "io.circe"               %% "circe-generic"                     % circeVersion,
    "io.circe"               %% "circe-parser"                      % circeVersion,

    "org.scalaz"             %% "scalaz-core"                       % scalazVersion,
    "org.scalaz"             %% "scalaz-concurrent"                 % scalazVersion,

    "org.apache.xmlgraphics"  % "fop"                               % "2.1"
      excludeAll ExclusionRule(organization = "org.apache.xmlgraphics", name = "batik-ext"),

    "net.sf.saxon"            % "Saxon-HE"                          % "9.7.0-8",

    "commons-codec"           % "commons-codec"                     % "1.10",

    "org.jfree"               % "jfreechart"                        % "1.0.19",

    "org.scala-lang.modules" %% "scala-async"                       % scalaAsyncVersion,
    "org.scala-lang.modules" %% "scala-xml"                         % scalaXmlVersion,
    "org.scalatest"          %% "scalatest"                         % scalaTestVersion     % "test",

    "junit"                   % "junit"                             % "4.12"               % "test",
    "org.powermock"           % "powermock-api-mockito"             % "1.6.6"              % "test",
    "org.powermock"           % "powermock-module-junit4"           % "1.6.6"              % "test",

    "com.sun.mail"            % "javax.mail"                        % "1.5.5"
  )
}

assemblyJarName in assembly := "bugs.jar"