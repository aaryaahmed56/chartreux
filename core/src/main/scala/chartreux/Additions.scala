package chartreux

import akka.NotUsed
import akka.stream.scaladsl.{Flow, Sink, Source}
import cats.data.OneAnd
import org.mongodb.scala.Observable

import scala.concurrent.Future

object Additions {
  /**
   * A [[akka.stream.scaladsl.Source]] which, by design, is guaranteed to be
   * non-empty. An additional design principle observed in the construction of this
   * type and its associated builder methods is to err on the side of
   * safety as well as laziness. Methods which may throw an exception are given
   * 'unsafe' in the title.
   */
  type NonEmptySource[A] = OneAnd[Source[+*, NotUsed], A]

  implicit def toFutureAddition[A](future: Future[A]): EnrichedFuture[A] = EnrichedFuture(future)

  implicit def toTOrAdditions[C, D, A](tor: TOr[C, D, A]): EnrichedTOr[C, D, A] = EnrichedTOr(tor)

  implicit def toFlowAddition[I, O](flow: Flow[I, O, NotUsed]): EnrichedFlow[I, O] = EnrichedFlow(flow)

  implicit def toStreamAddition[O](source: Source[O, NotUsed]): EnrichedSource[O] = EnrichedSource(source)

  implicit def toNESourceAddition[O](nesrc: NonEmptySource[O]): EnrichedNonEmptySource[O] = EnrichedNonEmptySource(nesrc)

  implicit def toObservableAddition[O](observable: Observable[O]): EnrichedObservable[O] = EnrichedObservable(observable)

  implicit def toFutureSinkAddition[I](sink: Future[Sink[I, NotUsed]]): EnrichedFutureSink[I] = EnrichedFutureSink(sink)

  implicit def toComposableStreamAddition[F[+_], O](source: Source[F[O], NotUsed]):
  EnrichedComposableSource[F, O] = EnrichedComposableSource(source)

  implicit def toComposableFlowAddition[F[+_], I, O](flow: Flow[I, F[O], NotUsed]):
  EnrichedComposableFlow[F, I, O] =
    EnrichedComposableFlow(flow)

  implicit def toComposableObservableAddition[F[+_], O](observable: Observable[F[O]]):
  EnrichedComposableObservable[F, O] =
    EnrichedComposableObservable(observable)

  implicit def toComposableFutureAddition[F[+_], O](future: Future[F[O]]):
  EnrichedComposableFuture[F, O] =
    EnrichedComposableFuture(future)
}
