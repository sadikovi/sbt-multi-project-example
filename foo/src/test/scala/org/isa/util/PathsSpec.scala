import org.scalatest._
import org.isa.util._
import java.io.File


class PathsSpec extends UnitTestSpec {
    "Paths" should "create local path" in {
        val paths = Seq(
            ("/home/temp/*.txt", true),
            ("home/temp/*.txt", true),
            ("file://home/temp/*.txt", true),
            ("file:///home/temp/*.txt", true),
            ("file:///home/temp/*.txt/", true),
            ("home/ano:ther/*.csv", false),
            ("file:/home/temp", false),
            ("//home/temp", false),
            ("home/{} path", false),
            ("file://home/file://test", false)
        )

        paths.foreach(
            path => path match {
                case (a:String, true) =>
                    val dir = new LocalPath(a)
                    dir.local should equal (a.stripPrefix("file://").stripSuffix("/"))
                case (b:String, false) =>
                    intercept[IllegalArgumentException] {
                        new LocalPath(b)
                    }
                case _ =>
                    throw new Exception("Wrong argument for test")
            }
        )
    }

    it should "create Hadoop path" in {
        val paths = Seq(
            ("hdfs://host:50700/home/temp/*.csv", true),
            ("hdfs://anotherhost:80/home/temp/*.csv", true),
            ("hdfs://anotherhost:80/home/temp/*.csv/", true),
            ("hdfs://anotherhost:9/home/temp/*.csv", false),
            ("hdfs:/anotherhost:50700/home/temp/*.csv", false),
            ("file://host:50700/home/temp/*.csv", false),
            ("/home/temp/*.csv", false)
        )

        paths.foreach(
            path => path match {
                case (a:String, true) =>
                    val dir = new HadoopPath(a)
                    dir.uri should equal (a.stripSuffix("/"))
                case (b:String, false) =>
                    intercept[IllegalArgumentException] {
                        new HadoopPath(b)
                    }
                case _ =>
                    throw new Exception("Wrong argument for test")
            }
        )
    }

    it should "correctly identify file system" in {
        val paths = Seq(
            ("/home/temp/*.txt", true),
            ("home/temp/*.txt", true),
            ("file://home/temp/*.txt", true),
            ("hdfs://host:50700/home/temp/*.csv", false)
        )

        paths.foreach(
            path => {
                val dir = Paths.fromString(path._1)
                dir.isLocalFS should be (path._2)
            }
        )
    }

    it should "return correct absolute path" in {
        val paths = Seq(
            ("/home/temp/*.txt", "/home/temp/*.txt"),
            ("home/temp/*.txt", new File("home/temp/*.txt").getAbsolutePath),
            ("file://home/temp/*.txt", new File("home/temp/*.txt").getAbsolutePath),
            ("file:///home/temp/*.txt", "/home/temp/*.txt"),
            ("file:///home/temp/*.txt/", "/home/temp/*.txt"),
            ("hdfs://host:50700/home/temp/*.csv", "/home/temp/*.csv"),
            ("hdfs://host:50700/home/temp/*.csv/", "/home/temp/*.csv")
        )

        paths.foreach(
            path => {
                Paths.fromString(path._1).absolute should equal (path._2)
            }
        )
    }

    it should "return correct local path" in {
        val paths = Seq(
            ("/home/temp/*.txt", "/home/temp/*.txt"),
            ("home/temp/*.txt", "home/temp/*.txt"),
            ("file://home/temp/*.txt", "home/temp/*.txt"),
            ("file:///home/temp/*.txt", "/home/temp/*.txt"),
            ("file:///home/temp/*.txt/", "/home/temp/*.txt"),
            ("hdfs://host:50700/home/temp/*.csv", "/home/temp/*.csv"),
            ("hdfs://host:50700/home/temp/*.csv/", "/home/temp/*.csv")
        )

        paths.foreach(
            path => {
                Paths.fromString(path._1).local should equal (path._2)
            }
        )
    }

    it should "return correct uri" in {
        val paths = Seq(
            ("/home/temp/*.txt", "file:///home/temp/*.txt"),
            ("home/temp/*.txt", "file://" + new File("home/temp/*.txt").getAbsolutePath),
            ("file://home/temp/*.txt", "file://" + new File("home/temp/*.txt").getAbsolutePath),
            ("file:///home/temp/*.txt", "file:///home/temp/*.txt"),
            ("file:///home/temp/*.txt/", "file:///home/temp/*.txt"),
            ("hdfs://host:50700/home/temp/*.csv", "hdfs://host:50700/home/temp/*.csv"),
            ("hdfs://host:50700/home/temp/*.csv/", "hdfs://host:50700/home/temp/*.csv")
        )

        paths.foreach(
            path => {
                Paths.fromString(path._1).uri should equal (path._2)
            }
        )
    }
}
