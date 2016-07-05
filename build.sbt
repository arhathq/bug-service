name := "bug-service"

version := "1.0"

scalaVersion := "2.11.8"

libraryDependencies ++= {
  val akkaVersion         = "2.4.5"
  val circeVersion        = "0.4.1"
  val scalaTestVersion    = "2.2.6"
  val scalaXmlVersion     = "1.0.4"
  Seq(
    "com.typesafe.akka"      %% "akka-actor"                        % akkaVersion,
    "com.typesafe.akka"      %% "akka-http-spray-json-experimental" % akkaVersion,
    "com.typesafe.akka"      %% "akka-http-experimental"            % akkaVersion,

    "de.heikoseeberger"      %% "akka-http-circe"                   % "1.6.0",

    "io.circe"               %% "circe-core"                        % circeVersion,
    "io.circe"               %% "circe-generic"                     % circeVersion,
    "io.circe"               %% "circe-parser"                      % circeVersion,

    "org.apache.xmlgraphics"  % "fop"                               % "2.1",

    "org.jfree"               % "jfreechart"                        % "1.0.19",

    "org.scala-lang.modules" %% "scala-xml"                         % scalaXmlVersion,
    "org.scalatest"          %% "scalatest"                         % scalaTestVersion     % "test"   
  )
}



