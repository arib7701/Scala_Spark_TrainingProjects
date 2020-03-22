package com.ribot.adv.scala

import scala.annotation.tailrec

/*
 naturals = MyStream.from(1)(x => x + 1) => stream of natural numbers (potentially infinite)
 naturals.take(100).foreach(println)  => lazily evaluated stream of the first 100 naturals
 naturals.foreach(println) => crash because stream is infinite
 naturals.map(_ * 2) ==> stream of all even numbers (potentially infinite)
*/

abstract class MyStream[+A] {
  def isEmpty: Boolean

  def head: A

  def tail: MyStream[A]

  // preprend
  def #::[B >: A](element: B): MyStream[B]

  // append
  def ++[B >: A](anotherStream: => MyStream[B]): MyStream[B]

  def foreach(f: A => Unit): Unit

  def map[B](f: A => B): MyStream[B]

  def flatMap[B](f: A => MyStream[B]): MyStream[B]

  def filter(predicate: A => Boolean): MyStream[A]

  def take(n: Int): MyStream[A]

  def takeAsList(n: Int): List[A] = take(n).toList()

  @tailrec
  final def toList[B >: A](acc: List[B] = Nil) : List[B] =
    if(isEmpty) acc.reverse
    else tail.toList(head :: acc)
}

// ----------------------------------------------------------------------

object MyStream {
  def from[A](start: A)(generator: A => A): MyStream[A] =
    new Cons(start, MyStream.from(generator(start))(generator))
}

// ----------------------------------------------------------------------

object EmptyStream extends MyStream[Nothing] {
  override def isEmpty: Boolean = true

  override def head: Nothing = throw new NoSuchElementException

  override def tail: MyStream[Nothing] = throw new NoSuchElementException

  override def #::[B >: Nothing](element: B): MyStream[B] = new Cons(element, this)

  override def ++[B >: Nothing](anotherStream: => MyStream[B]): MyStream[B] = anotherStream

  override def foreach(f: Nothing => Unit): Unit = ()

  override def map[B](f: Nothing => B): MyStream[B] = this

  override def flatMap[B](f: Nothing => MyStream[B]): MyStream[B] = this

  override def filter(predicate: Nothing => Boolean): MyStream[Nothing] = this

  override def take(n: Int): MyStream[Nothing] = this
}

// ----------------------------------------------------------------------

class Cons[+A](hd: A, tl: => MyStream[A]) extends MyStream[A] {    // => call by name to enable lazy evaluation
  override def isEmpty: Boolean = false

  override val head: A = hd
  override lazy val tail: MyStream[A] = tl // call by need

  override def #::[B >: A](element: B): MyStream[B] = new Cons(element, this)

  override def ++[B >: A](anotherStream: => MyStream[B]): MyStream[B] = new Cons(head, tail ++ anotherStream)    // => call by name to enable lazy evaluation

  override def foreach(f: A => Unit): Unit = {
    f(head)
    tail.foreach(f)
  }

  override def map[B](f: A => B): MyStream[B] = new Cons(f(head), tail.map(f)) // tail.map(f) lazy evaluated when needed

  override def flatMap[B](f: A => MyStream[B]): MyStream[B] = f(head) ++ tail.flatMap(f) // tail.flatMap(f) lazy evaluated when needed

  override def filter(predicate: A => Boolean): MyStream[A] =
    if (predicate(head)) new Cons(head, tail.filter(predicate)) // tail.filter(predicate) lazy evaluated when needed
    else tail.filter(predicate)

  override def take(n: Int): MyStream[A] =
    if (n <= 0) EmptyStream
    else if (n == 1) new Cons(head, EmptyStream)
    else new Cons(head, tail.take(n - 1)) // tail.take(n-1) lazy evaluated when needed
}
// ----------------------------------------------------------------------

object LazyStream extends App {

  val naturals = MyStream.from(1)(_ + 1)    // do not crash since tail is lazy evaluated
  println(naturals.head)
  println(naturals.tail.head)
  println(naturals.tail.tail.head)

  val startFrom0 = 0 #:: naturals   // naturals.#::(0)
  println(startFrom0.head)

  startFrom0.take(10000).foreach(println)

  println(startFrom0.map( _ * 2).take(100).toList())
  println(startFrom0.flatMap(x => new Cons(x, new Cons(x+1, EmptyStream))).take(10).toList())
  println(startFrom0.filter(_ < 10).take(10).toList())


  // Stream of Fibonacci numbers
  def fibonacci(first: BigInt, second: BigInt): MyStream[BigInt] =
    new Cons(first, fibonacci(second, first + second))

    println(fibonacci(1,1).take(100).toList())

  // Stream of Prime Numbers with Eratosthenes' sieve
  def eratosthenes(numbers: MyStream[Int]): MyStream[Int] =
    if(numbers.isEmpty) numbers
    else new Cons(numbers.head, eratosthenes(numbers.tail.filter(_ % numbers.head != 0)))

  println(eratosthenes(MyStream.from(2)(_ + 1)).take(100).toList())
}
