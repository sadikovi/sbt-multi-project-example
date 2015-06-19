package org.hello.foo

import org.hello.bar._
import scala.util.Random
import scala.io.Source


class Simple(step:Int) {
    private val _step:Int = step

    def timeStep: Long = this._step * Bar.showTiming

    def showStep: Int = this._step
}

object Foo {
    private val Limit = 10

    def main(args:Array[String]) {
        val iterations:Int = if (args.nonEmpty) args(0).toInt else 1

        (0 until iterations).foreach(
            x => {
                val aNew = new Simple(Random.nextInt(this.Limit))
                println(aNew.timeStep)
            }
        )

        println("# parsing file now")
        
        Source.fromURL(getClass.getResource("dataset.txt")).getLines.foreach(
            x => {
                val aNew = new Simple(Random.nextInt(this.Limit))
                println(aNew.timeStep)
            }
        )
    }
}
