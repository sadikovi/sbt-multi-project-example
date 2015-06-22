import org.scalatest._

import org.apache.spark._
import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.log4j.Logger
import org.apache.log4j.Level


class SparkSpec extends UnitTestSpec with BeforeAndAfter {

    val master = "local-cluster[2, 1, 512]"
    val appName = this.getClass.getSimpleName
    val envPair = Map(
        "spark.master" -> "",
        "spark.executorEnv.SPARK_HOME" -> ".",
        "spark.testing" -> "true"
    )

    /**
     * You have to set SPARK_HOME manually for Executor and method fetchAndRun
     * You also have to set SPARK_PREPEND_CLASSES as test classes of your app
     * do not forget to set class path for executors
     * Both paths must be full paths to the files.
     * E.g.
     * export SPARK_PREPEND_CLASSES=/Users/sadikovi/Developer/hello/foo/target/scala-2.10/test-classes
     * export SPARK_HOME=/Users/sadikovi/Developer/spark-1.3.1-bin-hadoop2.4
     */

    val conf = new SparkConf().setMaster(master).setAppName(appName).
        set("spark.executor.extraClassPath", "/Users/sadikovi/Developer/hello/foo/target/scala-2.10/test-classes").
        set("spark.deploy.mode", "cluster")

    Logger.getLogger("org").setLevel(Level.ERROR)
    Logger.getLogger("akka").setLevel(Level.ERROR)

    var sc: SparkContext = null

    after {
        if (sc != null) {
            sc.stop()
            sc = null
        }
    }

    "A SparkSpec" should "run in local mode" in {
        sc = new SparkContext(conf)

        val created = sc.parallelize(Array(1, 2, 3))
        val filtered = created.filter(_ > 1)
        filtered.coalesce(1, true).saveAsTextFile("out")
    }
}
