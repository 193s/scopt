import org.specs2._

class ImmutableParserSpec extends Specification { def is =      s2"""
  This is a specification to check the immutable parser
  
  opt[Unit]('f', "foo") action { x => x } should
    parse () out of --foo                                       ${unitParser("--foo")}
    parse () out of -f                                          ${unitParser("-f")} 

  opt[Int]('f', "foo") action { x => x } should
    parse 1 out of --foo 1                                      ${intParser("--foo", "1")}
    parse 1 out of --foo:1                                      ${intParser("--foo:1")}
    parse 1 out of -f 1                                         ${intParser("-f", "1")}
    parse 1 out of -f:1                                         ${intParser("-f:1")}
    fail to parse --foo                                         ${intParserFail{"--foo"}}
    fail to parse --foo bar                                     ${intParserFail("--foo", "bar")}

  opt[String]("foo") action { x => x } should
    parse "bar" out of --foo bar                                ${stringParser("--foo", "bar")}
    parse "bar" out of --foo:bar                                ${stringParser("--foo:bar")}

  opt[Double]("foo") action { x => x } should
    parse 1.0 out of --foo 1.0                                  ${doubleParser("--foo", "1.0")}
    parse 1.0 out of --foo:1.0                                  ${doubleParser("--foo:1.0")}
    fail to parse --foo bar                                     ${doubleParserFail("--foo", "bar")}

  opt[Boolean]("foo") action { x => x } should
    parse true out of --foo true                                ${trueParser("--foo", "true")}
    parse true out of --foo:true                                ${trueParser("--foo:true")}
    parse true out of --foo 1                                   ${trueParser("--foo", "1")}
    parse true out of --foo:1                                   ${trueParser("--foo:1")}
    fail to parse --foo bar                                     ${boolParserFail("--foo", "bar")}

  opt[(String, Int)]("foo") action { x => x } should
    parse ("k", 1) out of --foo k=1                             ${pairParser("--foo", "k=1")}
    parse ("k", 1) out of --foo:k=1                             ${pairParser("--foo:k=1")}
    fail to parse --foo                                         ${pairParserFail("--foo")}
    fail to parse --foo bar                                     ${pairParserFail("--foo", "bar")}
    fail to parse --foo k=bar                                   ${pairParserFail("--foo", "k=bar")}

  opt[String]("foo") required() action { x => x } should
    fail to parse Nil                                           ${requiredFail()}

  unknown options should
    fail to parse by default                                    ${intParserFail("-z", "bar")}

  opt[(String, Int)]("foo") action { x => x } validate { x =>
    if (x > 0) success else failure("Option --foo must be >0") } should
    fail to parse --foo 0                                       ${validFail("--foo", "0")}

  arg[Int]("<port>") action { x => x } should
    parse 80 out of 80                                          ${intArg("80")}
    be required and should fail to parse Nil                    ${intArgFail()}

  arg[String]("<a>"); arg[String]("<b>") action { x => x } should
    parse "b" out of a b                                        ${multipleArgs("a", "b")}

  arg[String]("<a>") action { x => x} unbounded() optional(); arg[String]("<b>") optional() should
    parse "b" out of a b                                        ${unboundedArgs("a", "b")}
    parse nothing out of Nil                                    ${emptyArgs()}

  cmd("update") action { x => x } children { opt[Unit]("foo") action { x => x} } should
    parse () out of update                                      ${cmdParser("update")}
    parse () out of update --foo                                ${cmdParser("update", "--foo")}
    fail to parse --foo                                         ${cmdParserFail("--foo")}

  cmd("update"); cmd("commit"); arg[String]("<a>") action { x => x} should
    parse commit out of update commit                           ${cmdPosParser("update", "commit")}
    fail to parse foo update                                    ${cmdPosParserFail("foo", "update")}

  help("help") should
    print usage text --help                                     ${helpParser("--help")}
                                                                """

  def unitParser(args: String*) = {
    val parser = new scopt.OptionParser[Config]("scopt") {
      head("scopt", "3.x")
      opt[Unit]('f', "foo") action { (x, c) => c.copy(flag = true) }
    }
    val result = parser.parse(args.toSeq, Config())
    result.get.flag === true
  }

