package chartreux

import akka.stream.scaladsl.Source
import akka.stream.{Graph, Materializer, SinkShape}
import akka.{Done, NotUsed}
import cats.{Applicative, Functor}
import chartreux.Additions.NonEmptySource
import chartreux.implicits.Instances.neSourceApplicative

import scala.concurrent.Future

case class EnrichedNonEmptySource[Out](nesrc: NonEmptySource[Out]) {
  private val nesrchd: Out = nesrc.head

  private val nesrctail: Source[Out, NotUsed] = nesrc.tail

  /**
   * Turn this `NonEmptySource` into a [[akka.stream.scaladsl.Source]].
   */
  def toSource: Source[Out, NotUsed] =
    nesrctail.prependLazy(Source.single(nesrchd))

  /**
   * Transform this stream by replacing the elements by some specified default value.
   */
  def as[B](default: B): NonEmptySource[B] =
    Functor[NonEmptySource].as(nesrc, default)

  /**
   * Form a stream of pairs by supplying some default value as the left factor.
   */
  def tupleLeft[B](b: B): NonEmptySource[(B, Out)] =
    Functor[NonEmptySource].tupleLeft(nesrc, b)

  /**
   * Form a stream of pairs by supplying some default value as the right factor.
   */
  def tupleRight[B](b: B): NonEmptySource[(Out, B)] =
    Functor[NonEmptySource].tupleRight(nesrc, b)

  /**
   * Transform this stream into a new stream by applying
   * a function `f` to the elements while preserving the inputs to the function, thereby
   * yielding a stream of tuples where the left factors are the inputs and
   * the right factors are the outputs.
   */
  def fproduct[B](f: Out => B): NonEmptySource[(Out, B)] =
    Functor[NonEmptySource].fproduct(nesrc)(f)

  /**
   * Transform this stream into a new stream by applying
   * a function `f` to the elements while preserving the inputs to the function, thereby
   * yielding a stream of tuples where the left factors are the outputs and
   * the right factors are the inputs.
   */
  def fproductLeft[B](f: Out => B): NonEmptySource[(B, Out)] =
    Functor[NonEmptySource].fproductLeft(nesrc)(f)

  /**
   * Form a new stream with elements being pairs of elements from two other streams.
   */
  def product[B](snd: NonEmptySource[B]): NonEmptySource[(Out, B)] =
    Applicative[NonEmptySource].product(nesrc, snd)

  /**
   * Concatenate the given `NonEmptySource` to this `NonEmptySource`, meaning that
   * once this `NonEmptySource`’s input is exhausted and all result elements have been generated,
   * the `NonEmptySource`’s elements will be produced.
   * If this `NonEmptySource` gets upstream error - no elements from the given Source will be pulled.
   */
  def concatLazy(snd: NonEmptySource[Out]): NonEmptySource[Out] = {
    val sndhd: Out = snd.head
    val sndtail: Source[Out, NotUsed] = snd.tail
    NonEmptySource(nesrchd, nesrctail.concatLazy(sndtail.prependLazy(Source.single(sndhd))))
  }

  /**
   * Transform this stream by applying the given partial function to each of the elements on which the function is
   * defined as they pass through this processing step. Non-matching elements are filtered out.
   */
  def collect[B](pf: PartialFunction[Out, B]): NonEmptySource[B] =
    NonEmptySource(pf(nesrchd), nesrctail.collect(pf))

  /**
   * Infix notation for [[EnrichedNonEmptySource.product]].
   */
  def <*>[B](snd: NonEmptySource[B]): NonEmptySource[(Out, B)] =
    Applicative[NonEmptySource].product(nesrc, snd)

  /**
   * Applies the (pure) binary function `g` to this stream with
   * another one, where the values of the inputs of the binary function
   * are of the element types for both streams.
   */
  def map2[B, C](snd: NonEmptySource[B])(f: (Out, B) => C): NonEmptySource[C] =
    Applicative[NonEmptySource].map2(nesrc, snd)(f)

  /**
   * Connect this `NonEmptySource` to a [[akka.stream.scaladsl.Sink]] and run it. The returned value
   * is the materialized value of the Sink, e.g. the Publisher of a #publisher.
   * Note that the [[akka.actor.ActorSystem]] can be used as the implicit materializer parameter
   * to use the [[akka.stream.SystemMaterializer]] for running the stream.
   */
  def runWith[Mat2](sink: Graph[SinkShape[Out], Mat2])(implicit materializer: Materializer): Mat2 =
    toSource.runWith(sink)

  /**
   * Connect this `NonEmptySource` to the [[akka.stream.scaladsl.Sink.ignore]] and run it. Elements
   * from the stream will be consumed and discarded. Note that the [[akka.actor.ActorSystem]] can be used
   * as the implicit materializer parameter to use the [[akka.stream.SystemMaterializer]] for running the stream.
   */
  def run()(implicit materializer: Materializer): Future[Done] =
    toSource.run()

  /**
   * Shortcut for running this `NonEmptySource` with a `foreach` procedure. The given procedure
   * is invoked for each received element. The returned Future will be completed with Success when
   * reaching the normal end of the stream, or completed with Failure if there is a failure signaled in the stream.
   * Note that the [[akka.actor.ActorSystem]] can be used as the implicit materializer parameter
   * to use the [[akka.stream.SystemMaterializer]] for running the stream.
   */
  def runForeach(f: Out => Unit)(implicit materializer: Materializer): Future[Done] =
    toSource.runForeach(f)

  /**
   * Shortcut for running this `NonEmptySource` with a `fold` function. The given function is invoked
   * for every received element, giving it its previous output (or the given zero value) and the element as input.
   * The returned [[scala.concurrent.Future]] will be completed with value of the final function evaluation when
   * the input stream ends, or completed with Failure if there is a failure signaled in the stream.
   * Note that the [[akka.actor.ActorSystem]] can be used as the implicit materializer parameter
   * to use the [[akka.stream.SystemMaterializer]] for running the stream.
   */
  def runFold[U](zero: U)(f: (U, Out) => U)(implicit materializer: Materializer): Future[U] =
    toSource.runFold(zero)(f)

  /**
   * Shortcut for running this `NonEmptySource` with a `reduce` function. The given function is invoked
   * for every received element, giving it its previous output (from the second element) and the element as input.
   * The returned [[scala.concurrent.Future]] will be completed with value of the final function evaluation when
   * the input stream ends, or completed with Failure if there is a failure signaled in the stream.
   *
   * Unlike for regular [[akka.stream.scaladsl.Source]]s, as `NonEmptySource`s are guaranteed by construction to
   * be non-empty, invoking this method will always be safe (whereas invoking this method on a [[akka.stream.scaladsl.Source]]
   * can result in a [[NoSuchElementException]] if it is empty).
   */
  def runReduce[U >: Out](f: (U, U) => U)(implicit materializer: Materializer): Future[U] =
    toSource.runReduce(f)
}
