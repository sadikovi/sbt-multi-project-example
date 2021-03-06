import org.scalatest._
import org.apache.spark._
import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.log4j.Logger
import org.apache.log4j.Level
import java.io.File

// abstract general testing class
abstract class UnitTestSpec extends FlatSpec with Matchers with OptionValues with Inside with Inspectors

/** General local Spark context */
trait LocalSparkContext {
    @transient private var _sc:SparkContext = null

    /**
     * Start (or init) Spark context.
     *
     * @param conf Spark configuration
     */
    def start(conf:SparkConf) {
        this._sc = new SparkContext(conf)
    }

    /**
     * Stop Spark context.
     *
     * @param sc Spark context
     */
    def stop(sc:SparkContext) {
        if (sc != null) {
            sc.stop
        }
    }

    /**
     * Init Spark context.
     * Needs to be implemented in all sub-traits.
     *
     */
    def initSparkContext

    /**
     * Reset Spark context.
     *
     */
    def resetSparkContext {
        this stop _sc
        _sc = null
    }

    /**
     * Set logging level globally for all.
     * Supported log levels:
     *      Level.OFF
     *      Level.ERROR
     *      Level.WARN
     *      Level.INFO
     *
     * @param level logging level
     */
    def setLoggingLevel(level:Level) {
        Logger.getLogger("org").setLevel(level)
        Logger.getLogger("akka").setLevel(level)
        Logger.getRootLogger().setLevel(level)
    }


    /**
     * Return spark context.
     *
     * @return spark context
     */
    def sc:SparkContext = this._sc
}

/** General Local mode, can be extended */
trait SparkLocalMode extends LocalSparkContext {

    /**
     * Init local mode.
     *
     */
    override def initSparkContext {
        val conf = new SparkConf().
            setMaster("local[2]").
            setAppName("spark-local-mode-test").
            set("spark.driver.memory", "1g").
            set("spark.executor.memory", "1g")

        this start conf
    }
}

/** General local cluster mode, can be extended */
trait SparkLocalClusterMode extends LocalSparkContext {
    // throw exception if it is not Linux or Mac
    System.getProperty("os.name") match {
        case mac if mac.toLowerCase.startsWith("mac") => true
        case linux if linux.toLowerCase.startsWith("linux") => true
        case _ => throw new UnsupportedOperationException(
            "Local cluster mode runs only on Unix-like systems"
        )
    }

    // throw exception if SPARK_HOME is not set
    val SPARK_HOME = System.getenv("SPARK_HOME") match {
        case nonEmpty if nonEmpty != null => nonEmpty
        case _ => throw new UnsupportedOperationException(
            "Local cluster mode requires SPARK_HOME to be set"
        )
    }

    // throw exception if spark-path-resolver is not found
    val SPARK_PREPEND_CLASSES = Seq(
        getClass.getResource("/spark-main-path-resolver").getPath.split("/").dropRight(1).mkString(File.separator),
        getClass.getResource("/spark-test-path-resolver").getPath.split("/").dropRight(1).mkString(File.separator)
    ).mkString(File.pathSeparator)

    /**
     * Init local cluster mode.
     *
     */
    override def initSparkContext {
        val conf = new SparkConf().
            setMaster("local-cluster[2, 1, 1024]").
            setAppName("spark-local-cluster-mode-test").
            set("spark.driver.memory", "1g").
            set("spark.executor.memory", "1g").
            set("spark.testing", "true").
            set("spark.executorEnv.SPARK_HOME", SPARK_HOME).
            set("spark.executor.extraClassPath", SPARK_PREPEND_CLASSES)

        this start conf
    }
}
