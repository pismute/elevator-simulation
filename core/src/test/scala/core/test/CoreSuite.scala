package core.test

import cats.{Eval, Functor}
import cats.syntax.functor.*
import munit.*
import org.scalacheck.Gen

import scala.compiletime.*
import scala.concurrent.Future
import scala.concurrent.duration.{Duration, FiniteDuration}

import java.util.concurrent.TimeUnit

trait CoreSuite extends DisciplineSuite with AssertionsF with ScalacheckGens:
  override def munitValueTransforms: List[ValueTransform] =
    munitEvalTransform :: super.munitValueTransforms

  // A hack for Matchable marker. munit can not match it with -new-syntax.
  private def matchable(pf: PartialFunction[Matchable, Future[Any]]): PartialFunction[Any, Future[Any]] =
    pf.compose(_.asMatchable)

  private val munitEvalTransform: ValueTransform =
    new ValueTransform(
      "Eval",
      matchable { case e: Eval[?] => Future(e.value)(munitExecutionContext) }
    )

  override def munitTimeout: Duration = new FiniteDuration(10, TimeUnit.SECONDS)

trait AssertionsF { self: Assertions =>
  def assertEqualsF[F[_]: Functor, A, B](
    obtained: F[A],
    expected: B,
    clue: => Any = "values are not the same"
  )(using loc: Location, compare: Compare[A, B]): F[Unit] =
    obtained.map(a => assertEquals(a, expected, clue))

  // `extension [F[_]: Functor, A](obtained: F[A])` doesn't work
  implicit class AssertionsFOps[F[_]: Functor, A](obtained: F[A]):
    def assertEquals[B](
      expected: B,
      clue: => Any = "values are not the same"
    )(using loc: Location, compare: Compare[A, B]): F[Unit] =
      assertEqualsF(obtained, expected, clue)
}

trait ScalacheckGens:
  def nonNegNum[A](using num: Numeric[A], c: Gen.Choose[A]): Gen[A] =
    Gen.sized(n => c.choose(num.zero, num.max(num.fromInt(n), num.one)))
