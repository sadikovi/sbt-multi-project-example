package org.isa.linalg

/**
 * Represents a distributively stored tensor backed by an RDD.
 */
trait DistributedTensor extends Serializable {

    /** Gets or computes the number of rows. */
    def numRows(): Long

    /** Gets or computes the number of columns. */
    def numCols(): Long

    /** Gets or computes the number of layers. */
    def numLayers(): Long
}
