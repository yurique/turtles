/* Copyright 2014–2018 SlamData Inc. and Greg Pfeil.
 * Licensed under the Apache License, Version 2.0.
 * See https://github.com/sellout/turtles#copyright for details.
 */

package turtles

import slamdata.Predef.{Eq => _, _}

import cats._

/** This is a workaround for a certain use case (e.g.,
  * [[turtles.patterns.Diff]] and [[turtles.patterns.PotentialFailure]]).
  * Define an instance of this rather than [[Steppable]] when possible.
  */
// NB: Not a `@typeclass` because we don’t want to inject these operations.
// Changed Serializable because Quasar connector for Apache Spark needs it to be Serializable
// We can allow above since this trait will be removed once we have mutual recursion
trait SteppableT[T[_[_]]] extends Serializable {
  def embedT[F[_]: Functor](t: F[T[F]]): T[F]

  def projectT[F[_]: Functor](t: T[F]): F[T[F]]
}

object SteppableT {
  def apply[T[_[_]]](implicit instance: SteppableT[T]) = instance
}
