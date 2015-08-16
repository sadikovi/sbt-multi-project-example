import org.scalatest._
import org.isa.linalg.{RIndexedRowMatrix, RCoordinateMatrix}
import org.apache.log4j.Level
import org.apache.spark.rdd.RDD
import org.apache.spark.mllib.linalg.distributed.{CoordinateMatrix, MatrixEntry}


class RCoordinateMatrixSpec extends UnitTestSpec with SparkLocalMode with BeforeAndAfter {

    var rdd:RDD[MatrixEntry] = null

    before {
        setLoggingLevel(Level.ERROR)
        initSparkContext

        rdd = sc.parallelize(
            Array(
                MatrixEntry(0, 0, 0.2),
                MatrixEntry(0, 1, -0.8),
                MatrixEntry(0, 2, 0.4),
                MatrixEntry(1, 0, 0.5),
                MatrixEntry(1, 1, 0.2),
                MatrixEntry(1, 2, -0.8),
                MatrixEntry(2, 0, 0.8),
                MatrixEntry(2, 1, -0.3),
                MatrixEntry(2, 2, 0.4)
            )
        )
    }

    after {
        resetSparkContext
    }

    "Rich coordinate matrix" can "use overload constructor" in {
        val cols = 3
        val rows = 3

        val matrix = new RCoordinateMatrix(rdd)

        matrix.numCols() should equal (cols)
        matrix.numRows() should equal (rows)
    }

    ignore can "compute SVD" in {
        // def computeSVD(k:Int, rCond:Double=1.0E-7): SVD
    }

    it can "be converted in CoordinateMatrix" in {
        val matrix = new RCoordinateMatrix(rdd)

        val coordinateMatrix = matrix.toCoordinateMatrix
        coordinateMatrix.isInstanceOf[CoordinateMatrix] should be (true)
    }

    it can "be converted in RIndexedRowMatrix" in {
        val matrix = new RCoordinateMatrix(rdd)

        val indexedMatrix = matrix.toRIndexedRowMatrix
        indexedMatrix.isInstanceOf[RIndexedRowMatrix] should be (true)
    }
}
