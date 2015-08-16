package org.isa.util

import scala.io.Source
import java.io.{InputStream, File, FileNotFoundException}


/**
 * Configuration helper to parse `.config` files.
 *
 */
object Configuration {
    private var pairs:Map[String, String] = Map()

    private val SectionPattern = """^[\s]*\[([\w-]+)\][\s]*$""".r

    private val ConfigPattern = """^[\s]*([\w-]+)[\s]*=[\s]*([^"\s]*|".*")[\s]*$""".r

    private val ArraySeparator = ","


    /**
     * Loads configuration file from file path.
     * Throws an exception, if file is not found, or path is empty
     *
     * @param pathToFile file path
     */
    def loadFile(pathToFile:String) {
        this.reset

        require(pathToFile.nonEmpty, throw new Exception("@Configuration: path is empty"))
        require(new File(pathToFile).exists, throw new FileNotFoundException("@Configuration: file does not exist: " + pathToFile))

        val lines = Source.fromFile(pathToFile).getLines.toArray
        this parseConfigurationFor lines
    }


    /**
     * Loads configuration file from input stream.
     * Used when requested file in .jar file
     *
     * @param stream java.io.InputStream object
     */
    def loadInputStream(stream:InputStream) {
        this.reset

        val lines = Source.fromInputStream(stream).getLines.toArray

        this parseConfigurationFor lines
    }


    /**
     * Parses configuration lines.
     * Defines sections, lines and comments.
     * We can call those methods sequentially without testing, since
     * patterns are completely different.
     *
     * @param lines lines of the configuration file
     */
    def parseConfigurationFor(lines:Array[String]) {
        var section:String = ""
        lines.foreach(
            line => {
                section = this parseSectionFor (line, section)
                this parseConfigurationFor (line, section)
            }
        )
    }


    /**
     * Parses section for a given line.
     * Returns previous section, if line does not match pattern,
     * or new line otherwise.
     *
     * @param line raw configuration line
     * @param section previous section of the file (can be empty)
     * @return new section, if line is section line, or previous otherwise
     */
    def parseSectionFor(line:String, section:String): String = {
        val iterator = this.SectionPattern findAllIn line
        if (iterator.nonEmpty) iterator.group(1) else section
    }


    /**
     * Parses configuration line and extracts key-value pair.
     * Uses section to create composite key, if available.
     *
     * @param line raw configuration line
     * @param section current section of the configuration file
     */
    def parseConfigurationFor(line:String, section:String="") {
        val iterator = this.ConfigPattern findAllIn line
        if (iterator.nonEmpty) {

            val (key, value) = (iterator.group(1), iterator.group(2))

            this.pairs +=
                { if (section.isEmpty) key else this.compositeKey(section, key) } ->
                { if (this foundArbitraryIn value) this stripArbitraryFrom value else value }
        }
    }


    /**
     * Returns value for key (and section) specified.
     * Empty string is returned, if there is no key.
     *
     * @param key key of the configuration map
     * @param section section within key exists (can be empty)
     * @return value for that key, or empty string, if key does not exist
     */
    def getValueFor(key:String, section:String=""): String = {
        val lookup = if (section.isEmpty) key else this.compositeKey(section, key)
        this.pairs.getOrElse(lookup, "")
    }


    /**
     * Returns converted to boolean value.
     * Raises an error, if value cannot be converted to Boolean.
     *
     * @param key key of the configuration map
     * @param section section within key exists (can be empty)
     * @return boolean value for that key
     */
    def getBooleanFor(key:String, section:String=""): Boolean = this.getValueFor(key, section).toBoolean


    /**
     * Returns converted to Integer value.
     * Raises an error, if value cannot be converted to Integer.
     *
     * @param key key of the configuration map
     * @param section section within key exists (can be empty)
     * @return integer value for that key
     */
    def getIntFor(key:String, section:String=""): Int = this.getValueFor(key, section).toInt


    /**
     * Returns array of strings.
     * Uses configuration separator.
     *
     * @param key key of the configuration map
     * @param section section within key exists (can be empty)
     */
     def getStringArrayFor(key:String, section:String=""): Array[String] = {
         this.getValueFor(key, section) match {
             case a if a.nonEmpty => a.split(this.ArraySeparator).map(x => x.stripPrefix("\\s").stripSuffix("\\s"))
             case _ => Array[String]()
         }
     }


    /**
     * Returns array of integers.
     * Uses configuration separator.
     *
     * @param key key of the configuration map
     * @param section section within key exists (can be empty)
     */
    def getIntArrayFor(key:String, section:String=""): Array[Int] = {
        this.getStringArrayFor(key, section) match {
            case a if a.nonEmpty => a.map(_.toInt)
            case _ => Array[Int]()
        }
    }


    /**
     * Returns array of integers.
     * Uses configuration separator.
     *
     * @param key key of the configuration map
     * @param section section within key exists (can be empty)
     */
    def getDoubleArrayFor(key:String, section:String=""): Array[Double] = {
        this.getStringArrayFor(key, section) match {
            case a if a.nonEmpty => a.map(_.toDouble)
            case _ => Array[Double]()
        }
    }


    /**
     * Returns all pairs of the configuration map.
     * [!] mostly for testing
     *
     * @return configuration map
     */
    def getAllPairs:Map[String, String] = this.pairs


    /**
     * Resets (removes entities from) map.
     *
     */
    def reset = this.pairs = Map()


    /**
     * Returns true, if it has found arbitrary property.
     * Arbitrary property means string tath wrapped in double quotes,
     * so it can contain any symbols (including double quotes), e.g. "test"
     *
     * @param line string to check
     * @return boolean flag whether string is arbitrary or not
     */
    private def foundArbitraryIn(line:String): Boolean = line.startsWith("\"") && line.endsWith("\"")


    /**
     * Removes first and last characters from arbitrary string.
     * Those characters are double quotes.
     *
     * @param line arbitrary string
     * @return stripped string
     */
    private def stripArbitraryFrom(line:String): String = line.take(line.length-1).drop(1)


    /**
     * Returns composite key for section and key
     *
     * @param section current section
     * @param key key within this section
     * @return composite key for section-key pair
     */
    private def compositeKey(section:String, key:String): String = s"${section}###${key}"


    /**
     * Checks whether passed line matches section pattern or not.
     *
     * @param line raw configuration line to check
     * @return true, if line is a section, otherwise false
     */
    private def isSection(line:String): Boolean = (this.SectionPattern findAllIn line).nonEmpty
}
