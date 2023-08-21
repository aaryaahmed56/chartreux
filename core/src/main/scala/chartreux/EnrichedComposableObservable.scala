package chartreux

import cats.{ Applicative, Functor }
import chartreux.util.CAlg
import chartreux.implicits.Instances.observableApplicative
import org.mongodb.scala.Observable

case class EnrichedComposableObservable[F[_], Out](observable: Observable[F[Out]]) {
  /**
   * Transform this [[org.mongodb.scala.Observable]] of effects into a new
   * [[org.mongodb.scala.Observable]] of effects by applying a function `f`
   * to the elements. `F[_]` must be a [[cats.Functor]].
   */
  def mapF[B](f: Out => B)(implicit functor: Functor[F]): Observable[F[B]] =
    CAlg[Observable, F].cmp.mapF(observable)(f)

  /**
   * Given an [[org.mongodb.scala.Observable]] of effects and some default value,
   * form a new [[org.mongodb.scala.Observable]] of effects
   * replacing the elements by this value. `F[_]` must be a [[cats.Functor]].
   */
  def asF[B](b: B)(implicit functor: Functor[F]): Observable[F[B]] =
    CAlg[Observable, F].cmp.asF(observable)(b)

  /**
   * Form a new [[org.mongodb.scala.Observable]] of effects with the data now tupled with some
   * default value as the left factor. `F[_]` must be a [[cats.Functor]].
   */
  def tupleLeftF[B](b: B)(implicit functor: Functor[F]): Observable[F[(B, Out)]] =
    CAlg[Observable, F].cmp.tupleLeftF(observable)(b)

  /**
   * Form a new [[org.mongodb.scala.Observable]] of effects with the data now tupled with some
   * default value as the right factor. `F[_]` must be a [[cats.Functor]].
   */
  def tupleRightF[B](b: B)(implicit functor: Functor[F]): Observable[F[(Out, B)]] =
    CAlg[Observable, F].cmp.tupleRightF(observable)(b)

  /**
   * Transform this [[org.mongodb.scala.Observable]] of effects into a new [[org.mongodb.scala.Observable]]
   * of effects by applying a function `f` to the elements while preserving the inputs to the function, thereby
   * yielding an effectful [[org.mongodb.scala.Observable]] of tuples where the left factors are the inputs and
   * the right factors are the outputs. `F[_]` must be a [[cats.Functor]].
   */
  def fproductF[B](f: Out => B)(implicit functor: Functor[F]): Observable[F[(Out, B)]] =
    CAlg[Observable, F].cmp.fproductF(observable)(f)

  /**
   * Transform this [[org.mongodb.scala.Observable]] of effects into a new [[org.mongodb.scala.Observable]]
   * of effects by applying a function `f` to the elements while preserving the inputs to the function, thereby
   * yielding an effectful [[org.mongodb.scala.Observable]] of tuples where the left factors are the outputs and
   * the right factors are the inputs. `F[_]` must be a [[cats.Functor]].
   */
  def fproductLeftF[B](obs: Observable[F[B]])(f: B => Out)
                      (implicit functor: Functor[F]): Observable[F[(Out, B)]] =
    CAlg[Observable, F].cmp.fproductLeftF(obs)(f)

  /**
   * Given another [[org.mongodb.scala.Observable]] of effects, form a new [[org.mongodb.scala.Observable]]
   * of effects with pairs of elements from the original streams. `F[_]` must be an [[cats.Applicative]].
   */
  def productF[B](that: Observable[F[B]])(implicit applicative: Applicative[F]): Observable[F[(Out, B)]] =
    CAlg[Observable, F].cmp.productF(observable, that)

  /**
   * Given two other [[org.mongodb.scala.Observable]]s of effects, form a new [[org.mongodb.scala.Observable]]
   * of effects with triples of elements from the original streams. `F[_]` must be an [[cats.Applicative]].
   */
  def product3F[B, C](snd: Observable[F[B]], thd: Observable[F[C]])
                     (implicit applicative: Applicative[F]): Observable[F[(Out, B, C)]] =
    CAlg[Observable, F].cmp.product3F(observable, snd, thd)

  /**
   * Given a Observable of effects of one element type with another, and a (pure) binary
   * function mapping these element types to another, form a new Observable of effects
   * with the resultant element type. `F[_]` must be an [[cats.Applicative]].
   */
  def map2F[B, C](snd: Observable[F[B]])(g: (Out, B) => C)
                 (implicit applicative: Applicative[F]): Observable[F[C]] =
    CAlg[Observable, F].cmp.map2F(observable, snd)(g)

  /**
   * Given two other [[org.mongodb.scala.Observable]]s of effects, and a (pure) ternary
   * function mapping the element types to another, form a new [[org.mongodb.scala.Observable]] of effects
   * with the resultant element type. `F[_]` must be an [[cats.Applicative]].
   */
  def map3F[B, C, D](snd: Observable[F[B]], thd: Observable[F[C]])(g: (Out, B, C) => D)
                    (implicit applicative: Applicative[F]): Observable[F[D]] =
    CAlg[Observable, F].cmp.map3F(observable, snd, thd)(g)
}
