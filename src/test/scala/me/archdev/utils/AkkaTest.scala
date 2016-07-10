package me.archdev.utils

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.testkit.TestKit

import scala.concurrent.duration._

abstract class AkkaTest extends TestKit(ActorSystem()) {

  implicit val flowMaterializer = ActorMaterializer()
  val defaultTimeout = 500 millis

  def testScope[T](f: => T): T =
    within(defaultTimeout)(f)

}