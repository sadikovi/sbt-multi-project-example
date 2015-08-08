import org.scalatest._
import org.apache.log4j.Level
import scala.util.Random


class SparkSpec extends UnitTestSpec with SparkLocalMode with BeforeAndAfter {

    before {
        setLoggingLevel(Level.ERROR)
        initSparkContext
    }

    after {
        resetSparkContext
    }

    "A Spark" should "reduce RDD" in {
        val n = 10
        val vector = for (i <- 0 until n) yield Random.nextInt(n)

        val randoms = sc.parallelize(vector)

        randoms.reduce(_+_) should equal (vector.sum)
    }

    it should "map RDD" in {
        val lines = Array("simple", "lines", "for", "Spark")

        val lined = sc.parallelize(lines).
            map(_.length)

        lined.reduce(_+_) should equal (lines.map(_.length).sum)
    }
}
