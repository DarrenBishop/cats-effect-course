import sbt.librarymanagement.SemanticSelector
import sbt.{CrossVersion, VersionNumber}

import simulacrum.{typeclass, op}

@typeclass
trait VersionSelector[V] {
  private def clean(selector: String): String = selector.replaceAll("^[<>=]", "")
  @op("??", true) def matches(v: V, selector: String): Boolean
  @op("<", true) def lessThan(v: V, selector: String): Boolean = matches(v, "<" + clean(selector))
  @op("<=", true) def lessThanOrEqual(v: V, selector: String): Boolean = matches(v, "<=" + clean(selector))
  @op(">", true) def greaterThan(v: V, selector: String): Boolean = matches(v, ">" + clean(selector))
  @op(">=", true) def greaterThanOrEqual(v: V, selector: String): Boolean = matches(v, ">=" + clean(selector))
  @op("===", true) def equals(v: V, selector: String): Boolean = matches(v, "=" + clean(selector))
}

object VersionSelector {

  type SemVer = VersionNumber
  object SemVer {
    def apply(v: String): SemVer = VersionNumber(v)
    def unapply(v: String): Option[SemVer] = Option(apply(v))
  }

  object PartialVersion {
    def unapply(v: String): Option[(Long, Long)] = CrossVersion.partialVersion(v)
  }

  implicit val semVerVersionSelector: VersionSelector[SemVer] =
    (sv: SemVer, selector: String) => sv.matchesSemVer(SemanticSelector(selector))

  trait Syntax extends ToVersionSelectorOps {
    type SemVer = VersionSelector.SemVer
    val SemVer = VersionSelector.SemVer

    val versionSelector = VersionSelector
    val PartialVersion = VersionSelector.PartialVersion
  }

  object syntax extends Syntax
}
