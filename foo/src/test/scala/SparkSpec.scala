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

class SparkClusterSpec extends UnitTestSpec with SparkLocalClusterMode with BeforeAndAfter {

    before {
        setLoggingLevel(Level.ERROR)
        initSparkContext
    }

    after {
        resetSparkContext
    }

    "A Spark" should "reduce RDD in cluster" in {
        val n = 10
        val vector = for (i <- 0 until n) yield Random.nextInt(n)

        val randoms = sc.parallelize(vector)

        randoms.reduce(_+_) should equal (vector.sum)
    }

    it should "parse big array in cluster" in {
        val n = 1000000
        val array = (0 to n).toArray

        val rdd = sc.parallelize(array)

        rdd.cache

        rdd.reduce(_+_) should equal (array.sum)
        rdd.max should equal (n)
    }
}
