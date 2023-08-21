package chartreux

import cats.implicits.catsStdInstancesForFuture
import cats.{ Applicative, Functor }
import chartreux.util.CAlg

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class EnrichedComposableFuture[F[_], Out](future: Future[F[Out]]) {
  /**
   * Transform this [[scala.concurrent.Future]] of effects into a new
   * [[scala.concurrent.Future]] of effects by applying a function `f`
   * to the elements. `F[_]` must be a [[cats.Functor]].
   */
  def mapF[B](f: Out => B)(implicit functor: Functor[F]): Future[F[B]] =
    CAlg[Future, F].cmp.mapF(future)(f)

  /**
   * Given a [[scala.concurrent.Future]] of effects and some default value, form a new
   * [[scala.concurrent.Future]] of effects
   * replacing the elements by this value. `F[_]` must be a [[cats.Functor]].
   */
  def asF[B](b: B)(implicit functor: Functor[F]): Future[F[B]] =
    CAlg[Future, F].cmp.asF(future)(b)

  /**
   * Form a new [[scala.concurrent.Future]] of effects with the data now tupled with some
   * default value as the left factor. `F[_]` must be a [[cats.Functor]].
   */
  def tupleLeftF[B](b: B)(implicit functor: Functor[F]): Future[F[(B, Out)]] =
    CAlg[Future, F].cmp.tupleLeftF(future)(b)

  /**
   * Form a new [[scala.concurrent.Future]] of effects with the data now tupled with some
   * default value as the right factor. `F[_]` must be a [[cats.Functor]].
   */
  def tupleRightF[B](b: B)(implicit functor: Functor[F]): Future[F[(Out, B)]] =
    CAlg[Future, F].cmp.tupleRightF(future)(b)

  /**
   * Transform this [[scala.concurrent.Future]] of effects into a new [[scala.concurrent.Future]]
   * of effects by applying a function `f` to the elements while preserving the input/s to the function,
   * thereby yielding an effectful [[scala.concurrent.Future]] of tuples where the left factors are
   * the inputs and the right factors are the outputs. `F[_]` must be a [[cats.Functor]].
   */
  def fproductF[B](f: Out => B)(implicit functor: Functor[F]): Future[F[(Out, B)]] =
    CAlg[Future, F].cmp.fproductF(future)(f)

  /**
   * Transform this [[scala.concurrent.Future]] of effects into a new [[scala.concurrent.Future]]
   * of effects by applying a function `f` to the elements while preserving the inputs to the function,
   * thereby yielding an effectful [[scala.concurrent.Future]] of tuples where the left factors are
   * the outputs and the right factors are the inputs. `F[_]` must be a [[cats.Functor]].
   */
  def fproductLeftF[B](f: Out => B)(implicit functor: Functor[F]): Future[F[(B, Out)]] =
    CAlg[Future, F].cmp.fproductLeftF(future)(f)

  /**
   * Given two [[scala.concurrent.Future]]s of effects, form a new [[scala.concurrent.Future]]
   * of effects with pairs of elements
   * from the original streams. `F[_]` must be an [[cats.Applicative]].
   */
  def productF[B](that: Future[F[B]])(implicit applicative: Applicative[F]): Future[F[(Out, B)]] =
    CAlg[Future, F].cmp.productF(future, that)

  /**
   * Given two [[scala.concurrent.Future]]s of effects, form a new [[scala.concurrent.Future]]
   * of effects with pairs of elements
   * from the original streams. `F[_]` must be an [[cats.Applicative]].
   */
  def product3F[B, C](snd: Future[F[B]], thd: Future[F[C]])
                     (implicit applicative: Applicative[F]): Future[F[(Out, B, C)]] =
    CAlg[Future, F].cmp.product3F(future, snd, thd)

  /**
   * Given a [[scala.concurrent.Future]] of effects of one element type with another, and a (pure) binary
   * function mapping these element types to another, form a new [[scala.concurrent.Future]] of effects
   * with the resultant element type. `F[_]` must be an [[cats.Applicative]].
   */
  def map2F[B, C](snd: Future[F[B]])(g: (Out, B) => C)
                 (implicit applicative: Applicative[F]): Future[F[C]] =
    CAlg[Future, F].cmp.map2F(future, snd)(g)

  /**
   * Given two other [[scala.concurrent.Future]]s of effects, and a (pure) ternary
   * function mapping the element types to another, form a new [[scala.concurrent.Future]] of effects
   * with the resultant element type. `F[_]` must be an [[cats.Applicative]].
   */
  def map3F[B, C, D](snd: Future[F[B]], thd: Future[F[C]])(g: (Out, B, C) => D)
                    (implicit applicative: Applicative[F]): Future[F[D]] =
    CAlg[Future, F].cmp.map3F(future, snd, thd)(g)
}
