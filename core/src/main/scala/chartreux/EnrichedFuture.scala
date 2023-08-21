package chartreux

import cats.implicits.catsStdInstancesForFuture

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class EnrichedFuture[+A](future: Future[A]) {
  /**
   * Form a [[scala.concurrent.Future]] whose contents are a triple of
   * the contents of three other `Future`s.
   */
  def product3[B, C](snd: Future[B])(thd: Future[C]): Future[(A, B, C)] = {
    val F = cats.Applicative[Future]
    val fabc = F.product(F.product(future, snd), thd)
    F.map(fabc) { case ((a, b), c) => (a, b, c) }
  }

  /**
   * Transform this [[scala.concurrent.Future]] by replacing the contents with some
   * specified default value.
   *
   */
  def as[B](default: B): Future[B] =
    cats.Functor[Future].as(future, default)
}
