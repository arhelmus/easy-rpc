package me.archdev.it

import autowire._
import me.archdev.rpc._
import me.archdev.utils.{AkkaTest, TestService, TestServiceRouter}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, WordSpecLike}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Random

class EasyRpcIT extends AkkaTest with WordSpecLike with Matchers with ScalaFutures {

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
