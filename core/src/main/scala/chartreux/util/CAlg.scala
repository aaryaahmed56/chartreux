package chartreux
package util

import cats.Applicative
import chartreux.util.algebras.{ Composable, ComposableAlg }
import chartreux.util.modules.ComposableAlgAPI

class CAlg[F[_]: Applicative, G[_]] extends ComposableAlg[F, G] {

  private lazy val module = ComposableAlgAPI[F, G]

  override def cmp: Composable[F, G] = module.cmp
}

object CAlg {
  @inline
  def apply[F[_]: Applicative, G[_]]: CAlg[F, G] = new CAlg[F, G]
}
