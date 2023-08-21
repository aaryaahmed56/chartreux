package chartreux.implicits

import akka.NotUsed
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Sink, Source, Zip}
import akka.stream.{FlowShape, SourceShape}
import cats.{Alternative, Applicative, Bifunctor, Contravariant, Functor, Monad}
import chartreux.Additions.NonEmptySource
import chartreux._
import org.mongodb.scala.Observable

object Instances {
  implicit def sinkContravariant: Contravariant[Sink[-*, NotUsed]] = new Contravariant[Sink[-*, NotUsed]] {
    override def contramap[A, B](fa: Sink[A, NotUsed])(f: B => A): Sink[B, NotUsed] = fa.contramap(f)
  }

  implicit def sourceFunctor: cats.Functor[Source[+*, NotUsed]] = new Functor[Source[+*, NotUsed]] {
    override def map[A, B](fa: Source[A, NotUsed])(f: A => B): Source[B, NotUsed] = fa.map(f)
  }

  implicit def sourceApplicative: Applicative[Source[+*, NotUsed]] = new Applicative[Source[+*, NotUsed]] {
    override def pure[A](x: A): Source[A, NotUsed] = Source.single[A](x)

    override def ap[A, B](ff: Source[A => B, NotUsed])(fa: Source[A, NotUsed]): Source[B, NotUsed] =
      Source.fromGraph {
        GraphDSL.create() { implicit b =>
          import akka.stream.scaladsl.GraphDSL.Implicits._

          val zip = b.add(akka.stream.scaladsl.Zip[A => B, A]())

          ff ~> zip.in0
          fa ~> zip.in1

          SourceShape(zip.out)
        }
      }.map { case _@(f, a) => f(a) }
  }

  implicit def sourceAlternative: Alternative[Source[+*, NotUsed]] = new Alternative[Source[+*, NotUsed]] {
    override def empty[A]: Source[A, NotUsed] = Source.empty[A]

    override def combineK[A](x: Source[A, NotUsed], y: Source[A, NotUsed]): Source[A, NotUsed] =
      Source.fromGraph {
        GraphDSL.create() { implicit b =>
          import akka.stream.scaladsl.GraphDSL.Implicits._

          val concat = b.add(akka.stream.scaladsl.Concat[A](inputPorts = 2))

          x ~> concat.in(0)
          y ~> concat.in(1)

          SourceShape(concat.out)
        }
      }

    override def pure[A](x: A): Source[A, NotUsed] =
      cats.Applicative[Source[+*, NotUsed]].pure(x)

    override def ap[A, B](ff: Source[A => B, NotUsed])(fa: Source[A, NotUsed]): Source[B, NotUsed] =
      cats.Applicative[Source[+*, NotUsed]].ap(ff)(fa)
  }

  implicit def flowFunctor[C]: Functor[Flow[C, +*, NotUsed]] = new Functor[Flow[C, +*, NotUsed]] {
    override def map[A, B](fa: Flow[C, A, NotUsed])(f: A => B): Flow[C, B, NotUsed] = fa.map(f)
  }

  implicit def flowApplicative[C]: Applicative[Flow[C, +*, NotUsed]] = new Applicative[Flow[C, +*, NotUsed]] {
    override def pure[A](x: A): Flow[C, A, NotUsed] = Flow[C].map(_ => x)

    override def ap[A, B](ff: Flow[C, A => B, NotUsed])(fa: Flow[C, A, NotUsed]): Flow[C, B, NotUsed] =
      Flow.fromGraph {
        GraphDSL.create() { implicit b =>
          import GraphDSL.Implicits._
          val bcast = b.add(Broadcast[C](2))
          val zip = b.add(Zip[A => B, A]())

          bcast.out(0) ~> ff ~> zip.in0
          bcast.out(1) ~> fa ~> zip.in1

          FlowShape(bcast.in, zip.out)
        }
      }.map { case _@(f, a) => f(a) }
  }

  implicit def observableFunctor: Functor[org.mongodb.scala.Observable] = new Functor[Observable] {
    override def map[A, B](fa: Observable[A])(f: A => B): Observable[B] = fa.collect { case x => f(x) }
  }

  implicit def observableApplicative: Applicative[org.mongodb.scala.Observable] = new Applicative[Observable] {
    override def pure[A](x: A): Observable[A] = Observable(Seq(x))

    override def ap[A, B](ff: Observable[A => B])(fa: Observable[A]): Observable[B] =
      ff.zip(fa).collect { case _@(f, a) => f(a) }
  }

  implicit def torBifunctor[E]: cats.Bifunctor[TOr[E, +*, +*]] = new Bifunctor[TOr[E, +*, +*]] {
    override def bimap[A, B, C, D](fab: TOr[E, A, B])(f: A => C, g: B => D): TOr[E, C, D] =
      fab match {
        case Fst(e) => Fst(e)
        case Snd(a) => Snd(f(a))
        case Thd(b) => Thd(g(b))
      }
  }

  implicit def torMonadRight[C, D]: Monad[TOr[C, D, +*]] = new Monad[TOr[C, D, +*]] {
    override def pure[A](x: A): TOr[C, D, A] = Thd(x)

    override def flatMap[A, B](fa: TOr[C, D, A])(f: A => TOr[C, D, B]): TOr[C, D, B] =
      fa match {
        case Fst(c) => Fst(c)
        case Snd(d) => Snd(d)
        case Thd(a) => f(a)
      }

    @scala.annotation.tailrec
    override def tailRecM[A, B](a: A)(f: A => TOr[C, D, Either[A, B]]): TOr[C, D, B] =
      f(a) match {
        case Fst(c) => Fst(c)
        case Snd(d) => Snd(d)
        case Thd(either) =>
          either match {
            case Left(a) => tailRecM(a)(f)
            case Right(b) => Thd(b)
          }
      }
  }

  implicit def torMonadLeft[C, D]: Monad[TOr[C, +*, D]] = new Monad[TOr[C, +*, D]] {
    override def pure[A](x: A): TOr[C, A, D] = Snd(x)

    override def flatMap[A, B](fa: TOr[C, A, D])(f: A => TOr[C, B, D]): TOr[C, B, D] =
      fa match {
        case Fst(c) => Fst(c)
        case Snd(a) => f(a)
        case Thd(d) => Thd(d)
      }

    @scala.annotation.tailrec
    override def tailRecM[A, B](a: A)(f: A => TOr[C, Either[A, B], D]): TOr[C, B, D] =
      f(a) match {
        case Fst(c) => Fst(c)
        case Snd(either) =>
          either match {
            case Left(a) => tailRecM(a)(f)
            case Right(b) => Snd(b)
          }
        case Thd(d) => Thd(d)
      }
  }

  implicit def neSourceFunctor: Functor[NonEmptySource] = new Functor[NonEmptySource] {
    override def map[A, B](fa: NonEmptySource[A])(f: A => B): NonEmptySource[B] =
      fa.map(f)
  }

  implicit def neSourceApplicative: Applicative[NonEmptySource] = new Applicative[NonEmptySource] {
    override def pure[A](x: A): NonEmptySource[A] = NonEmptySource.single[A](x)

    override def ap[A, B](ff: NonEmptySource[A => B])(fa: NonEmptySource[A]): NonEmptySource[B] = {
      val ffsrctl: Source[A => B, NotUsed] = ff.tail
      val fasrctl: Source[A, NotUsed] = fa.tail
      NonEmptySource(ff.head(fa.head), Applicative[Source[+*, NotUsed]].ap(ffsrctl)(fasrctl))
    }
  }
}