  val intParser1 = new scopt.OptionParser[Config]("scopt") {
    head("scopt", "3.x")
    opt[Int]('f', "foo") action { (x, c) => c.copy(intValue = x) }
  }
  def intParser(args: String*) = {
    val result = intParser1.parse(args.toSeq, Config())
    result.get.intValue === 1
  }
  def intParserFail(args: String*) = {
    val result = intParser1.parse(args.toSeq, Config())
    result === None
  }

  val stringParser1 = new scopt.OptionParser[Config]("scopt") {
    head("scopt", "3.x")
    opt[String]("foo") action { (x, c) => c.copy(stringValue = x) }
  }
  def stringParser(args: String*) = {
    val result = stringParser1.parse(args.toSeq, Config())
    result.get.stringValue === "bar"
  }

  val doubleParser1 = new scopt.OptionParser[Config]("scopt") {
    head("scopt", "3.x")
    opt[Double]("foo") action { (x, c) => c.copy(doubleValue = x) }
  }
  def doubleParser(args: String*) = {
    val result = doubleParser1.parse(args.toSeq, Config())
    result.get.doubleValue === 1.0
  }
  def doubleParserFail(args: String*) = {
    val result = doubleParser1.parse(args.toSeq, Config())
    result === None
  }

  val boolParser1 = new scopt.OptionParser[Config]("scopt") {
    head("scopt", "3.x")
    opt[Boolean]("foo") action { (x, c) => c.copy(boolValue = x) }
  }
  def trueParser(args: String*) = {
    val result = boolParser1.parse(args.toSeq, Config())
    result.get.boolValue === true
  }
  def boolParserFail(args: String*) = {
    val result = boolParser1.parse(args.toSeq, Config())
    result === None
  }

  val pairParser1 = new scopt.OptionParser[Config]("scopt") {
    head("scopt", "3.x")
    opt[(String, Int)]("foo") action { case ((k, v), c) => c.copy(key = k, intValue = v) }
  }
  def pairParser(args: String*) = {
    val result = pairParser1.parse(args.toSeq, Config())
    (result.get.key === "k") and (result.get.intValue === 1)
  }
  def pairParserFail(args: String*) = {
    val result = pairParser1.parse(args.toSeq, Config())
    result === None
  }

  val requireParser1 = new scopt.OptionParser[Config]("scopt") {
    head("scopt", "3.x")
    opt[String]("foo") required() action { (x, c) => c.copy(stringValue = x) }
  }
  def requiredFail(args: String*) = {
    val result = requireParser1.parse(args.toSeq, Config())
    result === None
  }

  val validParser1 = new scopt.OptionParser[Config]("scopt") {
    head("scopt", "3.x")
    opt[Int]('f', "foo") action { (x, c) => c.copy(intValue = x) } validate { x =>
      if (x > 0) success else failure("Option --foo must be >0") } validate { x =>
      failure("Just because") }
  }
  def validFail(args: String*) = {
    val result = validParser1.parse(args.toSeq, Config())
    result === None
  }

  val intArgParser1 = new scopt.OptionParser[Config]("scopt") {
    head("scopt", "3.x")
    arg[Int]("<port>") action { (x, c) => c.copy(intValue = x) }
  }
  def intArg(args: String*) = {
    val result = intArgParser1.parse(args.toSeq, Config())
    result.get.intValue === 80
  }
  def intArgFail(args: String*) = {
    val result = intArgParser1.parse(args.toSeq, Config())
    result === None
  }

  val multipleArgsParser1 = new scopt.OptionParser[Config]("scopt") {
    head("scopt", "3.x")
    arg[String]("<a>") action { (x, c) => c.copy(a = x) }
    arg[String]("<b>") action { (x, c) => c.copy(b = x) }
  }
  def multipleArgs(args: String*) = {
    val result = multipleArgsParser1.parse(args.toSeq, Config())
    (result.get.a === "a") and (result.get.b === "b")
  }

