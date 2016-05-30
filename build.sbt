name := "bug-service"

version := "1.0"

scalaVersion := "2.11.8"

libraryDependencies ++= {
  val akkaVersion         = "2.4.5"
  val scalaTestVersion    = "2.2.6"
  val scalaXmlVersion     = "1.0.4"
  Seq(
    "org.scala-lang.modules" %% "scala-xml"                         % scalaXmlVersion,
    "com.typesafe.akka"      %% "akka-actor"                        % akkaVersion,
    "com.typesafe.akka"      %% "akka-http-spray-json-experimental" % akkaVersion,
    "com.typesafe.akka"      %% "akka-http-experimental"            % akkaVersion,
    "org.scalatest"          %% "scalatest"                         % scalaTestVersion     % "test"   
  )
}