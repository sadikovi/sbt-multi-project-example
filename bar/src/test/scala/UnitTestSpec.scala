import org.scalatest._
// abstract general testing class
abstract class UnitTestSpec extends FlatSpec with Matchers with OptionValues with Inside with Inspectors


case class ConditionResolver(val check:Boolean) {
    def skip(ignore: () => Unit) = Executor(check, ignore)
}

case class Executor(val flag:Boolean, val ignore: () => Unit) {
    def makeItHappen(action: => Unit) { if (flag) action else ignore.apply }

    def otherwise(action: => Unit) { makeItHappen (action) }
}

trait ConditionSpec {
    private def resolve(negate:Boolean, expression: => Boolean): ConditionResolver = {
        ConditionResolver(negate ^ expression)
    }

    def unless(expression: => Boolean): ConditionResolver = resolve(false, expression)

    def once(expression: => Boolean): ConditionResolver = resolve(true, expression)
}
