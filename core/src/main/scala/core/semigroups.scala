package core

import cats.{Eq, Order, Semigroup}

object semigroups:
  object min:
    opaque type Min[A] = A

    extension [A](x: Min[A]) def get: A = x

    object Min:
      def apply[A](x: A): Min[A] = x

      given [A: Order]: Semigroup[Min[A]] = Semigroup.instance(Order[A].min)
      given [A: Eq]: Eq[Min[A]]           = Eq[A]

  export min.*
