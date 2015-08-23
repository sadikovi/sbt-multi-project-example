package org.isa.rdd

import scala.reflect.ClassTag
import org.apache.spark.InterruptibleIterator
import org.apache.spark.{SparkContext, Partition, TaskContext}
import org.apache.spark.rdd.RDD
import org.apache.hadoop.mapred.JobConf
import org.apache.hadoop.fs.{FileSystem, FileStatus, Path}

private[rdd] class FilenameCollectionPartition[String](
    var rddId: Long,
    var slice: Int,
    var values: Seq[String]
) extends Partition with Serializable {

    def iterator: Iterator[String] = values.iterator

    override def hashCode(): Int = (41 * (41 + rddId) + slice).toInt

    override def equals(other:Any): Boolean = other match {
        case that: FilenameCollectionPartition[_] =>
            this.rddId == that.rddId && this.slice == that.slice
        case _ => false
    }

    override def index: Int = slice
}

private[rdd] class FilenameCollectionRDD[T:ClassTag](
    @transient sc: SparkContext,
    @transient data: Seq[String],
    numSlices: Int
) extends RDD[String](sc, Nil) {

    override def getPartitions: Array[Partition] = {
        val slices = this.slice(data, numSlices).toArray
        slices.indices.map(i => new FilenameCollectionPartition(id, i, slices(i))).toArray
    }

    override def compute(s:Partition, context:TaskContext): Iterator[String] = {
        val iterator = new InterruptibleIterator(
            context,
            s.asInstanceOf[FilenameCollectionPartition[String]].iterator
        )
        iterator
    }

    private def slice(seq: Seq[String], numSlices: Int): Seq[Seq[String]] = {
        require(numSlices >= 1, "Positive number of slices required")

        def positions(length: Long, numSlices: Int): Iterator[(Int, Int)] = {
            (0 until numSlices).iterator.map(i => {
                val start = ((i * length) / numSlices).toInt
                val end = (((i + 1) * length) / numSlices).toInt
                (start, end)
            })
        }

        val array = seq.toArray
        positions(array.length, numSlices).map(
            { case (start, end) => array.slice(start, end).toSeq }
        ).toSeq
    }
}
