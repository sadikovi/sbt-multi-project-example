import org.scalatest._
// abstract general testing class
abstract class UnitTestSpec extends FlatSpec with Matchers with OptionValues with Inside with Inspectors
