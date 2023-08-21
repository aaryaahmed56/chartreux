package chartreux

import cats.{ Applicative, Functor }
import chartreux.implicits.Instances.observableApplicative
import org.mongodb.scala.Observable

case class EnrichedObservable[Out](observable: Observable[Out]) {
  /**
   * Transform this [[org.mongodb.scala.Observable]] by replacing the elements by some specified default value.
   */
  def as[B](b: B): Observable[B] =
    Functor[Observable].as(observable, b)

  /**
   * Form an [[org.mongodb.scala.Observable]] of pairs by supplying some default value as the left factor.
   */
  def tupleLeft[B](b: B): Observable[(B, Out)] =
    Functor[Observable].tupleLeft(observable, b)

  /**
   * Form an [[org.mongodb.scala.Observable]] of pairs by supplying some default value as the right factor.
   */
  def tupleRight[B](b: B): Observable[(Out, B)] =
    Functor[Observable].tupleRight(observable, b)

  /**
   * Form an [[org.mongodb.scala.Observable]] of pairs (right-wise) given another [[org.mongodb.scala.Observable]]
   * and a function mapping the element type of that [[org.mongodb.scala.Observable]] to this one.
   */
  def fproduct[B](obs: Observable[B])(f: B => Out): Observable[(B, Out)] =
    Functor[Observable].fproduct(obs)(f)

  /**
   * Form an [[org.mongodb.scala.Observable]] of pairs (left-wise) given another [[org.mongodb.scala.Observable]]
   * and a function mapping the element type of that [[org.mongodb.scala.Observable]] to this one.
   */
  def fproductLeft[B](obs: Observable[B])(f: B => Out): Observable[(Out, B)] =
    Functor[Observable].fproductLeft(obs)(f)

  /**
   * Form a new [[org.mongodb.scala.Observable]] with elements being pairs of elements from two other
   * [[org.mongodb.scala.Observable]]s.
   */
  def product[B](snd: Observable[B]): Observable[(Out, B)] =
    Applicative[Observable].product(observable, snd)

  /**
   * Infix notation for [[EnrichedObservable.product]].
   */
  def <*>[B](snd: Observable[B]): Observable[(Out, B)] =
    Applicative[Observable].product(observable, snd)

  /**
   * Form a new [[org.mongodb.scala.Observable]] with elements being triples of elements from three
   * [[org.mongodb.scala.Observable]]s.
   */
  def product3[B, C](snd: Observable[B], thd: Observable[C]): Observable[(Out, B, C)] = {
    val F = Applicative[Observable]
    val fabc = F.product(F.product(observable, snd), thd)
    F.map(fabc) { case ((a, b), c) => (a, b, c) }
  }

  /**
   * Applies the (pure) binary function `g` to this [[org.mongodb.scala.Observable]] with
   * another one, where the values of the inputs of the binary function
   * are of the element types for both streams.
   */
  def map2[B, C](snd: Observable[B])(g: (Out, B) => C): Observable[C] =
    Applicative[Observable].map2(observable, snd)(g)

  /**
   * Applies the (pure) ternary function `g` to this [[org.mongodb.scala.Observable]] with
   * two others, where the values of the inputs of the ternary function
   * are of the element types for all streams.
   */
  def map3[B, C, D](snd: Observable[B], thd: Observable[C])(g: (Out, B, C) => D): Observable[D] =
    Applicative[Observable].map3(observable, snd, thd)(g)
}
