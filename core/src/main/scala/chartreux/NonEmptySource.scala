package chartreux

import akka.NotUsed
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import cats.Eval
import cats.data.OneAnd
import cats.implicits.catsSyntaxOptionId
import chartreux.Additions.NonEmptySource

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success, Try}

object NonEmptySource {
  /**
   * Create a `NonEmptySource` by supplying some element as the head and some [[akka.stream.scaladsl.Source]]
   * as the tail.
   */
  def apply[A](head: A, tail: Source[A, NotUsed]): NonEmptySource[A] = OneAnd[Source[+*, NotUsed], A](head, tail)

  /**
   * Create an optional `NonEmptySource` from a thunk which evaluates to an [[Iterator]]. The result will be a [[None]]
   * if, upon evaluation, the [[Iterator]] is empty.
   */
  def fromIterator[A](thunk: () => Iterator[A]): Option[NonEmptySource[A]] = {
    val evalit = Eval.later(thunk)
    if (evalit.value().isEmpty) None else {
      val head: A = evalit.value().slice(0, 1).toList.head
      val tail: () => Iterator[A] = () => evalit.value().slice(1, evalit.value().size - 1)
      NonEmptySource(head, Source.fromIterator(tail)).some
    }
  }

  /**
   * Create a `NonEmptySource` from a thunk which evaluates to an [[Iterator]], or throw an [[java.lang.IllegalArgumentException]]
   * if, upon evaluation, the [[Iterator]] is empty.
   */
  def fromIteratorUnsafe[A](thunk: () => Iterator[A]): NonEmptySource[A] = {
    val evalit = Eval.later(thunk)
    if (evalit.value().isEmpty) throw new IllegalArgumentException("Cannot construct `NonEmptySource` without elements.") else {
      val head: A = evalit.value().slice(0, 1).toList.head
      val tail: () => Iterator[A] = () => evalit.value().slice(1, evalit.value().size - 1)
      NonEmptySource(head, Source.fromIterator(tail))
    }
  }

  /**
   * Create an optional `NonEmptySource` from an arbitrary number of elements. The result will be a [[None]]
   * if no elements are supplied.
   */
  def fromElements[A](elem: A*): Option[NonEmptySource[A]] =
    if (elem.isEmpty) None else
      NonEmptySource(elem.head, Source.fromIterator(() => Iterator.from(elem.tail))).some

  /**
   * Create a `NonEmptySource` from an arbitrary number of elements, or throw an [[java.lang.IllegalArgumentException]]
   * if no elements are supplied.
   */
  def fromElementsUnsafe[A](elem: A*): NonEmptySource[A] =
  if (elem.isEmpty) throw new IllegalArgumentException("Cannot construct `NonEmptySource` without elements.") else
    NonEmptySource(elem.head, Source.fromIterator(() => Iterator.from(elem.tail)))

  /**
   * Create a `NonEmptySource` with a single element.
   */
  def single[A](elem: A): NonEmptySource[A] = NonEmptySource(elem, Source.empty[A])

  /**
   * Lazily create a `NonEmptySource` with a thunk, which, upon evaluation, will produce
   * a single element.
   */
  def lazySingle[A](thunk: () => A): NonEmptySource[A] = NonEmptySource(Eval.later(thunk).value(), Source.empty[A])

  /**
   * Construct an optional `NonEmptySource` from a [[akka.stream.scaladsl.Source]]. The result will be a [[None]] if
   * upon materializing the [[akka.stream.scaladsl.Source]], we find that the resultant [[scala.concurrent.Future]]
   * failed with some exception, or the [[akka.stream.scaladsl.Source]] was empty.
   *
   * This is a blocking operation, i.e. we use [[scala.concurrent.Await]] to suspend the main thread for a
   * maximum of five seconds to (possibly) acquire the result from the [[scala.concurrent.Future]]. As such,
   * although this is a safe method, it is recommended to invoke it sparingly. Construct `NonEmptySource`s
   * using its other builder methods (ideally the safe variants, but if you know for a fact that you aren't
   * attempting to build `NonEmptySource`s without elements, and handling [[Option]]s proves to be a pain,
   * then invoking the `unsafe` variants is fine.
   */
  def fromSource[A](src: Source[A, NotUsed])(implicit materializer: Materializer): Option[NonEmptySource[A]] = {
    val seq: Future[Seq[A]] = src.runWith(Sink.seq[A])

    val res: Try[Seq[A]] = Try(Await.result(seq, FiniteDuration(5, TimeUnit.SECONDS)))

    res match {
      case Failure(_) => None
      case Success(elems) =>
        elems match {
          case x::ys => NonEmptySource(x, Source.fromIterator(() => Iterator.from(ys))).some
          case Nil => None
        }
    }
  }

  /**
   * Construct a `NonEmptySource` from a [[akka.stream.scaladsl.Source]] unsafely, in which several
   * exceptions may be thrown on account of [[scala.concurrent.Future]] failure status or
   * possible emptiness of the initial [[akka.stream.scaladsl.Source]].
   *
   * This is a blocking operation, i.e. we use [[scala.concurrent.Await]] to suspend the main thread for a
   * maximum of five seconds to (possibly) acquire the result from the [[scala.concurrent.Future]].
   * It is not generally recommended to invoke this method unless we know for a fact that the
   * [[akka.stream.scaladsl.Source]] we are attempting to build the `NonEmptySource` from is non-empty,
   * and we are invoking it sparingly enough so that the `cost` of performing a blocking operation
   * is diversified. Construct `NonEmptySource`s using its other builder methods (ideally the safe variants,
   * but if you know for a fact that you aren't attempting to build `NonEmptySource`s without elements,
   * and handling [[Option]]s proves to be a pain, then invoking the `unsafe` variants is fine.
   */
  def fromSourceUnsafe[A](src: Source[A, NotUsed])(implicit mat: Materializer): NonEmptySource[A] = {
    val seq: Future[Seq[A]] = src.runWith(Sink.seq[A])

    val res: Try[Seq[A]] = Try(Await.result(seq, FiniteDuration(5, TimeUnit.SECONDS)))

    res match {
      case Failure(exception) => throw exception
      case Success(elems) =>
        elems match {
          case x::ys => NonEmptySource(x, Source.fromIterator(() => Iterator.from(ys)))
          case Nil => throw new IllegalArgumentException("Cannot construct a `NonEmptySource` without elements.")
        }
    }
  }
}
