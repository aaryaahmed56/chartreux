package chartreux
package util

import akka.NotUsed
import akka.stream.scaladsl.{ Flow, Sink }
import cats.ApplicativeError

object ErrorSink {
  private def errorSink[E]: Sink[E, NotUsed] =
    Flow[E]
      .takeWhile { _ => true }
      .to(Sink.ignore)

  private def sinkEither[E, A]: Sink[Either[E, A], NotUsed] = errorSink.contramap(
    _.left.getOrElse(sys.error("No left value!"))
  )

  private def shouldSendToSink[E, A](message: Either[E, A]): Boolean =
    message.isLeft

  /**
    * Typeful error handling approach in `akka-streams` involving [[scala.util.Either]],
    * [[akka.stream.scaladsl.FlowOps.divertTo]] and [[akka.stream.scaladsl.FlowOps.collect]].
    * An error [[akka.stream.scaladsl.Sink]] which may either skip, halt, or re-process messages is defined
    * to which messages are diverted upon detection of failure, and successfully computed values
    * (regardless of materialization status) are extracted.
    *
    * @return `Flow[Either[E, A], A, NotUsed]`
    *
    */
  def divertErrors[E, A]: Flow[Either[E, A], A, NotUsed] =
    Flow[Either[E, A]]
      .divertTo(sinkEither, shouldSendToSink)
      .collect { case Right(a) => a }

  def handleErrors[E, A](default: A): Flow[Either[E, A], A, NotUsed] = {
    val eitherAppError = ApplicativeError[Either[E, +*], E]

    Flow[Either[E, A]]
      .map(res => eitherAppError.handleError(res)(_ => default))
      .collect { case Right(a) => a }
  }
}
