import org.scalatest._
import org.isa.linalg.{RIndexedRowMatrix, RCoordinateMatrix}
import org.apache.spark.rdd.RDD
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.mllib.linalg.distributed.{IndexedRow, IndexedRowMatrix}
import org.apache.log4j.Level


class RIndexedRowMatrixSpec extends UnitTestSpec with SparkLocalMode with BeforeAndAfter {

    var rdd:RDD[IndexedRow] = null

    before {
        setLoggingLevel(Level.ERROR)
        initSparkContext

        rdd = sc.parallelize(
            Array(
                IndexedRow(0, Vectors.dense( Array(0.2, -0.8, 0.4) )),
                IndexedRow(1, Vectors.dense( Array(0.5, 0.2, -0.8) )),
                IndexedRow(2, Vectors.dense( Array(0.8, -0.3, 0.4) ))
            )
        )
    }

    after {
        resetSparkContext
    }

    "Rich indexed matrix" can "use overload constructor" in {
        val cols = 3
        val rows = 4
        val localRDD = sc.parallelize(
            Array(
                IndexedRow(0, Vectors.dense( Array(0.1, 0.2, 0.3) )),
                IndexedRow(1, Vectors.dense( Array(0.4, 0.5, 0.6) )),
                IndexedRow(2, Vectors.dense( Array(0.7, 0.8, 0.9) )),
                IndexedRow(3, Vectors.dense( Array(1.0, 1.1, 1.2) ))
            )
        )

        val matrix = new RIndexedRowMatrix(localRDD)

        matrix.numCols() should equal (cols)
        matrix.numRows() should equal (rows)
    }

    it can "slice columns" in {
        val matrix = new RIndexedRowMatrix(rdd)

        intercept[IllegalArgumentException] {
            matrix.sliceColumns(-1, 2)
        }

        intercept[IllegalArgumentException] {
            matrix.sliceColumns(2, 1)
        }

        val sliced = matrix.sliceColumns(1, 2)
        sliced.rows.collect should equal (
            Array(
                IndexedRow(0, Vectors.dense( Array(-0.8) )),
                IndexedRow(1, Vectors.dense( Array(0.2) )),
                IndexedRow(2, Vectors.dense( Array(-0.3) ))
            )
        )
    }

    it can "add column" in {
        val matrix = new RIndexedRowMatrix(rdd)

        intercept[IllegalArgumentException] { matrix.addColumn(Array(0.1, 0.2)) }
        intercept[IllegalArgumentException] { matrix.addColumn(Array(0.1, 0.2, 0.3, 0.4)) }

        val added = matrix.addColumn(Array(0.1, 0.2, 0.3))

        added.rows.collect should equal (
            Array(
                IndexedRow(0, Vectors.dense( Array(0.2, -0.8, 0.4, 0.1) )),
                IndexedRow(1, Vectors.dense( Array(0.5, 0.2, -0.8, 0.2) )),
                IndexedRow(2, Vectors.dense( Array(0.8, -0.3, 0.4, 0.3) ))
            )
        )
        added.numCols() should be (4)
        added.numRows() should be (3)
    }

    it can "add row" in {
        val matrix = new RIndexedRowMatrix(rdd)

        intercept[IllegalArgumentException] { matrix.addColumn(Array(0.1, 0.2)) }
        intercept[IllegalArgumentException] { matrix.addColumn(Array(0.1, 0.2, 0.3, 0.4)) }

        val added = matrix.addRow(Array(0.1, 0.2, 0.3))

        added.rows.collect should equal (
            Array(
                IndexedRow(0, Vectors.dense( Array(0.2, -0.8, 0.4) )),
                IndexedRow(1, Vectors.dense( Array(0.5, 0.2, -0.8) )),
                IndexedRow(2, Vectors.dense( Array(0.8, -0.3, 0.4) )),
                IndexedRow(3, Vectors.dense( Array(0.1, 0.2, 0.3) ))
            )
        )
        added.numCols() should be (3)
        added.numRows() should be (4)
    }

    it can "revert columns sign" in {
        val matrix = new RIndexedRowMatrix(rdd)

        matrix.revertColumnSign(Array[Long]()) should equal (matrix)
        intercept[IllegalArgumentException] {
            matrix.revertColumnSign(Array(1L, 3L, 4L))
        }

        val negated = matrix.revertColumnSign(Array(1L, 2L))
        val sorted = negated.rows.map(
            entry => (entry.index, entry.vector.toArray.mkString(","))
        ).collect.sortBy(x => x._1)

        sorted should equal (
            Array(
                (0, Array(0.2, 0.8, -0.4).mkString(",") ),
                (1, Array(0.5, -0.2, 0.8).mkString(",") ),
                (2, Array(0.8, 0.3, -0.4).mkString(",") )
            )
        )
    }

    it can "be converted in IndexedRowMatrix" in {
        val matrix = new RIndexedRowMatrix(rdd)
        val indexed = matrix.toIndexedRowMatrix

        indexed.isInstanceOf[IndexedRowMatrix] should be (true)
    }

    it can "be converted in RCoordinateMatrix" in {
        val matrix = new RIndexedRowMatrix(rdd)
        val rcoordinate = matrix.toRCoordinateMatrix

        rcoordinate.isInstanceOf[RCoordinateMatrix] should be (true)
    }
}
