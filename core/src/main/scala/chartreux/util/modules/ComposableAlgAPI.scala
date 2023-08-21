package chartreux
package util
package modules

import cats.Applicative
import chartreux.util.algebras.{ Composable, ComposableAlg }
import chartreux.util.interpreters.ComposableInterpreter

class ComposableAlgAPI[F[_]: Applicative, G[_]] extends ComposableAlg[F, G] {
  override def cmp: Composable[F, G] = ComposableInterpreter[F, G]
}

object ComposableAlgAPI {
  @inline
  def apply[F[_]: Applicative, G[_]]: ComposableAlgAPI[F, G] = new ComposableAlgAPI[F, G]
}
