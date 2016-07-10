package me.archdev.rpc

import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.util.ByteString
import me.archdev.utils.{AkkaTest, TestServiceRouter}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, WordSpecLike}

import scala.concurrent.ExecutionContext.Implicits.global

class RpcServerImplementationTest extends AkkaTest with WordSpecLike with Matchers with ScalaFutures {

  "Rpc Server implementation" should {

    "response on rpc request" in new Context {
      whenReady(rpcServerStream(TestServiceRouter.echoRequest("test")).run()) { r =>
        r should be(TestServiceRouter.echoResponse("test"))
      }
    }

    "send error if method cannot be found" in new Context {
      whenReady(rpcServerStream(TestServiceRouter.echoRequest("test").copy(path = Seq("wtf", "123"))).run()) { r =>
        Unpickle[MethodNotFoundException].fromBytes(r.asByteBuffer) should be(MethodNotFoundException("wtf", "123"))
      }
    }

    "send error if method parameters missing or invalid" in new Context {
      whenReady(rpcServerStream(TestServiceRouter.echoRequest("test").copy(params = Map())).run()) { r =>
        Unpickle[InvalidMethodParametersException].fromBytes(r.asByteBuffer) should be(InvalidMethodParametersException(Seq("message")))
      }
    }

    "send exception if it occurred during invoke" in new Context {
      whenReady(rpcServerStream(TestServiceRouter.throwRequest()).run()) { r =>
        val error = Unpickle[ErrorOccurredException].fromBytes(r.asByteBuffer)
        error.name should be("RuntimeException")
        error.message should be("Hey, we got an error!")
      }
    }

  }

  trait Context {

    val rpcServer = new RpcServerImplementation(TestServiceRouter())
    val rpcServerFlow = rpcServer.rpcServerFlow

    def rpcServerStream(byteString: ByteString) = {
      Source.single(byteString).via(rpcServerFlow).toMat(Sink.last)(Keep.right)
    }

    def rpcServerStream(rpcRequest: RpcRequest) = {
      val request = ByteString(RpcRequest.serialize(rpcRequest))
      Source.single(request).via(rpcServerFlow).toMat(Sink.last)(Keep.right)
    }

  }

}
