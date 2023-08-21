package chartreux
package util
package algebras

import cats.{ Applicative, Functor }

/**
 * A `tagless-final` algebra encapsulating a subset of functions that
 * rely on (applicative) covariant functoriality and which are "preserved"
 * on account of the composability of (applicative) covariant functors.
 *
 * @tparam F A type variable of kind `* -> *` which, although unbound in this
 *           algebra definition, is context-bounded as an [[cats.Applicative]]
 *           in the interpreter [[chartreux.util.interpreters.ComposableInterpreter]].
 * @tparam G A type variable of kind `* -> *` which is given implicit instances of
 *           [[cats.Functor]] or [[cats.Applicative]] depending
 *           on the requirements of each function in the algebra.
 */
trait Composable[F[_], G[_]] {
  def mapF[A, B](fga: F[G[A]])(f: A => B)(implicit functor: Functor[G]): F[G[B]]

  def asF[A, B](fga: F[G[A]])(b: B)(implicit functor: Functor[G]): F[G[B]]

  def tupleLeftF[A, B](fga: F[G[A]])(b: B)(implicit functor: Functor[G]): F[G[(B, A)]]

  def tupleRightF[A, B](fga: F[G[A]])(b: B)(implicit functor: Functor[G]): F[G[(A, B)]]

  def fproductF[A, B](fga: F[G[A]])(f: A => B)(implicit functor: Functor[G]): F[G[(A, B)]]

  def fproductLeftF[A, B](fga: F[G[A]])(f: A => B)(implicit functor: Functor[G]): F[G[(B, A)]]

  def productF[A, B](fga: F[G[A]], fgb: F[G[B]])(implicit applicative: Applicative[G]): F[G[(A, B)]]

  def product3F[A, B, C](fga: F[G[A]], fgb: F[G[B]], fgc: F[G[C]])(implicit applicative: Applicative[G]): F[G[(A, B, C)]]

  def map2F[A, B, C](fga: F[G[A]], fgb: F[G[B]])(g: (A, B) => C)(implicit applicative: Applicative[G]): F[G[C]]

  def map3F[A, B, C, D](fga: F[G[A]], fgb: F[G[B]], fgc: F[G[C]])(g: (A, B, C) => D)(implicit applicative: Applicative[G]): F[G[D]]
}
