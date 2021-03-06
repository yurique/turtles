/* Copyright 2014–2018 SlamData Inc. and Greg Pfeil.
 * Licensed under the Apache License, Version 2.0.
 * See https://github.com/sellout/turtles#copyright for details.
 */

import slamdata.Predef.{Eq => _, _}
import turtles.data.cofree._
import turtles.data.free._
import turtles.derived._
import turtles.implicits._
import turtles.instances.fixedpoint.{BirecursiveOptionOps, Nat}
import turtles.patterns.{CoEnv, EnvT}

import cats._
import cats.arrow._
import cats.data._
import cats.free._
import cats.implicits._
import monocle._

/** Generalized folds, unfolds, and refolds.
  *
  * @groupname algebras Algebras & Coalgebras
  * @groupdesc algebras Generic algebras that operate over most functors.
  * @groupname algtrans Algebra Transformations
  * @groupdesc algtrans Operations that modify algebras in various ways to make them easier to combine with others.
  * @groupname refolds Refolds
  * @groupdesc refolds Traversals that apply an unfold and a fold in a single pass.
  * @groupname dist Distributive Laws
  * @groupdesc dist Natural transformations required for generalized folds and unfolds.
  */
package object turtles {
  /** Fold a structure `F` containing values in `W`, to a value `A`,
    * accumulating effects in the monad `M`.
    * @group algebras
    */
  type GAlgebraM[W[_], M[_], F[_], A] = F[W[A]] => M[A]
  /** Fold a structure `F` containing values in `W`, to a value `A`.
    * @group algebras
    */
  type GAlgebra[W[_], F[_], A]        = F[W[A]] => A    // GAlgebraM[W, Id, F, A]
  /** Fold a structure `F` to a value `A`, accumulating effects in the monad `M`.
    * @group algebras
    */
  type AlgebraM[M[_], F[_], A]        = F[A]    => M[A] // GAlgebraM[Id, M, F, A]
  /** Fold a structure `F` to a value `A`.
    * @group algebras
    */
  type Algebra[F[_], A]               = F[A] => A       // GAlgebra[Id, F, A]
  /** Fold a structure `F` (usually a `Functor`) contained in `W` (usually a
    * `Comonad`), to a value `A`, accumulating effects in the monad `M`.
    * @group algebras
    */
  type ElgotAlgebraM[W[_], M[_], F[_], A] =                        W[F[A]] => M[A]
  /** Fold a structure `F` (usually a `Functor`) contained in `W` (usually a
    * `Comonad`), to a value `A`.
    * @group algebras
    */
  type ElgotAlgebra[W[_], F[_], A] = ElgotAlgebraM[W, Id, F, A] // W[F[A]] => A

  /** Unfold a value `A` to a structure `F` containing values in `N`,
    * accumulating effects in the monad `M`.
    * @group algebras
    */
  type GCoalgebraM[N[_], M[_], F[_], A] = A => M[F[N[A]]]
  /** Unfold a value `A` to a structure `F` containing values in `N`.
    * @group algebras
    */
  type GCoalgebra[N[_], F[_], A]        = A => F[N[A]] // GCoalgebraM[N, Id, F, A]
  /** Unfold a value `A` to a structure `F`, accumulating effects in the monad
    * `M`.
    * @group algebras
    */
  type CoalgebraM[M[_], F[_], A]        = A => M[F[A]] // GCoalgebraM[Id, M, F, A]
  /** Unfold a value `A` to a structure `F`.
    * @group algebras
    */
  type Coalgebra[F[_], A]               = A => F[A]    // GCoalgebra[Id, F, A]
  /** Unfold a value `A` to a structure `F` (usually a `Functor`), contained in
    * `E`, accumulating effects in the monad `M`.
    * @group algebras
    */
  type ElgotCoalgebraM[E[_], M[_], F[_], A] = A => M[E[F[A]]]
  /** Unfold a value `A` to a structure `F` (usually a `Functor`), contained in
    * `E`.
    * @group algebras
    */
  type ElgotCoalgebra[E[_], F[_], A]        = A => E[F[A]] // ElgotCoalgebraM[E, Id, F, A]

  /** Transform a structure `F` containing values in `W`, to a structure `G`,
    * in bottom-up fashion.
    * @group algebras
    */
  type AlgebraicGTransformM[W[_], M[_], T, F[_], G[_]] = F[W[T]] => M[G[T]]

  /** Transform a structure `F` containing values in `W`, to a structure `G`,
    * in bottom-up fashion.
    * @group algebras
    */
  type AlgebraicGTransform[W[_], T, F[_], G[_]]        = F[W[T]] => G[T]

  /** Transform a structure `F` contained in `W`, to a structure `G`,
    * in bottom-up fashion.
    * @group algebras
    */
  type AlgebraicElgotTransformM[W[_], M[_], T, F[_], G[_]] = W[F[T]] => M[G[T]]

  /** Transform a structure `F` contained in `W`, to a structure `G`,
    * in bottom-up fashion.
    * @group algebras
    */
  type AlgebraicElgotTransform[W[_], T, F[_], G[_]] = W[F[T]] => G[T]

  /** Transform a structure `F` to a structure `G` containing values in `N`,
    * in top-down fashion, accumulating effects in the monad `M`.
    * @group algebras
    */
  type CoalgebraicGTransformM[N[_], M[_], T, F[_], G[_]] = F[T] => M[G[N[T]]]

  /** Transform a structure `F` to a structure `G`, in top-down fashion,
    * accumulating effects in the monad `M`.
    * @group algebras
    */
  type CoalgebraicGTransform[N[_], T, F[_], G[_]]        = F[T] => G[N[T]]

  /** Transform a structure `F` to a structure `G`, in top-down fashion,
    * accumulating effects in the monad `M`.
    * @group algebras
    */
  type CoalgebraicElgotTransform[N[_], T, F[_], G[_]]    = F[T] => N[G[T]]

  /** Transform a structure `F` to a structure `G`, accumulating effects in the
    *  monad `M`. For a top-down transformation, `T#Base` must be `F`, and for a
    *  bottom-up transformation, `T#Base` must be `G`.
    *
    * @group algebras
    */
  type TransformM[M[_], T, F[_], G[_]]        = F[T] => M[G[T]]

  /** Transform a structure `F` to a structure `G`. For a top-down
    * transformation, `T#Base` must be `F`, and for a bottom-up transformation,
    * `T#Base` must be `G`.
    *
    * @group algebras
    */
  type Transform[T, F[_], G[_]]               = F[T] => G[T] // TransformM[Id, T, F, G]

  type EndoTransform[T, F[_]]                 = F[T] => F[T] // Transform[T, F, F]

  /** An algebra and its dual form an isomorphism.
    */
  type GAlgebraIso[W[_], N[_], F[_], A] = PIso[F[W[A]], F[N[A]], A, A]
  object GAlgebraIso {
    def apply[W[_], N[_], F[_], A](φ: GAlgebra[W, F, A])(ψ: GCoalgebra[N, F, A])
        : GAlgebraIso[W, N, F, A] =
      PIso(φ)(ψ)
  }

  type ElgotAlgebraIso[W[_], N[_], F[_], A] = PIso[W[F[A]], N[F[A]], A, A]
  object ElgotAlgebraIso {
    def apply[W[_], N[_], F[_], A](φ: ElgotAlgebra[W, F, A])(ψ: ElgotCoalgebra[N, F, A])
        : ElgotAlgebraIso[W, N, F, A] =
      PIso(φ)(ψ)
  }

  type AlgebraIso[F[_], A] = GAlgebraIso[Id, Id, F, A]
  object AlgebraIso {
    def apply[F[_], A](φ: Algebra[F, A])(ψ: Coalgebra[F, A]):
        AlgebraIso[F, A] =
      Iso(φ)(ψ)
  }

  type AlgebraPrism[F[_], A] = Prism[F[A], A]
  object AlgebraPrism {
    def apply[F[_], A](φ: AlgebraM[Option, F, A])(ψ: Coalgebra[F, A]):
        AlgebraPrism[F, A] =
      Prism(φ)(ψ)
  }

  type CoalgebraPrism[F[_], A] = Prism[A, F[A]]
  object CoalgebraPrism {
    def apply[F[_], A](ψ: CoalgebraM[Option, F, A])(φ: Algebra[F, A]):
        CoalgebraPrism[F, A] =
      Prism(ψ)(φ)
  }

  /** There is a fold/unfold isomorphism for any AlgebraIso.
    */
  def foldIso[T, F[_]: Functor, A]
    (alg: AlgebraIso[F, A])
    (implicit T: Birecursive.Aux[T, F]) =
    Iso[T, A](_.cata(alg.get))(_.ana[T](alg.reverseGet))

  /** There is a fold prism for any AlgebraPrism.
    */
  def foldPrism[T, F[_]: Traverse, A]
    (alg: AlgebraPrism[F, A])
    (implicit T: Birecursive.Aux[T, F]) =
    Prism[T, A](T.cataM(_)(alg.getOption))(_.ana[T](alg.reverseGet))

  /** There is an unfold prism for any CoalgebraPrism.
    */
  def unfoldPrism[T, F[_]: Traverse, A]
    (coalg: CoalgebraPrism[F, A])
    (implicit T: Birecursive.Aux[T, F]) =
    Prism[A, T](_.anaM[T](coalg.getOption))(_.cata(coalg.reverseGet))

  /** A NaturalTransformation that sequences two types
    *
    * @group dist
    */
  type DistributiveLaw[F[_], G[_]] = (F ∘ G)#λ ~> (G ∘ F)#λ

  /** The hylomorphism is the fundamental operation of recursion schemes. It
    * first applies `ψ`, recursively breaking an `A` into layers of `F`, then
    * applies `φ`, combining the `F`s into a `B`. It can also be seen as the
    * (fused) composition of an anamorphism and a catamorphism that avoids
    * building the intermediate recursive data structure.
    *
    * @group refolds
    */
  @SuppressWarnings(Array("org.wartremover.warts.Recursion"))
  def hylo[F[_]: Functor, A, B](a: A)(φ: Algebra[F, B], ψ: Coalgebra[F, A]): B =
    φ(ψ(a).map(hylo(_)(φ, ψ)))

  /** A Kleisli hylomorphism.
    *
    * @group refolds
    */
  def hyloM[M[_], F[_], A, B]
    (a: A)
    (f: AlgebraM[M, F, B], g: CoalgebraM[M, F, A])
    (implicit M: Monad[M], F: Traverse[F])
      : M[B] =
    hylo[(M ∘ F)#λ, A, M[B]](a)(_ >>= (_.sequence >>= f), g)(M compose F)

  /** Composition of an elgot-anamorphism and an elgot-catamorphism that avoids
    * building the intermediate recursive data structure.
    *
    * `elgotCata <<< elgotAna`
    *
    * @group refolds
    */
  def elgotHylo[W[_]: Comonad, N[_]: Monad, F[_]: Functor, A, B]
      (a: A)
      (kφ: DistributiveLaw[F, W], kψ: DistributiveLaw[N, F], φ: ElgotAlgebra[W, F, B], ψ: ElgotCoalgebra[N, F, A])
        : B = {
    lazy val trans: N[A] => W[B] =
      na => loop(na >>= ψ) coflatMap φ
    lazy val loop: N[F[A]] => W[F[B]] =
      (kψ[A] _) >>> (_ map trans) >>> kφ[B]
    (ψ >>> loop >>> φ)(a)
  }

  /** `histo <<< ana`
    *
    * @group refolds
    */
  def dyna[F[_]: Functor, A, B](a: A)(φ: F[Cofree[F, B]] => B, ψ: Coalgebra[F, A]): B =
    ghylo[Cofree[F, ?], Id, F, A, B](a)(distHisto, distAna, φ, ψ)

  /** `cata <<< futu`
    *
    * @group refolds
    */
  def codyna[F[_]: Functor, A, B](a: A)(φ: Algebra[F, B], ψ: GCoalgebra[Free[F, ?], F, A]): B =
    ghylo[Id, Free[F, ?], F, A, B](a)(distCata, distFutu, φ, ψ)

  /** `cataM <<< futuM`
    *
    * @group refolds
    */
  def codynaM[M[_]: Monad, F[_]: Traverse, A, B](a: A)(φ: AlgebraM[M, F, B], ψ: GCoalgebraM[Free[F, ?], M, F, A]): M[B] =
    ghyloM[Id, Free[F, ?], M, F, A, B](a)(distCata, distFutu, φ, ψ)

  /** A generalized version of a hylomorphism that composes any coalgebra and
    * algebra. (`gcata <<< gana`)
    *
    * @group refolds
    */
  def ghylo[W[_]: Comonad, N[_]: Monad, F[_]: Functor, A, B](
    a: A)(
    w: DistributiveLaw[F, W],
    n: DistributiveLaw[N, F],
    f: GAlgebra[W, F, B],
    g: GCoalgebra[N, F, A]):
      B =
    hylo[Yoneda[F, ?], N[A], W[B]](
      a.pure[N])(
      fwb => w((fwb.map(_.coflatten)).run).map(f), na => Yoneda(n(na.map(g))).map(_.flatten)).extract

  /** A Kleisli `ghylo` (`gcataM <<< ganaM`)
    *
    * @group refolds
    */
  def ghyloM[W[_]: Comonad: Traverse, N[_]: Monad: Traverse, M[_]: Monad, F[_]: Traverse, A, B](
    a: A)(
    w: DistributiveLaw[F, W],
    n: DistributiveLaw[N, F],
    f: GAlgebraM[W, M, F, B],
    g: GCoalgebraM[N, M, F, A]):
      M[B] =
    hyloM[M, F, N[A], W[B]](
      a.pure[N])(
      fwb => w(fwb.map(_.coflatten)).traverse(f),
        na => na.traverse(g).map(n(_).map(_.flatten))).map(_.extract)

  /** Similar to a hylomorphism, this composes a futumorphism and a
    * histomorphism.
    *
    * @group refolds
    */
  def chrono[F[_]: Functor, A, B](
    a: A)(
    g: GAlgebra[Cofree[F, ?], F, B], f: GCoalgebra[Free[F, ?], F, A]):
      B =
    ghylo[Cofree[F, ?], Free[F, ?], F, A, B](a)(distHisto, distFutu, g, f)

  /** `cata <<< elgotGApo`
    *
    * @group refolds
    */
  def elgot[F[_], A, B]
    (a: A)
    (φ: Algebra[F, B], ψ: ElgotCoalgebra[Either[B, ?], F, A])
    (implicit F: Functor[F])
      : B =
    hylo[(Either[B, ?] ∘ F)#λ, A, B](a)(_.swap.valueOr(φ), ψ)(Functor[Either[B, ?]] compose F)

  /** `cataM <<< elgotGApoM`
    *
    * @group refolds
    */
  def elgotM[M[_], F[_], A, B]
    (a: A)
    (φ: AlgebraM[M, F, B], ψ: ElgotCoalgebraM[Either[B, ?], M, F, A])
    (implicit M: Monad[M], F: Traverse[F])
      : M[B] =
    hyloM[M, (Either[B, ?] ∘ F)#λ, A, B](
      a)(
      _.fold(_.pure[M], φ), ψ)(
      M, Traverse[Either[B, ?]] compose F)

  /** `elgotZygo <<< ana`
    *
    * @group refolds
    */
  def coelgot[F[_], A, B]
    (a: A)
    (φ: ElgotAlgebra[(A, ?), F, B], ψ: Coalgebra[F, A])
    (implicit F: Functor[F])
      : B =
    hylo[((A, ?) ∘ F)#λ, A, B](a)(φ, a => (a, ψ(a)))(Functor[(A, ?)] compose F)

  /** `elgotZygoM <<< anaM`
    *
    * @group refolds
    */
  object coelgotM {
    def apply[M[_]] = new PartiallyApplied[M]
    final class PartiallyApplied[M[_]] {
      def apply[F[_], A, B]
        (a: A)
        (φ: ElgotAlgebraM[(A, ?), M, F, B], ψ: CoalgebraM[M, F, A])
        (implicit M: Monad[M], F: Traverse[F])
          : M[B] =
        hyloM[M, ((A, ?) ∘ F)#λ, A, B](
          a)(
          φ, a => ψ(a).map((a, _)))(
          M, Traverse[(A, ?)] compose F)
    }
  }

  def transHylo[T, F[_]: Functor, G[_]: Functor, U, H[_]: Functor]
    (t: T)
    (φ: G[U] => H[U], ψ: F[T] => G[T])
    (implicit T: Recursive.Aux[T, F], U: Corecursive.Aux[U, H])
      : U =
    hylo(t)(φ >>> (U.embed(_)), ψ <<< (T.project(_)))

  /**
    *
    * @group dist
    */
  def distPara[T, F[_]: Functor](implicit T: Corecursive.Aux[T, F])
      : DistributiveLaw[F, (T, ?)] =
    distZygo[F, T](T.embed(_))

  /**
    *
    * @group dist
    */
  def distParaT[T, W[_]: Comonad, F[_]: Functor]
    (t: DistributiveLaw[F, W])
    (implicit T: Corecursive.Aux[T, F])
      : DistributiveLaw[F, EnvT[T, W, ?]] =
    distZygoT[F, W, T](_.embed, t)

  /**
    *
    * @group dist
    */
  def distCata[F[_]]: DistributiveLaw[F, Id] = FunctionK.id

  /** A general [[DistributiveLaw]] for the case where the [[scalaz.Comonad]] is
    * also [[scalaz.Applicative]].
    *
    * @group dist
    */
  def distApplicative[F[_]: Traverse, G[_]: Applicative] =
    new DistributiveLaw[F, G] {
      def apply[A](fga: F[G[A]]) = fga.sequence
    }

  /** A general [[DistributiveLaw]] for the case where the [[scalaz.Comonad]] is
    * also [[scalaz.Distributive]].
    *
    * @group dist
    */
  // def distDistributive[F[_]: Functor, G[_]: Distributive] =
  //   new DistributiveLaw[F, G] {
  //     def apply[A](fga: F[G[A]]) = fga.cosequence
  //   }

  /**
    *
    * @group dist
    */
  def distZygo[F[_]: Functor, B](g: Algebra[F, B]) =
    new DistributiveLaw[F, (B, ?)] {
      def apply[α](m: F[(B, α)]) = (g(m.map(_._1)), m.map(_._2))
    }

  /**
    *
    * @group dist
    */
  def distZygoM[F[_]: Functor, M[_]: Monad, B](
    g: AlgebraM[M, F, B], k: DistributiveLaw[F, M]) =
    new DistributiveLaw[F, (M ∘ (B, ?))#λ] {
      def apply[α](fm: F[M[(B, α)]]) = {
        k(fm) >>= { f => g(f.map(_._1)).map((_, f.map(_._2))) }
      }
    }

  /**
    *
    * @group dist
    */
  def distZygoT[F[_]: Functor, W[_]: Comonad, B](
    g: Algebra[F, B], k: DistributiveLaw[F, W]) =
    new DistributiveLaw[F, EnvT[B, W, ?]] {
      def apply[α](fe: F[EnvT[B, W, α]]) =
        EnvT((g(fe.map(_.ask)), k(fe.map(_.lower))))
    }

  /**
    *
    * @group dist
    */
  def distHisto[F[_]: Functor] =
    new DistributiveLaw[F, Cofree[F, ?]] {
      def apply[α](m: F[Cofree[F, α]]) =
        distGHisto[F, F](FunctionK.id[λ[α => F[F[α]]]]).apply(m)
    }

  /**
    *
    * @group dist
    */
  // TODO: Should be able to generalize this over `Recursive.Aux[T, EnvT[…]]`
  //       somehow, then it wouldn’t depend on Scalaz and would work with any
  //       `Cofree` representation.
  def distGHisto[F[_]: Functor,  H[_]: Functor](k: DistributiveLaw[F, H]) =
    new DistributiveLaw[F, Cofree[H, ?]] {
      def apply[α](m: F[Cofree[H, α]]) =
        m.ana[Cofree[H, F[α]]](as => EnvT[F[α], H, F[Cofree[H, α]]]((as.map(_.extract), k(as.map(_.tail.value)))))
    }

  /**
    *
    * @group dist
    */
  def distAna[F[_]]: DistributiveLaw[Id, F] = FunctionK.id

  /**
    *
    * @group dist
    */
  def distApo[T, F[_]: Functor](implicit T: Recursive.Aux[T, F])
      : DistributiveLaw[Either[T, ?], F] =
    distGApo[F, T](T.project(_))

  /**
    *
    * @group dist
    */
  def distGApo[F[_]: Functor, B](g: Coalgebra[F, B]) =
    new DistributiveLaw[Either[B, ?], F] {
      def apply[α](m: Either[B, F[α]]) =
        m.fold(g(_).map(_.asLeft), _.map(_.asRight))
    }

  /** Allows for more complex unfolds, like
    * `futuGApo(φ0: Coalgebra[F, B], φ: GCoalgebra[λ[α => EitherT[Free[F, ?], B, α]], F, A])`
    *
    * @group dist
    */
  def distGApoT[F[_]: Functor, M[_]: Functor, B](
    g: Coalgebra[F, B], k: DistributiveLaw[M, F]) =
    new DistributiveLaw[EitherT[M, B, ?], F] {
      def apply[α](m: EitherT[M, B, F[α]]) =
        k(m.value.map(distGApo(g).apply(_))).map(EitherT(_))
    }

  /**
    *
    * @group dist
    */
  def distFutu[F[_]: Functor] =
    new DistributiveLaw[Free[F, ?], F] {
      def apply[α](m: Free[F, F[α]]) =
        distGFutu[F, F](FunctionK.id[λ[α => F[F[α]]]]).apply(m)
    }

  /**
    *
    * @group dist
    */
  // TODO: Should be able to generalize this over `Recursive.Aux[T, CoEnv[…]]`
  //       somehow, then it wouldn’t depend on Scalaz and would work with any
  //       `Free` representation.
  def distGFutu[H[_]: Functor, F[_]: Functor](k: DistributiveLaw[H, F])
      : DistributiveLaw[Free[H, ?], F] =
    new DistributiveLaw[Free[H, ?], F] {
      def apply[A](m: Free[H, F[A]]) =
        m.cata[F[Free[H, A]]](_.run.fold(_.map(Free.pure), k(_).map(Free.roll)))
    }

  /** Turns any F-algebra, into a transform that attributes the tree with
    * the results for each node.
    *
    * @group algtrans
    */
  object attributeAlgebraM {
    def apply[T] = new PartiallyApplied[T]

    final class PartiallyApplied[T] {
      def apply[F[_]: Functor, M[_]: Functor, A]
        (f: AlgebraM[M, F, A])
        (implicit T: Recursive.Aux[T, EnvT[A, F, ?]])
          : TransformM[M, T, F, EnvT[A, F, ?]] =
        fa => f(fa.map(_.project.ask)).map(a => EnvT((a, fa)))
    }
  }

  /**
    *
    * @group algtrans
    */
  object attributeAlgebra {
    def apply[T] = new PartiallyApplied[T]

    final class PartiallyApplied[T] {
      def apply[F[_]: Functor, A]
        (f: Algebra[F, A])
        (implicit T: Recursive.Aux[T, EnvT[A, F, ?]])
          : Transform[T, F, EnvT[A, F, ?]] =
        attributeAlgebraM[T][F, Id, A](f)
    }
  }

  /**
    *
    * @group algtrans
    */
  def attributeCoalgebra[F[_], B](ψ: Coalgebra[F, B]):
      Coalgebra[EnvT[B, F, ?], B] =
    b => EnvT[B, F, B]((b, ψ(b)))

  /** Useful for ignoring the annotation when folding a cofree. */
  def deattribute[F[_], A, B](φ: Algebra[F, B]): Algebra[EnvT[A, F, ?], B] =
    ann => φ(ann.lower)

  def forgetAnnotation[T, R, F[_]: Functor, A]
    (t: T)
    (implicit T: Recursive.Aux[T, EnvT[A, F, ?]], R: Corecursive.Aux[R, F])
      : R =
    t.cata(deattribute[F, A, R](_.embed))

  // def ignoreAttribute[T[_[_]], F[_], G[_], A](φ: F[T[G]] => G[T[G]]):
  //     EnvT[A, F, T[EnvT[A, G, ?]]] => EnvT[A, G, T[EnvT[A, G, ?]]] =
  //   ???

  /**
    *
    * @group algtrans
    */
  object attrK {
    def apply[T] = new PartiallyApplied[T]

    final class PartiallyApplied[T] {
      def apply[F[_]: Functor, A]
        (k: A)
        (implicit T: Recursive.Aux[T, EnvT[A, F, ?]])
          : Transform[T, F, EnvT[A, F, ?]] =
        attributeAlgebra[T](Function.const[A, F[A]](k))
    }
  }

  /**
    *
    * @group algtrans
    */
  object attrSelf {
    def apply[T] = new PartiallyApplied[T]

    final class PartiallyApplied[T] {
      def apply[U, F[_]: Functor]
        (implicit T: Recursive.Aux[T, EnvT[U, F, ?]], U: Corecursive.Aux[U, F])
          : Transform[T, F, EnvT[U, F, ?]] =
        attributeAlgebra[T](U.embed(_: F[U]))
    }
  }

  /** A function to be called like `attributeElgotM[M](myElgotAlgebraM)`.
    *
    * @group algtrans
    */
  object attributeElgotM {
    def apply[W[_], M[_], T] = new PartiallyApplied[W, M, T]

    final class PartiallyApplied[W[_], M[_], T] {
      def apply[F[_]: Functor, A]
        (f: ElgotAlgebraM[W, M, F, A])
        (implicit W: Comonad[W], M: Functor[M], T: Recursive.Aux[T, EnvT[A, F, ?]])
          : AlgebraicElgotTransformM[W, M, T, F, EnvT[A, F, ?]] =
        node => f(node.map(_.map(_.project.ask))).map(a => EnvT((a, node.extract)))
    }
  }

  /**
    *
    * @group algtrans
    */
  object attributeElgot {
    def apply[W[_], T] = new PartiallyApplied[W, T]

    final class PartiallyApplied[W[_], T] {
      def apply[F[_]: Functor, A]
        (f: ElgotAlgebra[W, F, A])
        (implicit W: Comonad[W], T: Recursive.Aux[T, EnvT[A, F, ?]])
          : AlgebraicElgotTransform[W, T, F, EnvT[A, F, ?]] =
        attributeElgotM[W, Id, T](f)
    }
  }

  /** Makes it possible to use ElgotAlgebras on EnvT.
    */
  def liftT[F[_], A, B](φ: ElgotAlgebra[(A, ?), F, B]):
      Algebra[EnvT[A, F, ?], B] =
    ann => φ(ann.run)

  /** Makes it possible to use ElgotAlgebras on EnvT.
    */
  def liftTM[M[_], F[_], A, B](φ: ElgotAlgebraM[(A, ?), M, F, B]):
      AlgebraM[M, EnvT[A, F, ?], B] =
    ann => φ(ann.run)

  def runT[F[_], A, B](ψ: ElgotCoalgebra[Either[A, ?], F, B])
      : Coalgebra[CoEnv[A, F, ?], B] =
    b => CoEnv(ψ(b))

  /**
    *
    * @group algtrans
    */
  implicit def GAlgebraSemigroupal[W[_]: Functor, F[_]: Functor]:
      Semigroupal[GAlgebra[W, F, ?]] =
    new Semigroupal[GAlgebra[W, F, ?]] {
      def product[A, B](a: GAlgebra[W, F, A], b: GAlgebra[W, F, B]) =
        node => (a(node.map(_.map(_._1))), b(node.map(_.map(_._2))))
    }
  /**
    *
    * @group algtrans
    */
  implicit def AlgebraSemigroupal[F[_]: Functor] = GAlgebraSemigroupal[Id, F]

  /**
    *
    * @group algtrans
    */
  implicit def ElgotAlgebraMSemigroupal[W[_]: Functor, M[_]: Applicative, F[_]: Functor]:
      Semigroupal[ElgotAlgebraM[W, M, F, ?]] =
    new Semigroupal[ElgotAlgebraM[W, M, F, ?]] {
      def product[A, B](a: ElgotAlgebraM[W, M, F, A], b: ElgotAlgebraM[W, M, F, B]) =
        w => Bitraverse[(?, ?)].bisequence((a(w.map(_.map(_._1))), b(w.map(_.map(_._2)))))
    }
  /**
    *
    * @group algtrans
    */
  implicit def ElgotAlgebraSemigroupal[W[_]: Functor, F[_]: Functor] =
    ElgotAlgebraMSemigroupal[W, Id, F]
    // (ann, node) => node.unfzip.bimap(f(ann, _), g(ann, _))

  final def repeatedlyƒ[A](f: A => Option[A]): Coalgebra[Either[A, ?], A] =
    a => f(a).toRight(a)

  /** Repeatedly applies the function to the result as long as it returns Some.
    * Finally returns the last non-None value (which may be the initial input).
    *
    * @group algtrans
    */
  object repeatedly {
    def apply[P] = new PartiallyApplied[P]
    class PartiallyApplied[P] {
      def apply[A](f: A => Option[A])(implicit P: Corecursive.Aux[P, Either[A, ?]])
          : A => P =
        _.ana[P](repeatedlyƒ(f))
    }
  }

  /** Converts a failable fold into a non-failable, by simply returning the
    * argument upon failure.
    *
    * @group algtrans
    */
  def orOriginal[A](f: A => Option[A]): A => A = expr => f(expr).getOrElse(expr)

  /** Converts a failable fold into a non-failable, by returning the default
    * upon failure.
    *
    * @group algtrans
    */
  def orDefault[A, B](default: B)(f: A => Option[B]): A => B =
    expr => f(expr).getOrElse(default)

  /** Count the instances of `form` in the structure.
    *
    * @group algebras
    */
  def count[T: Eq, F[_]: Functor: Foldable]
    (form: T)
    (implicit T: Recursive.Aux[T, F])
      : ElgotAlgebra[(T, ?), F, Int] =
    e => (if (e._1 === form) 1 else 0) + e._2.foldRight(Eval.now(0))((a, b) => b.map(a + _)).value

  /** The number of nodes in this structure.
    *
    * @group algebras
    */
  object size {
    def apply[N] = new PartiallyApplied[N]
    class PartiallyApplied[N] {
      def apply[F[_]: Foldable](implicit N: Birecursive.Aux[N, Option])
          : Algebra[F, N] =
        _.foldRight(Now(Nat.one[N]))((a, b) => b.map(a + _)).value
    }
  }

  /** The largest number of hops from a node to a leaf.
    *
    * @group algebras
    */
  def height[F[_]: Foldable]: Algebra[F, Int] =
    _.foldRight(Eval.now(-1))((a, b) => b.map(a max _)).map(_ + 1).value

  /** Collects the set of all subtrees.
    *
    * @group algebras
    */
  def universe[F[_]: Foldable, A]: ElgotAlgebra[(A, ?), F, NonEmptyList[A]] =
    p => NonEmptyList(p._1, p._2.toList.unite)

  /** Combines a tuple of zippable functors.
    *
    * @group algebras
    */
  def zipTuple[T, F[_]: Functor: Semigroupal](implicit T: Recursive.Aux[T, F])
      : Coalgebra[F, (T, T)] =
    p => Semigroupal[F].product[T, T](p._1.project, p._2.project)

  /** Aligns “These” into a single structure, short-circuting when we hit a
    * “This” or “That”.
    *
    * @group algebras
    */
  // def alignThese[T, F[_]: Align](implicit T: Recursive.Aux[T, F])
  //     : ElgotCoalgebra[Either[T, ?], F, Ior[T, T]] =
  //   _.fold(_.asLeft, _.asLeft, (a, b) => a.project.align(b.project).asRight)

  /** Merges a tuple of functors, if possible.
    *
    * @group algebras
    */
  def mergeTuple[T, F[_]: Functor: Merge](implicit T: Recursive.Aux[T, F])
      : CoalgebraM[Option, F, (T, T)] =
    p => Merge[F].merge[T, T](p._1.project, p._2.project)

  /** Generates an infinite sequence from two seed values.
    *
    * @group algebras
    */
  def binarySequence[A](relation: (A, A) => A): Coalgebra[(A, ?), (A, A)] = {
    case (a1, a2) =>
      val c = relation(a1, a2)
      (c, (a2, c))
  }

  def partition[A: Order]: Coalgebra[λ[α => Option[(A, (α, α))]], List[A]] = {
    case Nil    => None
    case h :: t => (h, (t.filter(_ <= h), t.filter(_ > h))).some
  }

  def join[A]: Algebra[λ[α => Option[(A, (α, α))]], List[A]] =
    _.fold[List[A]](Nil) { case (a, (x, y)) => x ++ (a :: y) }

  // NB: not in-place
  def quicksort[A: Order](as: List[A]): List[A] = {
    implicit val F: Functor[λ[α => Option[(A, (α, α))]]] =
      new Functor[λ[α => Option[(A, (α, α))]]] {
        def map[B, C] (fa: Option[(A, (B, B))])(f: B => C) =
          fa.map(_.map(_.bimap(f, f)))
      }
    as.hylo[λ[α => Option[(A, (α, α))]], List[A]](join, partition)
  }

  /** Converts a fixed-point structure into a generic Tree.
    * One use of this is using `.cata(toTree).drawTree` rather than `.show` to
    * get a pretty-printed tree.
    *
    * @group algebras
    */
  // def toTree[F[_]: Functor: Foldable]: Algebra[F, Tree[F[Unit]]] =
  //   x => Tree.Node(x.void, x.toStream)

  /** Returns (on the left) the first element that passes `f`.
    */
  def find[T](f: T => Boolean): T => Either[T, T] =
    tf => if (f(tf)) tf.asLeft else tf.asRight

  /** Replaces all instances of `original` in the structure with `replacement`.
    *
    * @group algebras
    */
  def substitute[T: Eq](original: T, replacement: T): T => Either[T, T] =
    find[T](_ === original)(_).leftMap(_ => replacement)

  /** This implicit allows Delay implicits to be found when searching for a
    * traditionally-defined instance.
    */
  implicit def delayEq[F[_], A](implicit F: Delay[Eq, F], A: Eq[A])
      : Eq[F[A]] =
    F(A)

  implicit def delayOrder[F[_], A](implicit F: Delay[Order, F], A: Order[A])
      : Order[F[A]] =
    F(A)

  /** See `delayEq`.
    */
  implicit def delayShow[F[_], A](implicit F: Delay[Show, F], A: Show[A]):
      Show[F[A]] =
    F(A)

  implicit def recursiveTRecursive[T[_[_]]: RecursiveT, F[_]]: Recursive.Aux[T[F], F] =
    new Recursive[T[F]] {
      type Base[A] = F[A]

      def project(t: T[F])(implicit F: Functor[F]) =
        RecursiveT[T].projectT[F](t)
      override def cata[A](t: T[F])(f: Algebra[F, A])(implicit F: Functor[F]) =
        RecursiveT[T].cataT[F, A](t)(f)
    }

  implicit def corecursiveTCorecursive[T[_[_]]: CorecursiveT, F[_]]: Corecursive.Aux[T[F], F] =
    new Corecursive[T[F]] {
      type Base[A] = F[A]

      def embed(t: F[T[F]])(implicit F: Functor[F]) =
        CorecursiveT[T].embedT[F](t)
      override def ana[A](a: A)(f: Coalgebra[F, A])(implicit F: Functor[F]) =
        CorecursiveT[T].anaT[F, A](a)(f)
    }

  implicit def birecursiveTBirecursive[T[_[_]]: BirecursiveT, F[_]]: Birecursive.Aux[T[F], F] =
    new Birecursive[T[F]] {
      type Base[A] = F[A]

      def project(t: T[F])(implicit F: Functor[F]) =
        BirecursiveT[T].projectT[F](t)
      override def cata[A](t: T[F])(f: Algebra[F, A])(implicit F: Functor[F]) =
        BirecursiveT[T].cataT[F, A](t)(f)

      def embed(t: F[T[F]])(implicit F: Functor[F]) =
        BirecursiveT[T].embedT[F](t)
      override def ana[A](a: A)(f: Coalgebra[F, A])(implicit F: Functor[F]) =
        BirecursiveT[T].anaT[F, A](a)(f)
    }

  implicit def orderTOrder[T[_[_]], F[_]: Functor](implicit T: OrderT[T], F: Delay[Order, F]): Order[T[F]] =
    T.orderT[F](F)

  implicit def eqTEq[T[_[_]], F[_]: Functor](implicit T: EqT[T], F: Delay[Eq, F]): Eq[T[F]] =
    T.eqT[F](F)

  implicit def showTShow[T[_[_]], F[_]: Functor](implicit T: ShowT[T], F: Delay[Show, F]): Show[T[F]] =
    T.showT[F](F)

  implicit def birecursiveTFunctor[T[_[_]]: BirecursiveT, F[_, _]](implicit F: Bifunctor[F]): Functor[λ[α => T[F[α, ?]]]] =
    new Functor[λ[α => T[F[α, ?]]]] {
      def map[A, B](fa: T[F[A, ?]])(f: A => B) =
        fa.transCata[T[F[B, ?]]](_.leftMap[B](f))
    }

  // TODO: Get this into Cats proper.
  implicit class ExtraTraverseOps[F[_]: Traverse, A](fa: F[A]) {
    import cats.data._

    def zipWith[B, C](fb: F[B])(f: (A, Option[B]) => C): (List[B], F[C]) =
      fa.traverse[State[List[B], ?], C](a => for {
        bs <- State.get
        _ <- State.set(if (bs.isEmpty) bs else bs.drop(1))
      } yield f(a, bs.headOption)).run(fb.toList).value

    def zipWithL[B, C](fb: F[B])(f: (A, Option[B]) => C): F[C] = zipWith(fb)(f)._2
    def zipWithR[B, C](fb: F[B])(f: (Option[A], B) => C): F[C] = fb.zipWith(fa)((b,oa) => f(oa,b))._2
  }
}