  val unboundedArgsParser1 = new scopt.OptionParser[Config]("scopt") {
    head("scopt", "3.x")
    arg[String]("<a>") action { (x, c) => c.copy(a = x) } unbounded() optional()
    arg[String]("<b>") action { (x, c) => c.copy(b = x) } optional()
  }
  def unboundedArgs(args: String*) = {
    val result = unboundedArgsParser1.parse(args.toSeq, Config())
    (result.get.a === "b") and (result.get.b === "")
  }
  def emptyArgs(args: String*) = {
    val result = unboundedArgsParser1.parse(args.toSeq, Config())
    (result.get.a === "") and (result.get.b === "")
  }

  val cmdParser1 = new scopt.OptionParser[Config]("scopt") {
    head("scopt", "3.x")
    cmd("update") action { (x, c) => c.copy(flag = true) } children {
      opt[Unit]("foo") action { (x, c) => c.copy(stringValue = "foo") }
    }
  }
  def cmdParser(args: String*) = {
    val result = cmdParser1.parse(args.toSeq, Config())
    result.get.flag === true
  }
  def cmdParserFail(args: String*) = {
    val result = cmdParser1.parse(args.toSeq, Config())
    result === None
  }

  val cmdPosParser1 = new scopt.OptionParser[Config]("scopt") {
    head("scopt", "3.x")
    cmd("update") action { (x, c) => c.copy(flag = true) }
    cmd("commit")
    arg[String]("<a>") action { (x, c) => c.copy(a = x) } 
  }
  def cmdPosParser(args: String*) = {
    val result = cmdPosParser1.parse(args.toSeq, Config())
    result.get.a === "commit"
  }
  def cmdPosParserFail(args: String*) = {
    val result = cmdPosParser1.parse(args.toSeq, Config())
    result === None    
  }

  def helpParser(args: String*) = {
    case class Config(foo: Int = -1, out: String = "", xyz: Boolean = false,
      libName: String = "", maxCount: Int = -1, verbose: Boolean = false,
      mode: String = "", files: Seq[String] = Seq())
    val parser = new scopt.OptionParser[Config]("scopt") {
      head("scopt", "3.x")
      opt[Int]('f', "foo") action { (x, c) =>
        c.copy(foo = x) } text("foo is an integer property")
      opt[String]('o', "out") required() valueName("<file>") action { (x, c) =>
        c.copy(out = x) } text("out is a required string property")
      opt[(String, Int)]("max") action { case ((k, v), c) =>
        c.copy(libName = k, maxCount = v) } validate { x =>
        if (x._2 > 0) success else failure("Value <max> must be >0") 
      } keyValueName("<libname>", "<max>") text("maximum count for <libname>")
      opt[Unit]("verbose") action { (_, c) =>
        c.copy(verbose = true) } text("verbose is a flag")
      cmd("update") action { (_, c) =>
        c.copy(mode = "update") } text("update is a command.") children {
        opt[Boolean]("xyz") action { (x, c) =>
          c.copy(xyz = x) } text("xyz is a boolean property")
      }
      note("some notes.\n")
      help("help") text("prints this usage text")
      arg[String]("<file>...") unbounded() optional() action { (x, c) =>
        c.copy(files = c.files :+ x) } text("optional unbounded args")
    }
    parser.parse(args.toSeq, Config())
    parser.usage === """
scopt 3.x
Usage: scopt [update] [options] [<file>...]

  -f <value> | --foo <value>
        foo is an integer property
  -o <file> | --out <file>
        out is a required string property
  --max:<libname>=<max>
        maximum count for <libname>
  --verbose
        verbose is a flag
some notes.

  --help
        prints this usage text
  <file>...
        optional unbounded args

Command: update
update is a command.

  --xyz <value>
        xyz is a boolean property
"""
  }

  case class Config(flag: Boolean = false, intValue: Int = 0, stringValue: String = "",
    doubleValue: Double = 0.0, boolValue: Boolean = false,
    key: String = "", a: String = "", b: String = "")
}
