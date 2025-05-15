package rtj.coordination

import cats.effect.Outcome.*
import cats.effect.{Deferred, IO, IOApp, Ref}
import cats.syntax.all.*
import rtj.all.*

import scala.collection.immutable.Queue
import scala.util.Random

abstract class Mutex {
  def acquire: IO[Unit]
  def release: IO[Unit]
}

object Mutex extends IOApp.Simple {
  type Signal = Deferred[IO, Unit]
  case class State(locked: Boolean, signals: Queue[Signal])
  private val unlocked = State(false, Queue())

  def createWithCancellation(state: Ref[IO, State]): Mutex = new Mutex {
    /*
        Change the state of the Ref:
        - if the mutex is currently unlocked, state becomes (true, [])
        - if the mutex is locked, state becomes (true, queue + new-signal) AND WAIT ON THAT SIGNAL
     */
    def acquire: IO[Unit] = IO.uncancelable { poll =>
      for {
        signal <- IO.deferred[Unit]
        cleanup = state.flatModify {
          case State(locked, queue) => State(locked, queue.filterNot(_ eq signal)) -> (IO.dbg("cleanup") >> release)
        }
        pf = ?> {
          case State(false, _) => State(true, Queue()) -> IO.unit
          case State(true, queue) => State(true, queue.enqueue(signal)) -> poll(signal.get).onCancel(cleanup)
        }
        //_ <- state.flatModify(pf) // hangs
        _ <- state.modify(pf).flatten
      } yield ()
    }

    /*
        Change the state of the Ref:
        - if the mutex is unlocked, leave the state unchanged
        - if the mutex is locked:
          - if the queue is empty, unlock the mutex i.e. state becomes (false, [])
          - if the queue is not empty, remove a signal from the queue and complete it (thereby unblocing a fiber waiting on it)
     */
    def release: IO[Unit] = for {
      _ <- state.flatModify {
        case state @ State(false, _) => (state, IO.unit)
        case State(true, Queue())=> (State(false, Queue()), IO.unit)
        case State(true, queue) =>
          val (next, rest) = queue.dequeue
          (State(true, rest), next.complete(()))
      }
    } yield ()
  }

  def createWithoutCancellation(state: Ref[IO, State]): Mutex = new Mutex {
    /*
        Change the state of the Ref:
        - if the mutex is currently unlocked, state becomes (true, [])
        - if the mutex is locked, state becomes (true, queue + new-signal) AND WAIT ON THAT SIGNAL
     */
    def acquire: IO[Unit] = for {
      signal <- IO.deferred[Unit]
      _ <- state.flatModify {
        case State(false, _) => State(true, Queue()) -> IO.unit
        case State(true, queue) => State(true, queue.enqueue(signal)) -> signal.get
      }
    } yield ()

    /*
        Change the state of the Ref:
        - if the mutex is unlocked, leave the state unchanged
        - if the mutex is locked:
          - if the queue is empty, unlock the mutex i.e. state becomes (false, [])
          - if the queue is not empty, remove a signal from the queue and complete it (thereby unblocing a fiber waiting on it)
     */
    def release: IO[Unit] = for {
      _ <- state.flatModify {
        case state @ State(false, _) => (state, IO.unit)
        case State(true, Queue())=> (State(false, Queue()), IO.unit)
        case State(true, queue) =>
          val (next, rest) = queue.dequeue
          (State(true, rest), next.complete(()))
      }
    } yield ()
  }

  def create: IO[Mutex] = IO.ref(unlocked).map(createWithCancellation)

  def mutex[A](f: Mutex => IO[A]): IO[A] = create.flatMap(f)

  // App

  def criticalTask(): IO[Int] = IO.sleep(1.second) >> IO(Random.nextInt(100))

  val ids: List[Int] = (1 to 10).toList

  def createNonLockingTask(id: Int): IO[Int] = for {
    _ <- IO.void(s"[task $id] working...")
    res <- criticalTask()
    _ <- IO.void(s"[task $id] got result: $res")
  } yield res

  def demoNonLockingTask(): IO[List[Int]] = ids.parTraverse(createNonLockingTask)

  def createLockingTask(id: Int, mutex: Mutex): IO[Int] = for {
    _ <- IO.void(s"[task $id] waiting for permission...")
    _ <- mutex.acquire // blocks if the mutex has been acquired by some other fibre
    // critical section start
    _ <- IO.void(s"[task $id] working...")
    res <- criticalTask()
    _ <- IO.void(s"[task $id] got result: $res")
    // critical section end
    _ <- IO.void(s"[task $id] permissions relinquished")
    _ <- mutex.release
  } yield res

  def demoLockingTask(): IO[List[Int]] = mutex { mtx => ids.parTraverse(createLockingTask(_, mtx)) }

  def createCancellingTask(id: Int, mutex: Mutex): IO[Int] =
    if (id % 2 == 0) createLockingTask(id, mutex)
    else for {
      fib <- createLockingTask(id, mutex).onCancel(IO.void(s"[task $id] received cancellation")).start
      _ <- IO.sleep(2.seconds) >> fib.cancel
      out <- fib.join
      result <- out match {
        case Succeeded(effect) => effect
        case Errored(_) => IO.dbg(s"[task $id] errored").as(-1)
        case Canceled() => IO.dbg(s"[task $id] canceled").as(-2)
      }
    } yield result

  def demoCancellingTask(): IO[List[Int]] = mutex { mtx => ids.parTraverse(createCancellingTask(_, mtx)) }

  //def run: IO[Unit] = demoNonLockingTask().dvoid
  //def run: IO[Unit] = demoLockingTask().dvoid
  def run: IO[Unit] = demoCancellingTask().dvoid
}