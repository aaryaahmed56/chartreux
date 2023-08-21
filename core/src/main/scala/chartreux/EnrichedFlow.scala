package chartreux

import akka.NotUsed
import akka.stream.scaladsl.Flow
import akka.stream.{ Graph, SourceShape }
import cats.{Applicative, Functor}
import chartreux.util.ErrorSink.{ divertErrors, handleErrors }
import chartreux.implicits.Instances.flowApplicative

case class EnrichedFlow[In, Out](flow: Flow[In, Out, NotUsed]) {
  /**
   * Transform this stream by replacing the elements by some specified default value.
   */
  def as[B](b: B): Flow[In, B, NotUsed] = Functor[Flow[In, +*, NotUsed]].as(flow, b)

  /**
   * Form a stream of pairs by supplying some default value as the left factor.
   */
  def tupleLeft[B](b: B): Flow[In, (B, Out), NotUsed] = Functor[Flow[In, +*, NotUsed]].tupleLeft(flow, b)

  /**
   * Form a stream of pairs by supplying some default value as the right factor.
   */
  def tupleRight[B](b: B): Flow[In, (Out, B), NotUsed] = Functor[Flow[In, +*, NotUsed]].tupleRight(flow, b)

  /**
   * Transform this stream into a new stream by applying
   * a function `f` to the elements while preserving the inputs to the function, thereby
   * yielding a stream of tuples where the left factors are the inputs and
   * the right factors are the outputs.
   */
  def fproduct[B](f: Out => B): Flow[In, (Out, B), NotUsed] =
    Functor[Flow[In, +*, NotUsed]].fproduct(flow)(f)

  /**
   * Transform this stream into a new stream by applying
   * a function `f` to the elements while preserving the inputs to the function, thereby
   * yielding a stream of tuples where the left factors are the outputs and
   * the right factors are the inputs.
   */
  def fproductLeft[B](f: Out => B): Flow[In, (B, Out), NotUsed] =
    Functor[Flow[In, +*, NotUsed]].fproductLeft(flow)(f)

  /**
   * Form a new stream with elements being pairs of elements from two other streams.
   */
  def product[C](snd: Flow[In, C, NotUsed]): Flow[In, (Out, C), NotUsed] =
    Applicative[Flow[In, +*, NotUsed]].product(flow, snd)

  /**
   * Form a new stream with elements being triples of elements from three other streams.
   */
  def product3[C, D](snd: Flow[In, C, NotUsed], thd: Flow[In, D, NotUsed]): Flow[In, (Out, C, D), NotUsed] = {
    val F = Applicative[Flow[In, +*, NotUsed]]
    val fabc = F.product(F.product(flow, snd), thd)
    F.map(fabc) { case ((a, b), c) => (a, b, c) }
  }

  /**
   * Applies the (pure) binary function `g` to this stream with
   * another one, where the values of the inputs of the binary function
   * are of the element types for both streams.
   */
  def map2[B, C](snd: Flow[In, B, NotUsed])(g: (Out, B) => C): Flow[In, C, NotUsed] =
    Applicative[Flow[In, +*, NotUsed]].map2(flow, snd)(g)

  /**
   * Applies the (pure) ternary function `g` to this stream with
   * another one, where the values of the inputs of the binary function
   * are of the element types for both streams.
   */
  def map3[B, C, D](snd: Flow[In, B, NotUsed], thd: Flow[In, C, NotUsed])(g: (Out, B, C) => D): Flow[In, D, NotUsed] =
    Applicative[Flow[In, +*, NotUsed]].map3(flow, snd, thd)(g)

  /**
   * Do a `recoverWithRetries` with `attempts` being 0 to do (unsafe and not referentially transparent) error
   * recovery in `akka-streams`.
   */
  def catchSome(pf: PartialFunction[Throwable, Graph[SourceShape[Out], NotUsed]]): Flow[In, Out, NotUsed] =
    flow.recoverWithRetries(0, pf)

  /**
   * Model (expected) fail-fast behavior in this stream according to some partial
   * function that results in a [[scala.util.Left]] modeling error or a [[scala.util.Right]]
   * modeling success, where either is raised typically by way of the `pure` and `raiseError`
   * methods in the [[cats.ApplicativeError]] instance for [[scala.util.Either]].
   */
  def collectOrFail[E, B](pf: PartialFunction[Out, Either[E, B]]): Flow[In, B, NotUsed] =
    flow.collect(pf).via(divertErrors)

  /**
   * Model (expected) error handling in this stream according to some partial
   * function that results in a [[scala.util.Left]] modeling error or a [[scala.util.Right]]
   * modeling success, where either is raised typically by way of the `pure` and `raiseError`
   * methods in the [[cats.ApplicativeError]] instance for [[scala.util.Either]], and
   * supplying some default value for any error instance.
   */
  def collectOrHandle[E, B](default: B)(pf: PartialFunction[Out, Either[E, B]]): Flow[In, B, NotUsed] =
    flow.collect(pf).via(handleErrors(default))
}
