package org.isa.linalg.distributed

import org.isa.annotation.Experimental
import org.isa.linalg.{Tensor, TensorEntry, UNFOLD_MODE, UnfoldModeEntry}
import org.apache.spark.rdd.RDD
import org.apache.spark.mllib.linalg.distributed.MatrixEntry

/**
 * :: Experimental ::
 * DistributedTensor class is a thre-dimensional structure, represented as RDD of TensorEntry
 * objects. Stored in compressed format.
 *
 */
@Experimental
class DistributedTensor(
    val entries:RDD[TensorEntry],
    private var nRows:Int,
    private var nCols:Int,
    private var nLayers:Int
) extends Tensor {
    /** flag to show whether dimensions are provided (sparse), or computed */
    private var recompute:Boolean = false

    /** Alternative constructor leaving tensor dimensions to be determined automatically. */
    def this(entries:RDD[TensorEntry]) = this(entries, 0, 0, 0)

    /** Computes size dynamicaly. */
    private def computeSize() {
        // once method is called we know, that dimensions are recomputed
        recompute = true

        val (imax, jmax, kmax) = entries.
            aggregate((0, 0, 0))(
                (U, V) => {
                    ( math.max(U._1, V.i), math.max(U._2, V.j), math.max(U._3, V.k) )
                },
                (U1, U2) => {
                    ( math.max(U1._1, U2._1), math.max(U1._2, U2._2), math.max(U1._3, U2._3) )
                }
            )
        // reassign dimensions
        nRows = 1 + imax
        nCols = 1 + jmax
        nLayers = 1 + kmax
    }

    /**
     * Gets number of rows.
     * @return number of rows
     */
    override def numRows(): Long = {
        if (nRows <= 0) {
            computeSize()
        }
        nRows
    }

    /**
     * Gets or computes number of columns.
     * @return number of columns
     */
    override def numCols(): Long = {
        if (nCols <= 0) {
            computeSize()
        }
        nCols
    }

    /**
     * Gets or computes number of layers.
     * @return number of layers
     */
    override def numLayers(): Long = {
        if (nLayers <= 0) {
            computeSize()
        }
        nLayers
    }

    /**
     * :: Experimental ::
     * Unfolds tensor for a mode and returns coordinate matrix.
     * Dense tensor will produce sparse coordinate matrix automatically.
     *
     * @param mode unfolding mode (A1, A2, A3)
     * @return Rich coordinate matrix
     */
    def unfold(mode:UnfoldModeEntry): RCoordinateMatrix = {
        val I:Int = numRows().toInt
        val J:Int = numCols().toInt
        val K:Int = numLayers().toInt

        mode match {
            case UNFOLD_MODE.A1 =>
                new RCoordinateMatrix(
                    entries.map(
                        entry => MatrixEntry(entry.i, entry.j + J * entry.k, entry.value)
                    ), I, K * J
                )
            case UNFOLD_MODE.A2 =>
                new RCoordinateMatrix(
                    entries.map(
                        entry => MatrixEntry(entry.j, entry.i + I * entry.k, entry.value)
                    ), J, I * K
                )
            case UNFOLD_MODE.A3 =>
                new RCoordinateMatrix(
                    entries.map(
                        entry => MatrixEntry(entry.k, entry.i + I * entry.j, entry.value)
                    ), K, J * I
                )
            case _ => throw new Exception("Unrecognized unfolding mode")
        }
    }

    /**
     * :: Experimental ::
     * Uncompresses tensor by dimensions (if possible) and returns itself with all the entries.
     * @return uncompressed tensor
     */
    def sparse(): DistributedTensor = {
        val Rows:Int = numRows().toInt
        val Cols:Int = numCols().toInt
        val Layers:Int = numLayers().toInt
        // if size was computed we return current tensor
        if (recompute) return this

        val sc = entries.sparkContext
        val component = sc.parallelize((0 until Rows), entries.partitions.length).
            flatMap { i => Iterator.tabulate(Cols)(j => (i, j)) }.
            flatMap { case (i, j) => Iterator.tabulate(Layers)(k => (i, j, k)) }.
            map { case (i, j, k) => ((i, j, k), TensorEntry(i, j, k, 0.0)) }

        val core = entries.map( entry => ( (entry.i, entry.j, entry.k), entry ) )

        val rdd = component.leftOuterJoin(core).map(
            pair => pair._2._2 match {
                case Some(s) => s
                case None => pair._2._1
            }
        )

        new DistributedTensor(rdd, Rows, Cols, Layers)
    }
}
