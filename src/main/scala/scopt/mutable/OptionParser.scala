package scopt.mutable

import scopt.generic._

/** scopt.mutable.OptionParser is instantiated within your object,
 * set up by an (ordered) sequence of invocations of 
 * the various builder methods such as
 * <a href="#opt[A](Char,String)(Read[A]):Def[A]"><code>opt</code></a> method or
 * <a href="#arg[A](String)(Read[A]):Def[A]"><code>arg</code></a> method.
 * {{{
 * val parser = new scopt.OptionParser("scopt") {
 *   opt[Int]('f', "foo") action { x =>
 *     c = c.copy(foo = x) } text("foo is an integer property")
 *   opt[String]('o', "out") required() valueName("<file>") action { x =>
 *     c = c.copy(out = x) } text("out is a required string property")
 *   opt[Boolean]("xyz") action { x =>
 *     c = c.copy(xyz = x) } text("xyz is a boolean property")
 *   opt[(String, Int)]("max") action { case (k, v) =>
 *     c = c.copy(libName = k, maxCount = v) } validate { x =>
 *     if (x._2 > 0) success else failure("Value <max> must be >0") 
 *   } keyValueName("<libname>", "<max>") text("maximum count for <libname>")
 *   opt[Unit]("verbose") action { _ =>
 *     c = c.copy(verbose = true) } text("verbose is a flag")
 *   note("some notes.\n")
 *   help("help") text("prints this usage text")
 *   arg[String]("<mode>") required() action { x =>
 *     c = c.copy(mode = x) } text("required argument")
 *   arg[String]("<file>...") unbounded() action { x =>
 *     c = c.copy(files = c.files :+ x) } text("optional unbounded args")
 * }
 * if (parser.parse(args)) {
 *   // do stuff
 * }
 * else {
 *   // arguments are bad, usage message will have been displayed
 * }
 * }}}
 */
case class OptionParser(programName: String) extends GenericOptionParser[Unit] {
  import OptionDefinition._

  type Def[A] = OptionDef[A]  
  def errorOnUnknownArgument: Boolean = true
  val options = new scala.collection.mutable.ListBuffer[OptionDef[_]]

  case class OptionDef[A: Read](
    id: Int,
    kind: OptionDefKind,
    name: String,
    _shortOpt: Option[Char] = None,
    _keyName: Option[String] = None,
    _valueName: Option[String] = None,
    _desc: String = "",
    _action: (A => Unit) = { (a: A) => () },
    _validations: Seq[A => Either[String, Unit]] = Seq(),
    _minOccurs: Int = 0,
    _maxOccurs: Int = 1) extends OptionDefinition[A, Unit] {
    /** Adds callback function. */
    def action(f: A => Unit): OptionDef[A] =
      updateOption(copy(_action = (a: A) => { _action(a); f(a) }))
    /** Adds short option -x. */
    def shortOpt(x: Char): OptionDef[A] =
      updateOption(copy(_shortOpt = Some(x)))
    /** Requires the option to appear at least `n` times. */
    def minOccurs(n: Int): OptionDef[A] =
      updateOption(copy(_minOccurs = n))
    /** Requires the option to appear at least once. */
    def required(): OptionDef[A] = minOccurs(1)
    /** Chanages the option to be optional. */
    def optional(): OptionDef[A] = minOccurs(0)
    /** Allows the argument to appear at most `n` times. */
    def maxOccurs(n: Int): OptionDef[A] =
      updateOption(copy(_maxOccurs = n))
    /** Allows the argument to appear multiple times. */
    def unbounded(): OptionDef[A] = maxOccurs(UNBOUNDED)
    /** Adds description in the usage text. */
    def text(x: String): OptionDef[A] =
      updateOption(copy(_desc = x))
    /** Adds value name used in the usage text. */
    def valueName(x: String): OptionDef[A] =
      updateOption(copy(_valueName = Some(x)))
    /** Adds key name used in the usage text. */
    def keyName(x: String): OptionDef[A] =
      updateOption(copy(_keyName = Some(x)))
    /** Adds key and value names used in the usage text. */
    def keyValueName(k: String, v: String): OptionDef[A] =
      keyName(k) valueName(v)
    /** Adds custom validation. */
    def validate(f: A => Either[String, Unit]) =
      updateOption(copy(_validations = _validations :+ f))

    def callback: (A, Unit) => Unit =
      { (a, c) => _action(a) }
    def getMinOccurs: Int = _minOccurs
    def getMaxOccurs: Int = _maxOccurs
  }

  protected def makeDef[A: Read](kind: OptionDefKind, name: String): Def[A] =
    updateOption(OptionDef[A](id = generateId, kind = kind, name = name))

  protected def updateOption[A: Read](option: OptionDef[A]): OptionDef[A] = {
    val idx = options indexWhere { _.id == option.id }
    if (idx > -1) options(idx) = option
    else options += option
    option
  }
  
  /** parses the given `args`.
   * @return `true` if successful, `false` otherwise
   */
  def parse(args: Seq[String]): Boolean =
    parse(args, ()) match {
      case Some(x) => true
      case None    => false
    }

  /** adds an option invoked by `--name` that displays usage text.
   * @param name0 name of the option
   */
  def help(name0: String): OptionDef[Unit] =
    opt[Unit](name0) action {_ => showUsage}
}
