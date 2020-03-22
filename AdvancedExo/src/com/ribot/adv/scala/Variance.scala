package com.ribot.adv.scala

object Variance extends App {

  /*
    Invariant, covariant, contravariant
    Parking[T](things: List[T]) {
      park(vehicle: T)
      impound(vehicles: List[T])
      checkVehicles(conditions: String): List[T]
      flatMap(f: T => Parking[S]): Parking[S]
    }
   */

  class Vehicle
  class Bike extends Vehicle
  class Car extends Vehicle

  // Using Contravariant List

  // Invariant
  class IParking[T](vehicles: List[T]) {
    def park(vehicle: T): IParking[T] = ???
    def impound(vehicles: List[T]): IParking[T] = ???
    def checkVehicles(conditions: String): List[T] = ???
    def flatMap[S](f: T => IParking[S]): IParking[S] = ???
  }

  // Covariant
  class CParking[+T](vehicles: List[T]) {
    def park[S >: T](vehicle: S): CParking[S] = ???
    def impound[S >: T](vehicles: List[S]): CParking[S]= ???
    def checkVehicles(conditions: String): List[T] = ???
    def flatMap[S](f: T => IParking[S]): IParking[S] = ???
  }

  // Contravariant
  class XParking[-T](vehicles: List[T]) {
    def park(vehicle: T): XParking[T] = ???
    def impound(vehicles: List[T]): XParking[T] = ???
    def checkVehicles[S <: T](conditions: String): List[S] = ???
    def flatMap[R <: T, S](f: R => IParking[R]): IParking[S] = ???
  }


  // Using Invariant Custom List
  class IList[T]

  // Invariant 2
  class IParking2[T](vehicles: IList[T]) {
    def park(vehicle: T): IParking2[T] = ???
    def impound(vehicles: IList[T]): IParking2[T] = ???
    def checkVehicles(conditions: String): IList[T] = ???
    def flatMap[S](f: T => IParking[S]): IParking[S] = ???
  }

  // Covariant 2
  class CParking2[+T](vehicles: IList[T]) {
    def park[S >: T](vehicle: S): CParking2[S] = ???
    def impound[S >: T](vehicles: IList[S]): CParking2[S]= ???
    def checkVehicles[S >: T](conditions: String): IList[S] = ???
    def flatMap[S](f: T => IParking[S]): IParking[S] = ???
  }

  // Contravariant 2
  class XParking2[-T](vehicles: IList[T]) {
    def park(vehicle: T): XParking2[T] = ???
    def impound[S <: T](vehicles: IList[S]): XParking2[S] = ???
    def checkVehicles[S <: T](conditions: String): IList[S] = ???
    def flatMap[R <: T, S](f: R => IParking[S]): IParking[S] = ???
  }
}
