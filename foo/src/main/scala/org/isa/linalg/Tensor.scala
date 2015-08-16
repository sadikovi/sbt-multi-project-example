package org.isa.linalg

import org.apache.spark.rdd.RDD
import org.apache.spark.mllib.linalg.distributed.MatrixEntry

/** unfolding mode and classes */
private[linalg] case class UnfoldModeEntry(name:String)

/** Unfolding mode. Use that to specify A1, A2, or A3 unfoldings */
object UNFOLD_MODE {
    val A1 = new UnfoldModeEntry("A1")
    val A2 = new UnfoldModeEntry("A2")
    val A3 = new UnfoldModeEntry("A3")
}

// entry for tensor core
case class TensorEntry(i:Int, j:Int, k:Int, value:Double)

/**
 * Tensor class is a thre-dimensional structure, represented as RDD of TensorEntry objects.
 * Stored in compressed format.
 */
class Tensor(
    val entries:RDD[TensorEntry],
    private var nRows:Int,
    private var nCols:Int,
    private var nLayers:Int
) extends DistributedTensor {
    /** flag to show whether dimensions are provided (sparse), or computed */
    private var recompute:Boolean = false

    /** Alternative constructor leaving tensor dimensions to be determined automatically. */
    def this(entries:RDD[TensorEntry]) = this(entries, 0, 0, 0)

    /** Computes size dynamicaly. */
    private def computeSize() {
        // once method is called we know, that dimensions are recomputed
        recompute = true

        val (imax, jmax, kmax) = entries.
            map(
                entry => (entry.i, entry.j, entry.k)
            ).
            reduce(
                (prev, next) => {
                    (
                        math.max(prev._1, next._1),
                        math.max(prev._2, next._2),
                        math.max(prev._3, next._3)
                    )
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
     * Uncompresses tensor by dimensions (if possible) and returns itself with all the entries.
     * @return uncompressed tensor
     */
    def sparse(): Tensor = {
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

        new Tensor(rdd, Rows, Cols, Layers)
    }
}
