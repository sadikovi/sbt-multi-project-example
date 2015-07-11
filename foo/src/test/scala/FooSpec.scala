import org.scalatest._
import org.hello.foo._
import scala.io.Source


class FooSpec extends UnitTestSpec {
    "A Simple object" should "return right step" in {
        val step = 10

        val custom = new Simple(step)

        custom.showStep should equal (step)
    }

    it should "read steps from text file" in {
        val filepath = getClass.getResource("/iterations.txt")

        Source.fromURL(filepath).getLines.foreach(
            x => {
                val newObject = new Simple(x.toInt)

                newObject.showStep should equal (x.toInt)
            }
        )
    }

    it should "check time step" in {
        val step = 10

        val custom = new Simple(step)

        assert(custom.timeStep > step)
    }

    it should "trigger Jenkins job" in {
        true should be (true)
    }

}
