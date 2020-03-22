package com.ribot.adv.scala

import scala.annotation.tailrec

trait MySet[A] extends (A => Boolean) {

  def contains(elem: A): Boolean

  def +(elem: A): MySet[A]

  def ++(anotherSet: MySet[A]): MySet[A]

  def map[B](f: A => B): MySet[B]

  def flatMap[B](f: A => MySet[B]): MySet[B]

  def filter(predicate: A => Boolean): MySet[A]

  def foreach(f: A => Unit): Unit

  def apply(elem: A): Boolean = contains(elem)

  def -(elem: A): MySet[A]

  def &(anotherSet: MySet[A]): MySet[A] //union
  def --(anotherSet: MySet[A]): MySet[A] //difference
  def unary_! : MySet[A]
}

// ----------------------------------------------------------------------------

class EmptySet[A] extends MySet[A] {

  override def contains(elem: A): Boolean = false

  override def +(elem: A): MySet[A] = new NonEmptySet[A](elem, this)

  override def ++(anotherSet: MySet[A]): MySet[A] = anotherSet

  override def map[B](f: A => B): MySet[B] = new EmptySet[B]

  override def flatMap[B](f: A => MySet[B]): MySet[B] = new EmptySet[B]

  override def filter(predicate: A => Boolean): MySet[A] = this

  override def foreach(f: A => Unit): Unit = ()

  override def -(elem: A): MySet[A] = this

  override def &(anotherSet: MySet[A]): MySet[A] = this

  override def --(anotherSet: MySet[A]): MySet[A] = this

  override def unary_! : MySet[A] = new PropertyBasedSet[A](_ => true)
}

// ----------------------------------------------------------------------------

class NonEmptySet[A](head: A, tail: MySet[A]) extends MySet[A] {

  override def contains(elem: A): Boolean =
    elem == head || tail.contains(elem)

  override def +(elem: A): MySet[A] =
    if (this contains elem) this
    else new NonEmptySet[A](elem, this)

  /*
  [1 2 3] ++ [4 5]
  [2 3] ++ [4 5] + 1
  [3] ++ [4 5] + 1 + 2
  [] ++ [4 5] + 1 + 2 + 3
  do ++ on EnptySet
  [4 5] + 1 + 2 + 3
  [4 5 1 2 3]
   */
  override def ++(anotherSet: MySet[A]): MySet[A] =
    tail ++ anotherSet + head

  override def map[B](f: A => B): MySet[B] =
    (tail map f) + f(head)

  override def flatMap[B](f: A => MySet[B]): MySet[B] =
    (tail flatMap f) ++ f(head)

  override def filter(predicate: A => Boolean): MySet[A] = {
    val filteredTail = tail.filter(predicate)
    if (predicate(head)) filteredTail + head
    else filteredTail
  }

  override def foreach(f: A => Unit): Unit = {
    f(head)
    tail foreach f
  }

  override def -(elem: A): MySet[A] =
    if (head == elem) tail
    else tail - elem + head

  override def &(anotherSet: MySet[A]): MySet[A] = filter(anotherSet) //intersection == filtering   filter(x => another contains x)

  override def --(anotherSet: MySet[A]): MySet[A] = filter(!anotherSet) // filter(x => !another contains x)

  // negate set by having all elem not in set
  override def unary_! : MySet[A] = new PropertyBasedSet[A](x => !this.contains(x))
}

// ----------------------------------------------------------------------------

object MySet {
  def apply[A](values: A*): MySet[A] = {

    @tailrec
    def buildSet(valSeq: Seq[A], acc: MySet[A]): MySet[A] =
      if (valSeq.isEmpty) acc
      else buildSet(valSeq.tail, acc + valSeq.head)

    buildSet(values.toSeq, new EmptySet[A])
  }
}

// ----------------------------------------------------------------------------

// Include all elements of type A which satisfy a property
// {x in A | property(x) }
class PropertyBasedSet[A](property: A => Boolean) extends MySet[A] {

  override def contains(elem: A): Boolean = property(elem)

  // {x in A |property(x)} + elem = { x in A | property(x) || x == elem}
  override def +(elem: A): MySet[A] =
    new PropertyBasedSet[A](x => property(x) || x == elem)

  // {x in A | property(x)} ++ anotherSet = {x in A | property(x) || anotherSet contains x}
  override def ++(anotherSet: MySet[A]): MySet[A] =
    new PropertyBasedSet[A](x => property(x) || anotherSet(x)) // another(x) == another contains x

  // fail we do not know if finite or infinite set
  override def map[B](f: A => B): MySet[B] = fail

  override def flatMap[B](f: A => MySet[B]): MySet[B] = fail

  override def foreach(f: A => Unit): Unit = fail


  // only keep elem that fit property and predicate
  override def filter(predicate: A => Boolean): MySet[A] = new PropertyBasedSet[A](x => property(x) && predicate(x))

  // filter all not equal to elem
  override def -(elem: A): MySet[A] = filter(x => x != elem)

  // filter if x contains in anotherSet
  override def &(anotherSet: MySet[A]): MySet[A] = filter(anotherSet)

  // filter if x not contains in anotherSet
  override def --(anotherSet: MySet[A]): MySet[A] = filter(!anotherSet)

  // negate set = which does not fit property
  override def unary_! : MySet[A] = new PropertyBasedSet[A](x => !property(x))

  def fail = throw new IllegalArgumentException("Failed")
}

// ----------------------------------------------------------------------------

object MySetPlayground extends App {

  val s = MySet(1, 2, 3, 4)
  s + 5 ++ MySet(-1, -2, 3) flatMap (x => MySet(x, x * 10)) filter (_ % 2 == 0) foreach println

  val negative = !s
  println(negative(2))
  println(negative(5))

  val even = negative.filter(_ % 2 == 0)
  println(even(5))

  val evenPlus5 = even + 5
  println(evenPlus5(5))
}
