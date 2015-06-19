# create build.sbt
cat > build.sbt <<EOL
name := "demo-sbt-app"

version := "0.0.1"

scalaVersion := "2.10.4"

// library dependencies for spark
/*
libraryDependencies ++= Seq(
    "org.apache.spark" %% "spark-core" % "1.2.0" % "provided",
    "org.apache.spark" %% "spark-streaming" % "1.2.0"
)
*/

libraryDependencies ++= Seq(
	"org.scalatest" % "scalatest_2.10" % "2.2.4" % "test"
)
EOL

# build directory
echo "[lib] - for .jar files and internal libraries"
mkdir lib
echo "[out] - for any output data, such as files and etc"
mkdir out
echo "[resource] - for any internal resources, such as files"
mkdir resource
echo "[src] - for .java or .scala project files"
echo "structure: src -> main -> { java, scala -> {Demo.scala} }"
mkdir src
cd ./src
mkdir main
cd ./main
mkdir java
mkdir scala
cd ./scala

cat > Demo.scala <<EOL
object Demo {
    def main(args: Array[String]) = println("Demo file!")
}
EOL

# go to source directory
cd ../../
echo "[test] - for tests, includes java/, scala/, resources/"
mkdir test
cd test
mkdir java
mkdir scala
mkdir resources
cd scala

# creating abstract unit test class
cat > UnitTestSpec.scala <<EOL
import org.scalatest._
// abstract general testing class
abstract class UnitTestSpec extends FlatSpec with Matchers with OptionValues with Inside with Inspectors
EOL

# creating simple unit test
cat > ExampleSpec.scala <<EOL
import collection.mutable.Stack
import org.scalatest._

class StackSpec extends UnitTestSpec {
  "A Stack" should "pop values in last-in-first-out order" in {
		val stack = new Stack[Int]
		stack.push(1)
		stack.push(2)
		assert(stack.pop() === 2)
		assert(stack.pop() === 1)
  }
}
EOL

# go to parent directory
cd ../../../
# and run sbt
echo "...Add new file in src/main/scala or ./java respectively"
echo "...To run project >sbt run"
echo "...To package project in jar >sbt package"
echo "...Recommended to run >sbt clean package"
echo "Compile, test and run Demo.scala> sbt clean compile"
sbt clean compile test run
