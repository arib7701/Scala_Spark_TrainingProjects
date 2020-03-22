package com.ribot.adv.scala

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object MagnetPattern extends App {


  class P2PRequest

  class P2PResponse

  class Serializer[T]

  trait Actor {
    def receive(statusCode: Int): Int

    def receive(request: P2PRequest): Int

    def receive(response: P2PResponse): Int

    def receive[T: Serializer](message: T): Int // == def receive[T](message: T)(implict serializer: Serializer[T]): Int
    def receive[T: Serializer](message: T, statusCode: Int): Int

    def receive(future: Future[P2PRequest]): Int
  }


  /*
    Pb:
    - too many overloads
    - type erasure
    - lifting doesn't work for all overloads
    - code duplication
    - type inference and default args
   */

  // trait + receive methods == Magnet Pattern
  trait MessageMagnet[Result] {
    def apply(): Result
  }
  def receive[R](magnet: MessageMagnet[R]): R = magnet() // == magnet.apply()

  implicit class FromP2PRequest(request: P2PRequest) extends MessageMagnet[Int] {
    override def apply(): Int = {
      println("Handling P2P Request")
      42
    }
  }

  implicit class FromP2PResponse(response: P2PResponse) extends MessageMagnet[Int] {
    override def apply(): Int = {
      println("Handling P2P Response")
      24
    }
  }

  implicit class FromRequestFuture(future: Future[P2PRequest]) extends MessageMagnet[Int] {
    override def apply(): Int = 1
  }

  implicit class FromResponseFuture(future: Future[P2PResponse]) extends MessageMagnet[Int] {
    override def apply(): Int = 2
  }

  receive(new P2PRequest)
  receive(new P2PResponse)
  println(receive(Future(new P2PRequest)))
  println(receive(Future(new P2PResponse)))
}
