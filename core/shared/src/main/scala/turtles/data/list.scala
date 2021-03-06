/* Copyright 2014–2018 SlamData Inc. and Greg Pfeil.
 * Licensed under the Apache License, Version 2.0.
 * See https://github.com/sellout/turtles#copyright for details.
 */

package turtles.data

import slamdata.Predef.{Eq => _, _}
import turtles._
import turtles.patterns._

trait ListInstances {
  implicit def listBirecursive[A]: Birecursive.Aux[List[A], ListF[A, ?]] =
    Birecursive.fromAlgebraIso[List[A], ListF[A, ?]]({
      case ConsF(h, t) => h :: t
      case NilF()      => Nil
    }, {
      case h :: t => ConsF(h, t)
      case Nil    => NilF[A, List[A]]()
    })
}

object list extends ListInstances
