package me.archdev.it

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import me.archdev.rpc._
import me.archdev.utils.{TestService, TestServiceRouter}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, WordSpec}

import scala.concurrent.ExecutionContext
import scala.util.Random
import autowire._

class EasyRpcIT extends WordSpec with Matchers with ScalaFutures {

  implicit val system = ActorSystem()
  implicit val executionContext: ExecutionContext = system.dispatcher
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  trait Context {
    val rpcHost = "localhost"
    val rpcPort = 20000 + Random.nextInt(500)

    val rpcServer = new RpcServer(TestServiceRouter())
    rpcServer.launch(rpcHost, rpcPort)
    val rpcClient = new RpcClient[TestService](rpcHost, rpcPort)
  }

  "Rpc service" should {

    "perform call" in new Context {
      whenReady(rpcClient.echo("echo").call()) { echo =>
        "echo" should be(echo)
      }
    }

    "don't fall on exception" in new Context {
      whenReady(rpcClient.throwEx().call()) { _ =>
        whenReady(rpcClient.echo("echo").call()) { echo =>
          "echo" should be(echo)
        }
      }
    }

  }

}
