package org.isa.linalg

import org.isa.annotation.Experimental

/**
 * :: Experimental ::
 * Represents a 3rd-order tensor local/backed by an RDD.
 */
@Experimental
private[linalg] trait Tensor extends Serializable {

    /** Gets or computes the number of rows. */
    def numRows(): Long

    /** Gets or computes the number of columns. */
    def numCols(): Long

    /** Gets or computes the number of layers. */
    def numLayers(): Long
}


/** unfolding mode and classes */
@Experimental
private[linalg] case class UnfoldModeEntry(name:String)

/** Unfolding mode. Use that to specify A1, A2, or A3 unfoldings */
@Experimental
object UNFOLD_MODE {
    val A1 = new UnfoldModeEntry("A1")
    val A2 = new UnfoldModeEntry("A2")
    val A3 = new UnfoldModeEntry("A3")
}

// entry for tensor core
@Experimental
case class TensorEntry(i:Int, j:Int, k:Int, value:Double)
