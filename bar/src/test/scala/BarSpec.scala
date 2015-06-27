import collection.mutable.Stack
import org.scalatest._
import scala.util.Random


class BarSpec extends UnitTestSpec {
    val expression = false

    def sample = Random.nextInt(100)

    "A Bar" should "execute test" in {
        val a = Array(1, 2, 3)
        a.length should be (3)
    }

    ignore should "ignore test" in {
        val a = Array(1, 2, 3)
        a.sum should be (6)
    }

    it should "generate number" in {
        sample should be >= (0)
    }

    if (expression) {
        it should "do something" in {
            true should be (true)
        }
    } else {
        ignore should "support only Linux" in {}
    }
}
