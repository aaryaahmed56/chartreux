package chartreux

import akka.NotUsed
import akka.stream.scaladsl.Sink
import cats.Functor
import cats.implicits.catsStdInstancesForFuture
import chartreux.implicits.Instances.sinkContravariant

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class EnrichedFutureSink[-In](sink: Future[Sink[In, NotUsed]]) {
  /**
   * Useful for when a [[akka.stream.scaladsl.Sink.contramap]] transformation needs to take place
   * inside of a "future sink" prior to constructing a [[akka.stream.scaladsl.Sink]] from it.
   */
  def contramap[B](g: B => In): Future[Sink[B, NotUsed]] =
    Functor[Future].composeContravariant[Sink[*, NotUsed]].contramap(sink)(g)
}
