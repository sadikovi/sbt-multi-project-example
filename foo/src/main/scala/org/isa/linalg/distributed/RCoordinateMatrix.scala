package org.isa.linalg.distributed

import org.isa.annotation.Experimental
import org.isa.linalg.{Matrix => MatrixFactory}
import org.apache.spark.rdd.RDD
import org.apache.spark.mllib.linalg.{Vectors, Vector, Matrix}
import org.apache.spark.mllib.linalg.distributed.{IndexedRowMatrix, IndexedRow, CoordinateMatrix, MatrixEntry}
import org.jblas.DoubleMatrix

/**
 * :: Experimental ::
 * Base class for SVD result.
 * Returns left and right singular vectors, and singular values
 */
@Experimental
case class SVD(val U:RIndexedRowMatrix, val s:Vector, val V:Matrix)


@Experimental
case class Lanczos(val A:RIndexedRowMatrix, val s:Vector)

private[distributed] case class EOrder(val name:String)

object EORDER_MODE {
    val Ascend = EOrder("ascend")
    val Descend = EOrder("descend")
}


/**
 * Rich CoordinateMatrix class.
 * Supports additional matrix operations.
 *
 */
@Experimental
class RCoordinateMatrix(
    entries:RDD[MatrixEntry],
    nRows:Long,
    nCols:Long
) extends CoordinateMatrix(entries, nRows, nCols) {

    /** Alternative constructor leaving matrix dimensions to be determined automatically. */
    def this(entries: RDD[MatrixEntry]) = this(entries, 0L, 0L)


    /**
     * Converts to RIndexedRowMatrix instance.
     *
     * @return RIndexedRowMatrix instance
     */
    def toRIndexedRowMatrix: RIndexedRowMatrix = {
        val matrix = toIndexedRowMatrix
        new RIndexedRowMatrix(matrix.rows, matrix.numRows(), matrix.numCols().toInt)
    }

    /**
     * Converts to CoordinateMatrix instance.
     *
     * @return CoordinateMatrix instance
     */
    def toCoordinateMatrix: CoordinateMatrix = this.asInstanceOf[CoordinateMatrix]

    /**
     * Converts matrix to local DoubleMatrix.
     * @return DoubleMatrix instance
     */
    def toLocalMatrix: DoubleMatrix = {
        val indexed = toRIndexedRowMatrix
        indexed.toLocalMatrix
    }

    /**
     * :: Experimental ::
     * Computes SVD for a coordinate matrix.
     * Automatically transposes for efficiency.
     *
     * @param k number of singular values to keep
     * @param rCond the reciprocal condition number
     * @return SVD object with UEV^T
     */
    @Experimental
    def computeSVD(k:Int, rCond:Double=1.0E-7): SVD = {
        // tranpose matrix if cols > rows
        val transposed = (numCols() > numRows())
        val cmatrix:CoordinateMatrix = transposed match {
            case true => transpose()
            case false => this
        }

        val svd = cmatrix.toIndexedRowMatrix.computeSVD(
            k,
            computeU=(!transposed),
            rCond
        )

        // return result based on `transposed` property
        transposed match {
            case true =>
                // extract Spark context
                val sc = cmatrix.entries.sparkContext
                val numCols = svd.V.numCols
                // ... and convert into indexed row matrix
                val u = new RIndexedRowMatrix(
                    sc.parallelize(
                        svd.V.transpose.toArray.
                            sliding(numCols, numCols).toArray.zipWithIndex.
                            map(
                                entry => IndexedRow(entry._2, Vectors.dense(entry._1))
                            )
                    )
                )
                SVD(u, svd.s, null)
            case false =>
                val nRows = svd.U.numRows()
                val nCols = svd.U.numCols().toInt
                SVD(new RIndexedRowMatrix(svd.U.rows, nRows, nCols), svd.s, svd.V)
        }
    }

    def subtract(another:RCoordinateMatrix): RCoordinateMatrix = {
        val m = another.numRows()
        val n = another.numCols()

        require(m == numRows() && n == numCols(), "Cannot subtract. Matrices have different size")

        val a = this.entries.map(entry =>((entry.i, entry.j), entry.value))
        val b = another.entries.map(entry =>((entry.i, entry.j), -entry.value))

        val elements = a.union(b).
            aggregateByKey(0.0)((U, V) => U + V, (U1, U2) => U1 + U2).
            map(elem => MatrixEntry(elem._1._1, elem._1._2, elem._2))
        new RCoordinateMatrix(elements, m, n)
    }

    private def lanczos(k:Int, eorder:EOrder=EORDER_MODE.Descend): Lanczos = {
        val m = numRows().toInt
        val n = numCols().toInt

        require(m < 1E4, "Lanczos algorithm is only applicable for size less than 1E4")
        require(m == n, "This eigen decomposition can work only for symmetric matrices")

        val sc = entries.sparkContext

        val A = eorder match {
            case EORDER_MODE.Ascend => MatrixFactory.eye(sc)(m, n).subtract(this)
            case EORDER_MODE.Descend => this
        }

        val V = MatrixFactory.zeros(sc)(n, k)

        throw new Exception("Not implemented")

        return null
    }
}
