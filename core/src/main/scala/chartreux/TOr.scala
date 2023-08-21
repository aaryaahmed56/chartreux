package chartreux

/**
 * Ternary disjoint union, or a ternary "exclusive-or" intended to model the
 * possibility of a type "being (one) of three types". It can be instanced as either
 * [[TOr.Fst]], [[TOr.Snd]], or [[TOr.Thd]]. The right-most factors, i.e.
 * [[TOr.Snd]] and [[TOr.Thd]], are meant to be the ones in which data transformation
 * can occur, whereas the left-most factor [[TOr.Fst]] is meant to be fixed.
 */
sealed abstract class TOr[+A, +B, +C] extends Product with Serializable

/**
 * The left-most factor of the disjoint union, as
 * opposed to the other two.
 */
final case class Fst[A, B, C](a: A) extends TOr[A, B, C]

/**
 * The "interior" of the disjoint union, as
 * opposed to the other two.
 */
final case class Snd[A, B, C](b: B) extends TOr[A, B, C]

/**
 * The right-most side of the disjoint union, as
 * opposed to the other two.
 */
final case class Thd[A, B, C](c: C) extends TOr[A, B, C]

object TOr {
  /**
   * Construct a right-most-biased `TOr`.
   */
  def apply[A](a: A): TOr[Nothing, Nothing, A] = Thd(a)

  /**
   * Lift an [[Option]]al value into an essentially right-most-biased `TOr`.
   * The [[None]] case is handled by [[Unit]].
   */
  def fromOption[C](o: Option[C]): TOr[Nothing, Unit, C] =
    o match {
      case None => Snd(())
      case Some(value) => Thd(value)
    }

  /**
   * Embed an [[Either]] value into a middle- and right-most-biased `TOr`.
   */
  def fromEither[B, C](e: Either[B, C]): TOr[Nothing, B, C] =
    e match {
      case Left(value) => Snd(value)
      case Right(value) => Thd(value)
    }
}
