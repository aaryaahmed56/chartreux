# `chartreux`

A minimal library of resiliency-focused as well as useful and/or quality of life-enhancing (i.e. boilerplate-minimizing) primitives vis-a-vis combining frameworks/libraries that operate on reactive streaming semantics (i.e. [`akka-streams`](https://doc.akka.io/docs/akka/current/stream/index.html), [`mongo-scala-driver`](https://www.mongodb.com/docs/drivers/scala/)) with [`cats`](https://typelevel.org/cats/). This library is purely functional in that it is algebra-driven in its design, and is informed by the [`tagless-final`](https://okmij.org/ftp/tagless-final/) approach as well as [library pimping](https://www.artima.com/weblogs/viewpost.jsp?thread=179766).

## Example Usage
```scala
/**
 * `chartreux` aims to be modular, but for convenience, all functionality can be accessed
 * via these imports.
 */
import chartreux._
import chartreux.implicits.Instances._
import chartreux.Additions._

import akka.NotUsed
import akka.stream.scaladsl.{Sink, Source}
import cats.ApplicativeError
import cats.implicits.catsSyntaxOptionId

import scala.concurrent.Future

object ChartreuxTesting {
  /**
   * It is often that we encounter nested effects in actual practice,
   * i.e. product types (case classes, tuples) with optional values,
   * `Flow[A, Option[B], NotUsed]`, `Source[Either[A, B]]`, `Future[Option[A]]`.
   * We generalize over this phenomenon and encapsulate a set of useful functions that
   * are preserved over this nesting to minimize boilerplate.
   */
  val pair: (Option[Int], String) = (28.some, "Asef")

  val futName: Future[Option[String]] = Future.successful("Asef".some)
  val futFullName: Future[Option[String]] = futName.mapF(firstName => s"$firstName Ahmed")

  val srcName: Source[Either[String, Int], NotUsed] = Source.lazySingle(() => Left("Asef"))
  val srcAge: Source[Either[String, Int], NotUsed] = Source.lazySingle(() => Right(28))

  val m2src: Source[Either[String, Int], NotUsed] = srcName.map2F(srcAge) { case (_, t) => t }

  /**
   * The so-called `reactive` paradigm, and in association the "reactive streaming" (`rx-streams`) initiative,
   * as it is delineated semantically and implemented by frameworks like `akka`/`akka-streams`,
   * is complicated, mutable, and unsafe. We attempt to circumvent this inasmuch as it is possible and practicable
   * by adopting cues from functional programming - though it is unfortunately not possible to reign in all concepts and
   * implements from functional effect systems/runtimes like `cats-effect`, `zio`, etc.
   */

  /**
   * Here is how exception raising/handling to beget fail-fast behavior is done idiomatically in `akka-streams`.
   * In simple scenarios, like the one below, this works fine, but if there are several side effects present,
   * this can fail to work as we expect.
   */
  val failingStream: Source[Int, NotUsed] =
    Source
      .fromIterator(() => Iterator.from(1 to 1000))
      .collect {
        case v if v <= 100 => v
        // Nothing is stopping us from raising an exception, i.e. something which isn't merely
        // partial data transformation of the kind Int => T, in this `collect` method call.
        case _ => throw new IllegalArgumentException("We don't want integers above 100 in this stream!")
      }.recoverWithRetries(0, {
      case _ => Source.empty[Int]
    })

  /**
   * Here's how we can simulate type/data-driven fail-fast behavior which will _always_ work as expected
   * regardless of what side effects are present in this streamlet.
   *
   * A new method, i.e. `collectOrFail`, accepts an [[Either]] value for either success or failure, vis-a-vis
   * representing errors by a sum type (in this example, `ServiceError`, but this can be tailored
   * naming-wise to particular services as they are defined) which may be extended with new factors as we see fit,
   * i.e. [[Either]]`[ServiceError, A]`. [[Left]] values are diverted to a [[akka.streams.scaladsl.Sink.ignore]].
   */
  sealed trait ServiceError
  final case object IllegalArgumentError extends ServiceError

  val serviceError = ApplicativeError[Either[ServiceError, +*], ServiceError]

  val failingStreamSafe: Source[Int, NotUsed] =
    Source
      .fromIterator(() => Iterator.from(1 to 1000))
      .collectOrFail {
        // We represent success and failure as pure data transformation in this
        // `collect` method call.
        case v if v <= 100 => serviceError.pure(v)
        case _ => serviceError.raiseError(IllegalArgumentError)
      }

  /**
   * We introduce a new type [[TOr]] which is meant to model the possibility of a
   * type being exclusively one of three types, and where data transformation
   * can occur among the right-most channels (unlike [[Either]], which is a
   * binary disjoint union, in which (idiomatically) data transformation can only occur
   * in the right-most channel.
   */
  val tnameFst: TOr[Nothing, String, Int] = Snd("Asef")
  val tAge: TOr[Nothing, String, Int] = Thd(28)

  val tfullName: TOr[Nothing, String, Int] =
    tnameFst.leftMap(name => s"$name Ahmed")

  val thexEncodedAge: TOr[Nothing, String, String] =
    tAge.map(_.toHexString)

  val doubleAge: TOr[Nothing, String, String] =
    thexEncodedAge.map2L(thexEncodedAge) { case (s, t) => s + t }

  val combineNameAndAge: TOr[Nothing, String, String] =
    tfullName.map2(thexEncodedAge) { case (u, t) => s"$t is $u." }

  /**
   * Building [[TOr]] values from [[Either]] and [[Option]] values. This is convenient
   * for the fact that we can exploit [[TOr]] as a means of handling [[Either]]
   * and [[Option]] values in a streamlined way.
   */
  val employeeNum: Either[Int, String] = Left(3843843)
  val employeeName: Either[Int, String] = Right("Asef Ahmed")
  val employeeAge: Option[Int] = 28.some

  val temployeeNum: TOr[Nothing, Int, String] = TOr.fromEither(employeeNum)
  val temployeeName: TOr[Nothing, Int, String] = TOr.fromEither(employeeName)
  val temployeeAge: TOr[Nothing, Unit, Int] = TOr.fromOption(employeeAge)

  val sentence: TOr[Nothing, Int, String] =
    for {
      num  <- temployeeNum
      name <- temployeeName
      // Necessary to have the types line up in this `for-comprehension`.
      age  <- temployeeAge.leftMap(_ => 0)
    } yield s"Employee $num, i.e. $name, is $age years old."


  /**
   * We introduce another new type, i.e. [[NonEmptySource]], which enforces
   * at the type level that an `akka-streams` [[akka.stream.scaladsl.Source]]
   * must contain at least a single element. This means that operations that are neither
   * safe nor total for regular [[akka.stream.scaladsl.Source]]s, e.g. acquiring
   * the head of a stream, as well as operations like `reduce` (tantamount to connecting
   * the [[NonEmptySource]] to a [[akka.stream.scaladsl.Sink.head]] or a
   * [[akka.stream.scaladsl.Sink.reduce]]) are now guaranteed to be both.
   * Moreover, this means that 'emptiness checking' can now be localized to
   * wherever we encounter an empty streamlet (by attempting to build a [[NonEmptySource]]
   * from a [[akka.stream.scaladsl.Source]]).
   *
   * The API we expose to build a [[NonEmptySource]] is similar to regular [[akka.stream.scaladsl.Source]]s,
   * but with `unsafe` variants for iterators, varargs, etc. indicating which methods might except.
   */

  val nesrcName: NonEmptySource[String] = NonEmptySource.lazySingle(() => "Asef")
  val nesrcInts: NonEmptySource[Int] =
    NonEmptySource.fromIteratorUnsafe(() => Iterator.from(1 to 1000))
      .collect {
        case v if v <= 100 => v + 2
      }

  val nesrcPairs: NonEmptySource[(String, Int)] = nesrcName <*> nesrcInts

  val nesrcPairsHd: (String, Int) = nesrcPairs.head

  val nesrcPairsRunHead: Future[(String, Int)] = nesrcPairs.runWith(Sink.head)
  val nesrcPairsRunReduce: Future[(String, Int)] = nesrcPairs.runReduce { case ((n0, i0), (_, i1)) =>  (n0, i0 + i1) }
}
```