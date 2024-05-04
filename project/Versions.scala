object Versions {
  object Compiler {
    val Scala = "3.4.1"
    //val SemanticDB = "4.8.4" // ...because broken-Simulacrum (1.0.1) needs Scala 2.13.6 (yes, .6)
    val SemanticDB = "4.9.3" // otherwise, if not using Simulacrum
    val Paradise = "2.1.1"
    val KindProjector = "0.13.3"
  }

  val Scala = Compiler.Scala

  object Sbt {
    val DotEnv = "2.1.233"
    val Tpolecat = "0.5.0"
    val ScapeGoat = "1.2.3"
    val ScalaFmt = "2.4.3"
    val Revolver = "0.10.0"
    val ScalaFix = "0.12.0"
    val BuildInfo = "0.11.0"
    val Mdoc = "2.5.2"
    val Scoverage = "1.9.2"
    val Assembly = "2.2.0"
    val AssemblyL4J2 = "1.1.3"
    val Release = "1.4.0"

    val Plugins = "0.2.1-SNAPSHOT"
  }

  object TypeLevel {
    val Refined = "0.11.1"
    val Newtype = "0.4.4"
    val Simulacrum = "1.0.1"

    val Shapeless = "3.4.0"

    val Cats = "2.10.0"
    val CatsEffect = "3.5.4"
    val Fs2 = "3.10.2"

    val Log4Cats = "2.6.0"
  }

  val ScapeGoat = "1.4.11"

  val Slf4j = "2.0.12"
  val Logback = "1.5.3"

  object Typesafe {
    val Config = "1.4.3"
    val Logging = "3.9.5"
  }

  val Shapeless = "2.3.10"

  val PureConfig = "0.17.6"

  val SourceCode = "0.3.1"

  val ScalaCommon = "4.7.2"

  val ScalaTest    = "3.2.18"
  val MockitoScala = "1.17.31"
  val Scalacheck   = "2.12.0"
  val ScalaCheck117 = "3.2.18.0"
}
