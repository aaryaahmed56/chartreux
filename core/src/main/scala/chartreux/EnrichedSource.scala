package chartreux

import akka.NotUsed
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import cats.data.OptionT
import cats.{Applicative, Functor}
import chartreux.util.ErrorSink.{divertErrors, handleErrors}
import chartreux.implicits.Instances.sourceApplicative
import chartreux.Additions.NonEmptySource

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class EnrichedSource[Out](source: Source[Out, NotUsed]) {
  /**
   * Transform this stream by replacing the elements by some specified default value.
   */
  def as[B](b: B): Source[B, NotUsed] =
    Functor[Source[+*, NotUsed]].as(source, b)

  /**
   * Form a stream of pairs by supplying some default value as the left factor.
   */
  def tupleLeft[B](b: B): Source[(B, Out), NotUsed] =
    Functor[Source[+*, NotUsed]].tupleLeft(source, b)

  /**
   * Form a stream of pairs by supplying some default value as the right factor.
   */
  def tupleRight[B](b: B): Source[(Out, B), NotUsed] =
    Functor[Source[+*, NotUsed]].tupleRight(source, b)

  /**
   * Transform this stream into a new stream by applying
   * a function `f` to the elements while preserving the inputs to the function, thereby
   * yielding a stream of tuples where the left factors are the inputs and
   * the right factors are the outputs.
   */
  def fproduct[B](f: Out => B): Source[(Out, B), NotUsed] =
    Functor[Source[+*, NotUsed]].fproduct(source)(f)

  /**
   * Transform this stream into a new stream by applying
   * a function `f` to the elements while preserving the inputs to the function, thereby
   * yielding a stream of tuples where the left factors are the outputs and
   * the right factors are the inputs.
   */
  def fproductLeft[B](f: Out => B): Source[(B, Out), NotUsed] =
    Functor[Source[+*, NotUsed]].fproductLeft(source)(f)

  /**
   * Form a new stream with elements being pairs of elements from two other streams.
   */
  def product[B](snd: Source[B, NotUsed]): Source[(Out, B), NotUsed] =
    Applicative[Source[+*, NotUsed]].product(source, snd)

  /**
   * Infix notation for [[EnrichedSource.product]].
   */
  def <*>[B](snd: Source[B, NotUsed]): Source[(Out, B), NotUsed] =
    Applicative[Source[+*, NotUsed]].product(source, snd)

  /**
   * Form a new stream with elements being triples of elements from three streams.
   */
  def product3[B, C](snd: Source[B, NotUsed], thd: Source[C, NotUsed]): Source[(Out, B, C), NotUsed] = {
    val F = Applicative[Source[+*, NotUsed]]
    val fabc = F.product(F.product(source, snd), thd)
    F.map(fabc) { case ((a, b), c) => (a, b, c) }
  }

  /**
   * Applies the (pure) binary function `g` to this stream with
   * another one, where the values of the inputs of the binary function
   * are of the element types for both streams.
   */
  def map2[B, C](snd: Source[B, NotUsed])(g: (Out, B) => C): Source[C, NotUsed] =
    Applicative[Source[+*, NotUsed]].map2(source, snd)(g)

  /**
   * Applies the (pure) ternary function `g` to this stream with
   * two others, where the values of the inputs of the ternary function
   * are of the element types for all streams.
   */
  def map3[B, C, D](snd: Source[B, NotUsed], thd: Source[C, NotUsed])(g: (Out, B, C) => D): Source[D, NotUsed] =
    Applicative[Source[+*, NotUsed]].map3(source, snd, thd)(g)

  /**
   * Model (expected) fail-fast behavior in this stream according to some partial
   * function that results in a [[scala.util.Left]] modeling error or a [[scala.util.Right]]
   * modeling success, where either is raised typically by way of the `pure` and `raiseError`
   * methods in the [[cats.ApplicativeError]] instance for [[scala.util.Either]].
   */
  def collectOrFail[E, B](pf: PartialFunction[Out, Either[E, B]]): Source[B, NotUsed] =
    source.collect(pf).via(divertErrors)

  /**
   * Model (expected) error handling in this stream according to some partial
   * function that results in a [[scala.util.Left]] modeling error or a [[scala.util.Right]]
   * modeling success, where either is raised typically by way of the `pure` and `raiseError`
   * methods in the [[cats.ApplicativeError]] instance for [[scala.util.Either]], and
   * supplying some default value for any error instance.
   */
  def collectOrHandle[E, B](default: B)(pf: PartialFunction[Out, Either[E, B]]): Source[B, NotUsed] =
    source.collect(pf).via(handleErrors(default))

  /**
   * Evaluate a stream with semantics similar to `runWith(Sink.head)` but with a user-defined
   * [[java.lang.Throwable]] to be 'raised' when failing vis-a-vis the [[cats.MonadError]] instance
   * for [[scala.concurrent.Future]] and `_ >:` [[java.lang.Throwable]]. Optionally accepts
   * a `className` and `methodName` to refer to where the stream evaluation takes place.
   */
  def safeEvalHead(className: Option[String] = None, methodName: Option[String] = None)
                  (implicit mat: Materializer): Future[Out] = {
    (className, methodName) match {
      case (None, None) =>
        OptionT(source.runWith(Sink.headOption)).getOrRaise(new Throwable(
          """Failed stream evaluation due to attempting to access head of empty stream."""))
      case (Some(s), None) =>
        OptionT(source.runWith(Sink.headOption)).getOrRaise(new Throwable(
          s"""`$s`:
             |Failed stream evaluation due to attempting to access head of empty stream.
             |""".stripMargin))
      case (None, Some(t)) =>
        OptionT(source.runWith(Sink.headOption)).getOrRaise(new Throwable(
          s"""`$t`:
             |Failed stream evaluation due to attempting to access head of empty stream.
             |""".stripMargin))
      case (Some(s), Some(t)) =>
        OptionT(source.runWith(Sink.headOption)).getOrRaise(new Throwable(
          s"""`$s`.`$t`:
             |Failed stream evaluation due to attempting to access head of empty stream.
             |""".stripMargin))
    }
  }

  def toNESource(implicit mat: Materializer): Option[NonEmptySource[Out]] =
    NonEmptySource.fromSource(source)

  def toNESourceUnsafe(implicit mat: Materializer): NonEmptySource[Out] =
    NonEmptySource.fromSourceUnsafe(source)
}
