package core

import cats.kernel.laws.discipline.*
import cats.syntax.semigroup.*

import org.scalacheck.Arbitrary

import core.semigroups.*
import core.test.*

class SemigroupsSpec extends CoreSuite:
  given [A: Arbitrary]: Arbitrary[Min[A]] = Arbitrary(Arbitrary.arbitrary[A].map(Min[A]))

  checkAll("Semigroup[Min[Int]]", SemigroupTests[Min[Int]].semigroup)

  test("Semingroup of Min should combine two values") {
    assertEquals(Min(1) |+| Min(-1), Min(-1))
  }
