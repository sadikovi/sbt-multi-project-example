package org.hello.bar


object Bar {
    private val timing = System.currentTimeMillis

    def showTiming: Long = this.timing

    def main(args:Array[String]) {
        println("Bar")
    }
}
