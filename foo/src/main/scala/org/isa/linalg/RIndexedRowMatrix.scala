package org.isa.linalg

import org.apache.spark.annotation.Experimental
import org.apache.spark.rdd.RDD
import org.apache.spark.mllib.linalg.{Vectors, Vector}
import org.apache.spark.mllib.linalg.distributed.{IndexedRow, IndexedRowMatrix, MatrixEntry}


/**
 * Rich IndexedRowMatrix class.
 * Supports additional matrix operations.
 *
 */
@Experimental
class RIndexedRowMatrix(
    rows:RDD[IndexedRow],
    nRows:Long,
    nCols:Int
) extends IndexedRowMatrix(rows, nRows, nCols) {

    /** Alternative constructor leaving matrix dimensions to be determined automatically. */
    def this(rows: RDD[IndexedRow]) = this(rows, 0L, 0)

    /**
     * Slices columns and returns matrix with columns that are in interval selected
     * by `from` and `to`-1, e.g. slice 2:6 will return indices 0, 1, [2, 3, 4, 5], 6.
     * Uses zero-based index for positions.
     *
     * @param from start index of the truncated interval (inclusive)
     * @param to end index of the truncated interval (exclusive)
     * @return new matrix with columns `from` to `to`-1
     */
    def sliceColumns(from:Int, to:Int): RIndexedRowMatrix = {
        require(from >= 0 && from < to, "Unsupported indices " + from + ": " + to)
        // if requested length is greater or equal to numCols, return itself
        if (to - from >= numCols()) return this

        val updated:RDD[IndexedRow] = rows.map(
            row => { IndexedRow(row.index, Vectors.dense(row.vector.toArray.slice(from, to))) }
        )
        new RIndexedRowMatrix(updated)
    }

    /**
     * Adds column to the matrix.
     * Number of rows in both matrices should match, otherwise will throw an Exception.
     *
     * @param column array with values to merge
     * @return new extended matrix
     */
    def addColumn(column:Array[Double]): RIndexedRowMatrix = {
        val arows = column.length
        val mrows = numRows()
        val mcols = numCols()
        require(mrows < Int.MaxValue, "Number of rows in matrix exceeds Int range")
        require(arows == mrows, "Number of rows does not match: " + arows + " != "  + mrows)

        val updated:RDD[IndexedRow] = rows.map(
            row => {
                val index:Long = row.index
                val array = row.vector.toArray :+ column(index.toInt)
                IndexedRow(index, Vectors.dense(array))
            }
        )
        new RIndexedRowMatrix(updated, mrows, mcols.toInt + 1)
    }

    /**
     * Adds column vector to the matrix.
     * Number of rows in both matrixes should match, otherwise will throw an Exception.
     *
     * @param column vector to merge
     * @return new extended matrix
     */
    def addColumn(column:Vector): RIndexedRowMatrix = addColumn(column.toArray)

    /**
     * Appends row to the matrix.
     * Number of elements should match one in matrix
     *
     * @param matrix IndexedRowMatrix matrix
     * @param row array of Double as row
     * @return extended matrix
     */
    def addRow(row:Array[Double]): RIndexedRowMatrix = addRow(Vectors.dense(row))

    /**
     * Appends row to the matrix.
     * Number of elements should match one in matrix
     *
     * @param row Vector object to append
     * @return extended matrix
     */
    def addRow(row:Vector): RIndexedRowMatrix = {
        val elems = row.size
        val mcols = numCols()
        val mrows = numRows()
        require(elems == mcols, "Number of columns does not match: " + elems + " != " + mcols)

        val sc = rows.sparkContext
        val newRow = sc.parallelize( Array(new IndexedRow(mrows, row)) )
        new RIndexedRowMatrix(rows.union(newRow), mrows + 1L, mcols.toInt)
    }

    /**
     * Reverts sign of a column for indexes specified.
     * Indices must be within vector of IndexedRow of the matrix.
     *
     * @param indices unique indices of columns whose sign must be changed
     * @return new matrix
     */
    def revertColumnSign(indices:Array[Long]): RIndexedRowMatrix = {
        if (indices.isEmpty) return this

        val set = indices.toSet
        val mcols = numCols()
        val mrows = numRows()
        require(set.max < mcols, "Index " + set.max + " is greater than number of columns " + mcols)

        // revert sign for Coordinate matrix entries
        val entries = toCoordinateMatrix.entries
        val updated = new RCoordinateMatrix(
            entries.map( elem => set.contains(elem.j) match {
                case true => MatrixEntry(elem.i, elem.j, -elem.value)
                case false => elem
            }),
            mrows,
            mcols
        )
        // return updated matrix
        updated.toRIndexedRowMatrix
    }

    /**
     * Returns IndexedRowMatrix instance.
     * Though it is not recommended, as RIndexedRowMatrix is already a subset with all methods.
     *
     * @return IndexedRowMatrix instance
     */
    def toIndexedRowMatrix: IndexedRowMatrix = this.asInstanceOf[IndexedRowMatrix]

    /**
     * Returns instance of RCoordinateMatrix.
     *
     * @return RCoordinateMatrix instance
     */
    def toRCoordinateMatrix: RCoordinateMatrix = {
        val matrix = toCoordinateMatrix
        new RCoordinateMatrix(matrix.entries, matrix.numRows(), matrix.numCols())
    }
}
