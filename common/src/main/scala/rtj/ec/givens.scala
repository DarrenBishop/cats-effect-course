package rtj.ec

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration


trait PkgGivens {
  given Conversion[EC, ExecutionContext] = _.execution

  given Conversion[Int, scala.concurrent.duration.DurationInt] = scala.concurrent.duration.DurationInt(_)

  //given defaultAsyncSettings: AsyncSettings = AsyncSettings(1.second)

  //given Conversion[AsyncSettings, FiniteDuration] = _.duration
}

object givens extends PkgGivens

export givens.*
