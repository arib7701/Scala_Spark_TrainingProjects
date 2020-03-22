package com.ribot.adv.scala


class Lazy[+A](value: => A) {

  // call by need
  private lazy val internalValue = value     // so evaluation done only once

  def use: A = internalValue
  def flatMap[B](f: ( => A) => Lazy[B]) = f(internalValue)           // => receive parameter by name for lazy evaluation

}

object Lazy {

  def apply[A](value: => A) : Lazy[A] = new Lazy[A](value)

}

object LazyMonad extends App {

  val lazyInstance = Lazy {
    println("Hi")
    42
  }

  val flatMappedInstance = lazyInstance.flatMap(x => Lazy {
    10 * x
  })

  val flatMappedInstance2 = lazyInstance.flatMap(x => Lazy {
    10 * x
  })

  flatMappedInstance.use
  flatMappedInstance2.use

  /*
  - left-identity =>  unit.flatMap(f) = f(v)
  - right-identity => l.flatMap(unit) = l  Lazy(v).flatMap(x => Lazy(x)) = Lazy(v)
  - associativity  => Lazy(v).flatMap(f).flatMap(g) = f(v).flatMap(g)    Lazy(v).flatMap(x => f(x).flatMap(g)) = f(v).flatMap(g)
   */

}
