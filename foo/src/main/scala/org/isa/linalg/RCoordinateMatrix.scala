package org.isa.linalg

import org.apache.spark.annotation.Experimental
import org.apache.spark.rdd.RDD
import org.apache.spark.mllib.linalg.{Vectors, Vector, Matrix}
import org.apache.spark.mllib.linalg.distributed.{IndexedRowMatrix, IndexedRow, CoordinateMatrix, MatrixEntry}


/**
 * ::Experimental::
 * Base class for SVD result.
 * Returns left and right singular vectors, and singular values
 */
@Experimental
case class SVD(val U:IndexedRowMatrix, val s:Vector, val V:Matrix)


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
                val u = new IndexedRowMatrix(
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
                SVD(svd.U, svd.s, svd.V)
        }
    }
}
