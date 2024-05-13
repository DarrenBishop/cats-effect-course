import sbt.Keys.*
import sbt.{Def, *}
import mdoc.{DocusaurusPlugin, MdocPlugin}
import sbt.plugins.JvmPlugin
import shapeless.<:!<


object Mdoc extends AutoPlugin {

  override def requires: Plugins = JvmPlugin

  override def trigger: PluginTrigger = allRequirements

  import MdocPlugin.autoImport.*
  import DocusaurusPlugin.autoImport.*

  object autoImport {
    type Settings = Seq[Setting[?]]

    val mdocRoot = {
      settingKey[File](
        "A convenience root directory from which to derive `mdocIn` as `mdocRoot / docs` and mdocOut as `mdocRoot / target / mdoc`, if set"
      )
    }

    implicit class SettingsOps(settings: Settings) {
      def filter(p: Def.Setting[?] => Boolean): Settings = settings.filter(p)
      def exclude(keys: Def.ScopedKey[?]*): Settings = filter(setting => !keys.toSet(setting.key))
      def include(keys: Def.ScopedKey[?]*): Settings =  filter(setting => keys.toSet(setting.key))
      def get(key: Def.ScopedKey[?]): Def.Setting[?] = include(key).head
    }

    implicit def scopedToAttributeKey[T](scoped: Scoped)(implicit ev: Scoped <:!< Taskable[?]): AttributeKey[?] = scoped.key
  }

  import autoImport.*

  def isRootDocs = Def.setting {
    val rootDir = (LocalRootProject / baseDirectory).value
    val rootBase = (LocalRootProject / baseDirectory).value.getName
    val rootName = (LocalRootProject / name).value

    val thisBase = baseDirectory.value.getName
    val docsName = s"$rootBase-docs"
    name.value == docsName || thisBase == docsName
  }

  def rootDocs = Def.setting { (LocalRootProject / baseDirectory).value / "docs" }

  override def projectSettings: Settings =
    MdocPlugin.projectSettings ++
    DocusaurusPlugin.projectSettings ++
    Seq(
      mdocIn := {
        if (isRootDocs.value) rootDocs.value
        else baseDirectory.value / "docs"
      },
      //mdocOut := {
      //  if (isRootDocs.value) (LocalRootProject / baseDirectory).value / "mdoc"
      //  else baseDirectory.value / "mdoc"
      //},
      mdocVariables := Map(
        "SCALA_VERSION" -> scalaVersion.value,
        "VERSION" -> version.value
      ),
      mdocExtraArguments ++= Seq(
        "--verbose true",
        "--clean-target true",
        "--allow-code-fence-indented",
        "--exclude **/.DS_Store",
        "--exclude .DS_Store",
      ),
      //mdocExtraArguments ++= {
      //  if (mdocRoot.?.value.isDefined)
      //    Seq(
      //      "--in", (mdocRoot.value / "docs").toString,
      //      "--out", (mdocRoot.value / "target" / "mdoc").toString
      //    )
      //  else Nil
      //},
      //mdocIn := baseDirectory.value / "docs",
      //mdoc := Def.inputTask {
      //  val parsed = sbt.complete.DefaultParsers.spaceDelimited("<arg>").parsed
      //  println(parsed.mkString(" "))
      //  println(mdocIn.value.toString)
      //  mdoc.toTask(parsed.mkString(" "))
      //  //if (mdocIn.value.exists()) Def.taskDyn {
      //  //  val parsed = sbt.complete.DefaultParsers.spaceDelimited("<arg>").parsed
      //  //  println(parsed.mkString(" "))
      //  //  println(mdocIn.value.toString)
      //  //  (Compile / mdoc).toTask(parsed.mkString(" "))
      //  //}
      //  //else Def.task { () }
      //}.inputTaskValue
    )
}
