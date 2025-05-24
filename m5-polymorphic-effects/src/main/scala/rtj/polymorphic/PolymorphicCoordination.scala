package rtj.polymorphic

import cats.{Applicative, FlatMap, Functor, Monad, Parallel}
import cats.effect.Outcome.{Canceled, Errored, Succeeded}
import cats.effect.{Concurrent, Deferred, Fiber, IO, IOApp, MonadCancel, MonadCancelThrow, Outcome, Poll, Ref, Spawn}
import cats.syntax.all.*
import cats.effect.syntax.all.*
import rtj.all.*

import scala.collection.immutable.Queue
import scala.util.Random

object PolymorphicCoordination extends IOApp.Simple {

  // Concurrent - Ref + Deferred for ANY effect type
  object my {
    //trait Concurrent[F[_]] extends Spawn[F] {
    //  def ref[A](a: A): F[Ref[F, A]]
    //  def deferred[A]: F[Deferred[F, A]]
    //}

    trait Mutex[F[_]] {
      def acquire: F[Unit]
      def release: F[Unit]
    }

    object Mutex {
      type Signal[F[_]] = Deferred[F, Unit]

      case class State[F[_]](locked: Boolean, signals: Queue[Signal[F]])

      private def unlocked[F[_]]: State[F] = State(false, Queue())

      def create[F[_]: Debug](using C: Concurrent[F]): F[Mutex[F]] = C.ref(unlocked[F]).map { state =>
        new Mutex[F] {
          /*
              Change the state of the Ref:
              - if the mutex is currently unlocked, state becomes (true, [])
              - if the mutex is locked, state becomes (true, queue + new-signal) AND WAIT ON THAT SIGNAL
           */
          def acquire: F[Unit] = C.uncancelable { poll =>
            for {
              signal <- C.deferred[Unit]
              cleanup = state.flatModify {
                case State(locked, queue) => State(locked, queue.filterNot(_ eq signal)) -> ("cleanup".pure.dbg >> release)
              }
              pf: (State[F] ?> (State[F], F[Unit])) = ?> {
                case State(false, _) => State(true, Queue()) -> C.unit
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
          def release: F[Unit] = state.flatModify {
              case state @ State(false, _) => (state, C.unit)
              case State(true, Queue()) => (State(false, Queue()), C.unit)
              case State(true, queue) =>
                val (next, rest) = queue.dequeue
                (State(true, rest), next.complete(()).void)
            }
        }
      }
    }

    def mutex[F[_]: {Concurrent, Debug}, A](f: Mutex[F] => F[A]): F[A] = Mutex.create.flatMap(f)
  }

  val concurrentIO = Concurrent[IO] // given instance if Concurrent[IO]

  val aRef = Ref[IO].of(42) // given/implicit Concurrent[IO] in scope
  val aDeferred = Deferred[IO, Int] // given/implicit Concurrent[IO] in scope

  val aRef_v2 = concurrentIO.ref(42)
  val aDeferred_v2 = concurrentIO.deferred[Int]

  // capabilities: pure, map/flatMap, raiseError, uncancelable, stat (fibers), + ref/deferred

  def polymorphicAlarm[F[_]: {Debug, Sleep}](using C: Concurrent[F]): F[Unit] = {
    def tick(time: Ref[F, Int], signal: Deferred[F, Int]): F[Unit] = for {
      _ <- ().pure.sleep(1.second)
      nt <- time.updateAndGet(_ + 1)
      _ <- s"the time is $nt".pure.dbg
      _ <- if (nt == 10) then signal.complete(nt).void else tick(time, signal)
    } yield ()

    def notification(signal: Deferred[F, Int]): F[Unit] =
      "timer started on some other fiber".pure.dbg >> signal.get >> "time's up!".pure.dvoid

    for {
      time <- C.ref(0)
      signal <- C.deferred[Int]
      _ <- notification(signal).start
      _ <- tick(time, signal)
    } yield ()
  }

  /**
   *    Exercise:
   *    1. Generalize racePair
   *    2. Generalize the Mutex primitive for any F
   */

  type RaceResult[F[_], A, B] = Either[
    (Outcome[F, Throwable, A], Fiber[F, Throwable, B]), // (winner outcome, loser fiber)
    (Fiber[F, Throwable, A], Outcome[F, Throwable, B]) // (loser fiber, winner outcome)
  ]

  def generalRacePair[F[_]: Debug, A, B](fa: F[A], fb: F[B])(using C: Concurrent[F]): F[RaceResult[F, A, B]] = for {
    race <- C.deferred[Either[Outcome[F, Throwable, A], Outcome[F, Throwable, B]]].onCancel("Deferred canceled!".pure.dvoid)
    fiba <- fa.guaranteeCase(out => race.complete(Left(out)).void).onCancel("IO-A canceled!".pure.dvoid).start.onCancel("IO-A-fiber canceled!".pure.dvoid)
    fibb <- fb.guaranteeCase(out => race.complete(Right(out)).void).onCancel("IO-B canceled!".pure.dvoid).start.onCancel("IO-B-fiber canceled!".pure.dvoid)
    result <- race.get.onCancel("IO-A vs IO-B race canceled!".pure.dvoid >> fiba.cancel.start >> fibb.cancel.start.void)
  } yield result match {
    case Left(outA) => Left(outA -> fibb)
    case Right(outB) => Right(fiba -> outB)
  }

  def criticalTask[F[_]: {Monad, Sleep}]: F[Int] = Random.nextInt(100).pure.sleep(1.second)

  val ids: List[Int] = 1 toL 10

  def createLockingTask[F[_]: {Monad, Debug, Sleep}](id: Int, mutex: my.Mutex[F]): F[Int] = for {
    _ <- s"[task $id] waiting for permission...".pure.dbg
    _ <- mutex.acquire // blocks if the mutex has been acquired by some other fibre
    // critical section start
    _ <- s"[task $id] working...".pure.dbg
    res <- criticalTask
    _ <- s"[task $id] got result: $res".pure.dbg
    // critical section end
    _ <- s"[task $id] permissions relinquished".pure.dbg
    _ <- mutex.release
  } yield res

  def createCancellingTask[F[_]: {Debug, Sleep}](id: Int, mutex: my.Mutex[F])(
    using
    C: Concurrent[F]
  ): F[Int] =
    if (id % 2 == 0) createLockingTask(id, mutex)
    else for {
      fib <- createLockingTask(id, mutex).onCancel(s"[task $id] received cancellation".pure.dvoid).start
      _ <- C.unit.sleep(2.seconds) >> fib.cancel
      out <- fib.join
      result <- out match {
        case Succeeded(effect) => effect
        case Errored(_) => s"[task $id] errored".pure.dbg.as(-1)
        case Canceled() => s"[task $id] canceled".pure.dbg.as(-2)
      }
    } yield result

  def demoCancellingTask[F[_]: {Concurrent, Parallel, Debug, Sleep}]: F[List[Int]] = my.mutex { mtx => ids.parTraverse(createCancellingTask(_, mtx)) }

  //def run: IO[Unit] = polymorphicAlarm[IO]
  //def run: IO[Unit] = generalRacePair(IO.sleep(750.millis) >> IO("IO-A succeeded"), IO.sleep(500.millis) >> IO("IO-B succeeded")).dbg.void
  //def run: IO[Unit] = generalRacePair(IO.sleep(1000.millis) >> !?("IO-A errored"), IO.sleep(500.millis) >> IO("IO-B succeeded")).dbg.start >>= (fib => IO.sleep(250.millis) >> fib.cancel)
  def run: IO[Unit] = demoCancellingTask[IO].dvoid
}
