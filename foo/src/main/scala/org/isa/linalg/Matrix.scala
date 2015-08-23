package org.isa.linalg

import org.isa.annotation.Experimental
import org.isa.linalg.distributed.RCoordinateMatrix

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import org.apache.spark.mllib.linalg.distributed.MatrixEntry

/**
 * :: Experimental ::
 * Represents a 3rd-order tensor local/backed by an RDD.
 */
@Experimental
class Matrix

object Matrix {
    def eye(sc:SparkContext)(rows:Int, cols:Int): RCoordinateMatrix = {
        require(rows > 0 && cols > 0, "Indices must be positive integers")

        val arr:Array[MatrixEntry] = (
            Iterator.tabulate(math.min(rows, cols))(i => MatrixEntry(i, i, 1.0))
        ).toArray
        val entries:RDD[MatrixEntry] = sc.parallelize(arr)
        new RCoordinateMatrix(entries, rows, cols)
    }

    def zeros(sc:SparkContext)(rows:Int, cols:Int): RCoordinateMatrix = {
        require(rows > 0 && cols > 0, "Indices must be positive integers")

        val entries:RDD[MatrixEntry] = sc.parallelize( Array(MatrixEntry(0, 0, 0.0)) )
        new RCoordinateMatrix(entries, rows, cols)
    }
}
