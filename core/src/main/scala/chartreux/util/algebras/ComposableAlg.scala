package chartreux
package util
package algebras

trait ComposableAlg[F[_], G[_]] {
  def cmp: Composable[F, G]
}
