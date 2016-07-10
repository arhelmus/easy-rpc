package me.archdev.utils

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.testkit.TestKit
import org.scalatest.concurrent.ScalaFutures

import scala.concurrent.duration._

abstract class AkkaTest extends TestKit(ActorSystem()) with ScalaFutures {

  implicit val flowMaterializer = ActorMaterializer()
  val defaultTimeout = 500 millis

  implicit val p = PatienceConfig(timeout = defaultTimeout, interval = 100 millis)

  def testScope[T](f: => T): T =
    within(defaultTimeout)(f)

}