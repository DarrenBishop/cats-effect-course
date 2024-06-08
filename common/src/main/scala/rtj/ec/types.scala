package rtj
package ec

import scala.concurrent.duration.FiniteDuration

trait PkgTypes {
  type EC = Context
  val EC: Context.type = Context

  type ECApp = Context.App

  type Future[+A] = scala.concurrent.Future[A]
  val Future: scala.concurrent.Future.type = scala.concurrent.Future

  val Await: scala.concurrent.Await.type = scala.concurrent.Await

  type ShutdownHook = Context.ShutdownHook

  case class AsyncSettings(duration: FiniteDuration)
}

object types extends PkgTypes

export types.*
