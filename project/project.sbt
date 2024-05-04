scalacOptions ++= { CrossVersion.partialVersion(scalaVersion.value) match {
  case Some((3, _))           => Seq("-source:3.2", "-java-output-version:8", "-explain")
  case Some((2, 12))          => Seq("-Xsource:2.12", "-target:8", "-explaintypes", "-Ypartial-unification", "-Ywarn-macros:after")
  case Some((2, n)) if n < 12 => Seq("-Xsource:2.12", "-target:8", "-explaintypes", "-Ypartial-unification")
  case Some((2, _))           => Seq("-Xsource:2.13", "-target:8", "-explaintypes", "-Ymacro-annotations")
  case _                      => Nil
}}

libraryDependencies ++= Seq(
  "eu.timepit" %% "refined" % Versions.TypeLevel.Refined,
  "io.estatico" %% "newtype" % Versions.TypeLevel.Newtype,
  "org.typelevel" %% "simulacrum" % Versions.TypeLevel.Simulacrum,
  "org.typelevel" %% "cats-core" % Versions.TypeLevel.Cats
)

addCompilerPlugin("org.scalamacros" % "paradise" % Versions.Compiler.Paradise cross CrossVersion.full)

addSbtPlugin("org.scalameta" % "sbt-mdoc" % Versions.Sbt.Mdoc)
