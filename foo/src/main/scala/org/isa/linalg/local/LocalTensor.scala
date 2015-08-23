package org.isa.linalg.local

import org.isa.annotation.Experimental
import org.isa.linalg.{Tensor, TensorEntry, UNFOLD_MODE, UnfoldModeEntry}
import org.apache.spark.mllib.linalg.distributed.MatrixEntry
import scala.collection.mutable.ArrayBuffer
import org.jblas.DoubleMatrix

/**
 * :: Experimental ::
 * Represents a local 3rd-order tensor.
 */
@Experimental
class LocalTensor(
    entries:Array[TensorEntry],
    private var nRows:Int,
    private var nCols:Int,
    private var nLayers:Int
) extends Tensor {
    /** flag to show whether dimensions are provided (sparse), or computed */
    private var recompute:Boolean = false

    def this(entries:Array[TensorEntry]) = this(entries, 0, 0, 0)

    def entries(): Array[TensorEntry] = entries

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

    override def numRows(): Long = {
        if (nRows <= 0) {
            computeSize()
        }
        nRows
    }

    override def numCols(): Long = {
        if (nCols <= 0) {
            computeSize()
        }
        nCols
    }

    override def numLayers(): Long = {
        if (nLayers <= 0) {
            computeSize()
        }
        nLayers
    }

    /**
     * Unfolds tensor for a mode and returns double matrix.
     * @param mode unfolding mode (A1, A2, A3)
     * @return Rich coordinate matrix
     */
    def unfold(mode:UnfoldModeEntry): DoubleMatrix = {
        val I:Int = numRows().toInt
        val J:Int = numCols().toInt
        val K:Int = numLayers().toInt

        // unlike with distributed tensor, we have to ensure that tensor is sparse before unfolding
        val sparsed = sparse().entries()
        val unfolded = mode match {
            case UNFOLD_MODE.A1 => sparsed.map(
                    entry => MatrixEntry(entry.i, entry.j + J * entry.k, entry.value)
                )
            case UNFOLD_MODE.A2 => sparsed.map(
                    entry => MatrixEntry(entry.j, entry.i + I * entry.k, entry.value)
                )
            case UNFOLD_MODE.A3 => sparsed.map(
                    entry => MatrixEntry(entry.k, entry.i + I * entry.j, entry.value)
                )
            case _ => throw new Exception("Unrecognized unfolding mode")
        }

        // convert into Array[Array[Double]] with sorting within each group
        val pseudo = unfolded.
            map(entry => (entry.i, (entry.j, entry.value))).groupBy(_._1).toArray.
            sortBy(_._1).map(elem => {
                val arr = elem._2
                arr.map(_._2).sortBy(_._1).map(_._2)
            })
        new DoubleMatrix(pseudo)
    }

    /**
     * Uncompresses tensor by dimensions (if possible) and returns itself with all the entries.
     * @return uncompressed tensor
     */
    def sparse(): LocalTensor = {
        val Rows:Int = numRows().toInt
        val Cols:Int = numCols().toInt
        val Layers:Int = numLayers().toInt
        // if size was computed we return current tensor
        if (recompute) return this

        // in local mode we can build sets with indices already present in current tensor.
        val set = entries.map(entry => (entry.i, entry.j, entry.k)).toSet
        var buffer:ArrayBuffer[TensorEntry] = new ArrayBuffer()

        (0 until Rows).foreach(
            i => { (0 until Cols).foreach(
                j => { (0 until Layers).foreach(
                    k => {
                        if (!set.contains( (i, j, k) )) {
                            buffer.append(TensorEntry(i, j, k, 0.0))
                        }
                    } )
                } )
            }
        )
        val elements = entries ++ buffer

        new LocalTensor(elements, Rows, Cols, Layers)
    }
}
