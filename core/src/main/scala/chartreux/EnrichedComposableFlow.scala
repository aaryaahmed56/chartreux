package chartreux

import akka.NotUsed
import akka.stream.scaladsl.Flow
import cats.{ Applicative, Functor }
import chartreux.util.Aliases.PureFlow
import chartreux.util.CAlg
import chartreux.implicits.Instances.flowApplicative

case class EnrichedComposableFlow[F[_], In, Out](flow: PureFlow[In, F[Out]]) {
  /**
   * Transform this stream of effects into a new stream of effects by applying a function `f`
   * to the elements. `F[_]` must be a [[cats.Functor]].
   */
  def mapF[B](f: Out => B)(implicit functor: Functor[F]): PureFlow[In, F[B]] =
    CAlg[Flow[In, +*, NotUsed], F].cmp.mapF(flow)(f)

  /**
   * Given a stream of effects and some default value, form a new stream of effects
   * replacing the elements by this value. `F[_]` must be a [[cats.Functor]].
   */
  def asF[B](b: B)(implicit functor: Functor[F]): PureFlow[In, F[B]] =
    CAlg[Flow[In, +*, NotUsed], F].cmp.asF(flow)(b)

  /**
   * Form a new stream of effects with the data now tupled with some
   * default value as the left factor. `F[_]` must be a [[cats.Functor]].
   */
  def tupleLeftF[B](b: B)(implicit functor: Functor[F]): PureFlow[In, F[(B, Out)]] =
    CAlg[Flow[In, +*, NotUsed], F].cmp.tupleLeftF(flow)(b)

  /**
   * Form a new stream of effects with the data now tupled with some
   * default value as the right factor. `F[_]` must be a [[cats.Functor]].
   */
  def tupleRightF[B](b: B)(implicit functor: Functor[F]): PureFlow[In, F[(Out, B)]] =
    CAlg[Flow[In, +*, NotUsed], F].cmp.tupleRightF(flow)(b)

  /**
   * Transform this stream of effects into a new stream of effects by applying
   * a function `f` to the elements while preserving the inputs to the function, thereby
   * yielding an effectful stream of tuples where the left factors are the inputs and
   * the right factors are the outputs. `F[_]` must be a [[cats.Functor]].
   */
  def fproductF[B](f: Out => B)(implicit functor: Functor[F]): PureFlow[In, F[(Out, B)]] =
    CAlg[Flow[In, +*, NotUsed], F].cmp.fproductF(flow)(f)

  /**
   * Transform this stream of effects into a new stream of effects by applying
   * a function `f` to the elements while preserving the inputs to the function, thereby
   * yielding an effectful stream of tuples where the left factors are the outputs and
   * the right factors are the inputs. `F[_]` must be a [[cats.Functor]].
   */
  def fproductLeftF[B](f: Out => B)(implicit functor: Functor[F]): PureFlow[In, F[(B, Out)]] =
    CAlg[Flow[In, +*, NotUsed], F].cmp.fproductLeftF(flow)(f)

  /**
   * Given two streams of effects, form a new stream of effects with pairs of elements
   * from the original streams. `F[_]` must be an [[cats.Applicative]].
   */
  def productF[B](that: PureFlow[In, F[B]])
                 (implicit applicative: Applicative[F]): PureFlow[In, F[(Out, B)]] =
    CAlg[Flow[In, +*, NotUsed], F].cmp.productF(flow, that)

  /**
   * Given three streams of effects, form a new stream of effects with triples of elements
   * from the original streams. `F[_]` must be an [[cats.Applicative]].
   */
  def product3F[B, C](snd: PureFlow[In, F[B]], thd: PureFlow[In, F[C]])
                     (implicit applicative: Applicative[F]): PureFlow[In, F[(Out, B, C)]] =
    CAlg[Flow[In, +*, NotUsed], F].cmp.product3F(flow, snd, thd)

  /**
   * Given another stream of effects, and a (pure) binary
   * function mapping the element types to another, form a new stream of effects
   * with the resultant element type. `F[_]` must be an [[cats.Applicative]].
   */
  def map2F[B, C](snd: PureFlow[In, F[B]])(g: (Out, B) => C)
                 (implicit applicative: Applicative[F]): PureFlow[In, F[C]] =
    CAlg[Flow[In, +*, NotUsed], F].cmp.map2F(flow, snd)(g)

  /**
   * Given two other streams of effects, and a (pure) ternary
   * function mapping the element types to another, form a new stream of effects
   * with the resultant element type. `F[_]` must be an [[cats.Applicative]].
   */
  def map3F[B, C, D](snd: PureFlow[In, F[B]], thd: PureFlow[In, F[C]])(g: (Out, B, C) => D)
                    (implicit applicative: Applicative[F]): PureFlow[In, F[D]] =
    CAlg[Flow[In, +*, NotUsed], F].cmp.map3F(flow, snd, thd)(g)
}
