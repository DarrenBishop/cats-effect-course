import sbt.Keys.*
import sbt.plugins.IvyPlugin
import sbt.{Def, *}


object Support extends AutoPlugin {

  override def requires: Plugins = IvyPlugin

  override def trigger: PluginTrigger = allRequirements

  object autoImport extends VersionSelector.Syntax {

    def when[T, U: Manifest](test: SettingKey[T])(predicate: T => Boolean)(values: U*): Def.Initialize[Seq[U]] =
      when(test(predicate))(values: _*)

    def when[U: Manifest](predicate: Def.Initialize[Boolean])(values: U*): Def.Initialize[Seq[U]] = Def.setting[Seq[U]] {
      if (predicate.value) values
      else Nil
    }

    def compilerPluginsIf[T](test: SettingKey[T])(predicate: T => Boolean)(dependencies: ModuleID*) = Def.setting[Seq[ModuleID]] {
      if (predicate(test.value)) dependencies.map(compilerPlugin)
      else Nil
    }

    def compilerPluginIf[T](test: SettingKey[T])(predicate: T => Boolean)(dependency: ModuleID) =
      compilerPluginsIf(test)(predicate)(dependency)

    def addCompilerPluginIf[T](test: SettingKey[T])(predicate: T => Boolean)(dependency: ModuleID): Setting[Seq[ModuleID]] = {
      libraryDependencies ++= compilerPluginIf(test)(predicate)(dependency).value
    }
  }
}
