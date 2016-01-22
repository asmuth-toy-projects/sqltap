import AssemblyKeys._

name := "SQLTap"

organization := "com.paulasmuth"

version := "0.8.1"

mainClass in (Compile, run) := Some("com.paulasmuth.sqltap.SQLTap")

scalaVersion := "2.11.7"

assemblySettings

jarName in assembly := { s"${name.value.toLowerCase}-${version.value}.jar" }

fork in run := true

libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "1.0.2"

libraryDependencies += "org.scalatest" % "scalatest_2.11" % "2.2.4" % "test"
