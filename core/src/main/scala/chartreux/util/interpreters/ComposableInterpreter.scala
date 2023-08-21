package chartreux
package util
package interpreters

import cats.{ Applicative, Functor }
import chartreux.util.algebras.Composable

class ComposableInterpreter[F[_]: Applicative, G[_]] extends Composable[F, G] {
  override def mapF[A, B](fga: F[G[A]])(f: A => B)(implicit functor: Functor[G]): F[G[B]] =
    Functor[F].compose[G].map(fga)(f)

  override def asF[A, B](fga: F[G[A]])(b: B)(implicit functor: Functor[G]): F[G[B]] =
    Functor[F].compose[G].as(fga, b)

  override def tupleLeftF[A, B](fga: F[G[A]])(b: B)(implicit functor: Functor[G]): F[G[(B, A)]] =
    Functor[F].compose[G].tupleLeft(fga, b)

  override def tupleRightF[A, B](fga: F[G[A]])(b: B)(implicit functor: Functor[G]): F[G[(A, B)]] =
    Functor[F].compose[G].tupleRight(fga, b)

  override def fproductF[A, B](fga: F[G[A]])(f: A => B)(implicit functor: Functor[G]): F[G[(A, B)]] =
    Functor[F].compose[G].fproduct(fga)(f)

  override def fproductLeftF[A, B](fga: F[G[A]])(f: A => B)(implicit functor: Functor[G]): F[G[(B, A)]] =
    Functor[F].compose[G].fproductLeft(fga)(f)

  override def productF[A, B](fga: F[G[A]], fgb: F[G[B]])(implicit applicative: Applicative[G]): F[G[(A, B)]] =
    Applicative[F].compose[G].product(fga, fgb)

  override def product3F[A, B, C](fga: F[G[A]], fgb: F[G[B]], fgc: F[G[C]])(implicit applicative: Applicative[G]): F[G[(A, B, C)]] = {
    val FG = Applicative[F].compose[G]
    val triple = FG.product(FG.product(fga, fgb), fgc)
    FG.map(triple) { case ((a, b), c) => (a, b, c) }
  }

  override def map2F[A, B, C](fga: F[G[A]], fgb: F[G[B]])(g: (A, B) => C)(implicit applicative: Applicative[G]): F[G[C]] =
    Applicative[F].compose[G].map2(fga, fgb)(g)

  override def map3F[A, B, C, D](fga: F[G[A]], fgb: F[G[B]], fgc: F[G[C]])(g: (A, B, C) => D)(implicit applicative: Applicative[G]): F[G[D]] =
    Applicative[F].compose[G].map3(fga, fgb, fgc)(g)
}

object ComposableInterpreter {
  @inline
  def apply[F[_]: Applicative, G[_]]: ComposableInterpreter[F, G] =
    new ComposableInterpreter[F, G]
}
