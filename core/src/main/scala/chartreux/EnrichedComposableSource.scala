package chartreux

import akka.NotUsed
import akka.stream.scaladsl.Source
import cats.{Applicative, Functor}
import chartreux.util.CAlg
import chartreux.implicits.Instances.sourceApplicative

case class EnrichedComposableSource[F[_], Out](source: Source[F[Out], NotUsed]) {
  /**
   * Transform this stream of effects into a new stream of effects by applying a function `f`
   * to the elements. `F[_]` must be a [[cats.Functor]].
   */
  def mapF[B](f: Out => B)(implicit functor: Functor[F]): Source[F[B], NotUsed] =
    CAlg[Source[+*, NotUsed], F].cmp.mapF(source)(f)

  /**
   * Given a stream of effects and some default value, form a new stream of effects
   * replacing the elements by this value. `F[_]` must be a [[cats.Functor]].
   */
  def asF[B](b: B)(implicit functor: Functor[F]): Source[F[B], NotUsed] =
    CAlg[Source[+*, NotUsed], F].cmp.asF(source)(b)

  /**
   * Form a new stream of effects with the data now tupled with some
   * default value as the left factor. `F[_]` must be a [[cats.Functor]].
   */
  def tupleLeftF[B](b: B)(implicit functor: Functor[F]): Source[F[(B, Out)], NotUsed] =
    CAlg[Source[+*, NotUsed], F].cmp.tupleLeftF(source)(b)

  /**
   * Form a new stream of effects with the data now tupled with some
   * default value as the right factor. `F[_]` must be a [[cats.Functor]].
   */
  def tupleRightF[B](b: B)(implicit functor: Functor[F]): Source[F[(Out, B)], NotUsed] =
    CAlg[Source[+*, NotUsed], F].cmp.tupleRightF(source)(b)

  /**
   * Transform this stream of effects into a new stream of effects by applying
   * a function `f` to the elements while preserving the inputs to the function, thereby
   * yielding an effectful stream of tuples where the left factors are the inputs and
   * the right factors are the outputs. `F[_]` must be a [[cats.Functor]].
   */
  def fproductF[B](f: Out => B)(implicit functor: Functor[F]): Source[F[(Out, B)], NotUsed] =
    CAlg[Source[+*, NotUsed], F].cmp.fproductF(source)(f)

  /**
   * Transform this stream of effects into a new stream of effects by applying
   * a function `f` to the elements while preserving the inputs to the function, thereby
   * yielding an effectful stream of tuples where the left factors are the outputs and
   * the right factors are the inputs. `F[_]` must be a [[cats.Functor]].
   */
  def fproductLeftF[B](f: Out => B)(implicit functor: Functor[F]): Source[F[(B, Out)], NotUsed] =
    CAlg[Source[+*, NotUsed], F].cmp.fproductLeftF(source)(f)

  /**
   * Given two streams of effects, form a new stream of effects with pairs of elements
   * from the original streams. `F[_]` must be an [[cats.Applicative]].
   */
  def productF[B](snd: Source[F[B], NotUsed])
                 (implicit applicative: Applicative[F]): Source[F[(Out, B)], NotUsed] =
    CAlg[Source[+*, NotUsed], F].cmp.productF(source, snd)

  /**
   * Given three streams of effects, form a new stream of effects with triples of elements
   * from the original streams. `F[_]` must be an [[cats.Applicative]].
   */
  def product3F[B, C](snd: Source[F[B], NotUsed], thd: Source[F[C], NotUsed])
                     (implicit applicative: Applicative[F]): Source[F[(Out, B, C)], NotUsed] =
    CAlg[Source[+*, NotUsed], F].cmp.product3F(source, snd, thd)

  /**
   * Given a stream of effects of one element type with another, and a (pure) binary
   * function mapping these element types to another, form a new stream of effects
   * with the resultant element type. `F[_]` must be an [[cats.Applicative]].
   */
  def map2F[B, C](snd: Source[F[B], NotUsed])(g: (Out, B) => C)
                 (implicit applicative: Applicative[F]): Source[F[C], NotUsed] =
    CAlg[Source[+*, NotUsed], F].cmp.map2F(source, snd)(g)

  /**
   * Given two other streams of effects, and a (pure) ternary
   * function mapping the element types to another, form a new stream of effects
   * with the resultant element type. `F[_]` must be an [[cats.Applicative]].
   */
  def map3F[B, C, D](snd: Source[F[B], NotUsed], thd: Source[F[C], NotUsed])(g: (Out, B, C) => D)
                    (implicit applicative: Applicative[F]): Source[F[D], NotUsed] =
    CAlg[Source[+*, NotUsed], F].cmp.map3F(source, snd, thd)(g)
}
