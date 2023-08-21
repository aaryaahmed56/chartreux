package chartreux

import cats.implicits.catsSyntaxOptionId
import cats.{Applicative, Bifunctor, Monad}
import chartreux.implicits.Instances.{torBifunctor, torMonadLeft, torMonadRight}

case class EnrichedTOr[C, D, A](tor: TOr[C, D, A]) {
  def toOptionL: Option[D] = tor match {
    case Fst(_) => None
    case Snd(d) => d.some
    case Thd(_) => None
  }

  def toOptionR: Option[A] = tor match {
    case Fst(_) => None
    case Snd(_) => None
    case Thd(a) => a.some
  }

  def toOptionalEither: Option[Either[D, A]] = tor match {
    case Fst(_) => None
    case Snd(d) => Left(d).some
    case Thd(c) => Right(c).some
  }

  def map[B](f: A => B): TOr[C, D, B] = Bifunctor[TOr[C, +*, +*]].rightFunctor.map(tor)(f)

  def leftMap[B](f: D => B): TOr[C, B, A] = Bifunctor[TOr[C, +*, +*]].leftMap(tor)(f)

  def as[B](b: B): TOr[C, D, B] = Bifunctor[TOr[C, +*, +*]].rightFunctor.as(tor, b)

  def asL[B](b: B): TOr[C, B, A] = Bifunctor[TOr[C, +*, +*]].leftFunctor.as(tor, b)

  def tupleLeft[B](b: B): TOr[C, D, (B, A)] = Bifunctor[TOr[C, +*, +*]].rightFunctor.tupleLeft(tor, b)

  def tupleLeftL[B](b: B): TOr[C, (B, D), A] = Bifunctor[TOr[C, +*, +*]].leftFunctor.tupleLeft(tor, b)

  def tupleRight[B](b: B): TOr[C, D, (A, B)] = Bifunctor[TOr[C, +*, +*]].rightFunctor.tupleRight(tor, b)

  def tupleRightL[B](b: B): TOr[C, (D, B), A] = Bifunctor[TOr[C, +*, +*]].leftFunctor.tupleRight(tor, b)

  def product[B](snd: TOr[C, D, B]): TOr[C, D, (A, B)] = Applicative[TOr[C, D, +*]].product(tor, snd)

  def <*>[B](snd: TOr[C, D, B]): TOr[C, D, (A, B)] = Applicative[TOr[C, D, +*]].product(tor, snd)

  def productLeft[B](snd: TOr[C, B, A]): TOr[C, (D, B), A] = Applicative[TOr[C, +*, A]].product(tor, snd)

  def <+>[B](snd: TOr[C, B, A]): TOr[C, (D, B), A] = Applicative[TOr[C, +*, A]].product(tor, snd)

  def map2[B, E](snd: TOr[C, D, B])(f: (A, B) => E): TOr[C, D, E] = Applicative[TOr[C, D, +*]].map2(tor, snd)(f)

  def map2L[B, E](snd: TOr[C, B, A])(f: (D, B) => E): TOr[C, E, A] = Applicative[TOr[C, +*, A]].map2(tor, snd)(f)

  def flatMap[B](f: A => TOr[C, D, B]): TOr[C, D, B] = Monad[TOr[C, D, +*]].flatMap(tor)(f)

  def leftFlatMap[B](f: D => TOr[C, B, A]): TOr[C, B, A] = Monad[TOr[C, +*, A]].flatMap(tor)(f)
}
