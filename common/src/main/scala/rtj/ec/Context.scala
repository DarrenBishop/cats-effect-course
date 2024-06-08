package rtj.ec

import java.util.concurrent.{ExecutorService, Executors}

import scala.concurrent.ExecutionContext


case class Context private[ec](private[ec] val hook: Context.ShutdownHook)(using val execution: ExecutionContext) {
  def shutdown(): Unit = hook()
}

object Context {
  type ShutdownHook = () => Unit
  private val hooksM = scala.collection.concurrent.TrieMap.empty[String, ShutdownHook]

  def shutdownAll(): Unit = hooksM.keys.foreach { k =>
    hooksM.updateWith(k)(_.flatMap { h => h(); None })
  }

  def apply(threadPoolSize: Int= 4): Context = {
    val executorService: ExecutorService = Executors.newFixedThreadPool(threadPoolSize)
    val executionContext: ExecutionContext = ExecutionContext.fromExecutorService(executorService)
    val hook: ShutdownHook = () => executorService.shutdown()
    hooksM.addOne(executorService.toString, hook)
    sys.addShutdownHook(hook())
    Context(hook)(using executionContext)
  }

  abstract class App {
    self: Singleton =>
    protected given ec: Context = Context()

    def main(args: Array[String]): Unit = {
      ec.shutdown()
    }
  }
}
