package me.archdev.rpc

import java.nio.ByteBuffer

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, WordSpec}

import scala.concurrent.ExecutionContext
import boopickle.Default._

class RpcTest extends WordSpec with Matchers with ScalaFutures {

  trait EchoService {
    def echo(message: String): String
  }

  class EchoServiceImpl extends EchoService {
    override def echo(message: String): String = message
  }

  trait Context {
    implicit val system = ActorSystem()
    implicit val executionContext: ExecutionContext = system.dispatcher
    implicit val materializer: ActorMaterializer = ActorMaterializer()

    val rpcServer = new RpcServer(Router.route[EchoService](new EchoServiceImpl))

    val rpcHost = "localhost"
    val rpcPort = 20000
  }

  "Rpc service" should {

    "serialize and deserialize RPC request" in new Context {
      val byteBuffer = ByteBuffer.allocate(1000)
      byteBuffer.put("test".toArray.map(_.toByte))

      val rpcRequest = RpcRequest(Seq("a", "b", "c"), Map("a" -> byteBuffer))

      val deserializedRequest = RpcRequest.deserialize(RpcRequest.serialize(rpcRequest))
      val deserializedByteBuffer = deserializedRequest.params("a")

      deserializedRequest should be(rpcRequest)
      deserializedByteBuffer should be(byteBuffer)
    }

    "bind server" in new Context {
      rpcServer.start(rpcHost, rpcPort + 1)
    }

    "perform call" in new Context {
      rpcServer.start(rpcHost, rpcPort)
      val rpcClient = new RpcClient(rpcHost, rpcPort)

      import autowire._
      val message = "echo"
      whenReady(rpcClient[EchoService].echo(message).call()) { echo =>
        message should be(echo)
      }
    }

  }

}
