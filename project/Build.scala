import sbt._
import Keys._

object HelloBuild extends Build {
    lazy val hello = project
        .in( file(".") )
        .dependsOn(bar % "test;compile", foo % "test->test;compile->compile")
        .aggregate(bar, foo)

    run in Compile <<= (run in Compile in foo)

    lazy val bar = project
        .in( file("bar") )

    lazy val foo = project
        .in( file("foo") )
        .dependsOn(bar)
}
