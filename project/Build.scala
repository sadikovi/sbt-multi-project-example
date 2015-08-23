import sbt._
import Keys._


object HelloBuild extends Build {
    lazy val commonSettings = Seq(
        organization := "com.wyn",
        scalaVersion := "2.10.4",
        libraryDependencies ++= Seq(
        	"org.scalatest" % "scalatest_2.10" % "2.2.4" % "test"
        )
    )

    lazy val testSettings = Seq(
        parallelExecution in Test := false
    )

    lazy val scaladocSettings = Defaults.defaultSettings ++ Seq(
        scalacOptions in (Compile, doc) ++=
            Seq("-groups", "-implicits", "-deprecation", "-unchecked")
    )

    lazy val bar = project.
        in( file("bar") ).
        disablePlugins(sbtassembly.AssemblyPlugin).
        settings(commonSettings: _*).
        settings(testSettings: _*).
        settings(scaladocSettings: _*)

    lazy val foo = project.
        in( file("foo") ).
        disablePlugins(sbtassembly.AssemblyPlugin).
        dependsOn(bar).
        aggregate(bar).
        settings(commonSettings: _*).
        settings(testSettings: _*).
        settings(scaladocSettings: _*).
        settings(
            aggregate in Test := false,
            unmanagedResourceDirectories in Compile ++= Seq(
                baseDirectory.value / "config"
            ),
            libraryDependencies ++= Seq(
            	"org.apache.spark" %% "spark-core" % "1.4.0" % "provided",
                "org.apache.spark" % "spark-mllib_2.10" % "1.4.0" % "provided",
                "org.jblas" % "jblas" % "1.2.4"
            )
        )

    lazy val hello = project.
        in( file(".") ).
        dependsOn(bar, foo).
        aggregate(bar, foo).
        settings(commonSettings: _*).
        settings(testSettings: _*).
        settings(scaladocSettings: _*)
}
