import collection.mutable.Stack
import org.scalatest._

class BarSpec extends UnitTestSpec {
  "A Bar" should "execute test" in {
		val a = Array(1, 2, 3)
		a.length should be (3)
  }
}
