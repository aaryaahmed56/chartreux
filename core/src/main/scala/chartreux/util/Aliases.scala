package chartreux
package util

import akka.NotUsed
import akka.stream.scaladsl.Flow

object Aliases {
  type PureFlow[-In, +Out] = Flow[In, Out, NotUsed]
}
