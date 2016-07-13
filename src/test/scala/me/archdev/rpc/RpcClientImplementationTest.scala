package me.archdev.rpc

import java.nio.ByteBuffer

import akka.stream.scaladsl.Tcp
import me.archdev.rpc.internal.RpcClientImplementation
import me.archdev.rpc.protocol._
import me.archdev.utils.AkkaTest
import org.scalatest.{Matchers, WordSpecLike}

import scala.concurrent.ExecutionContext.Implicits.global

class RpcClientImplementationTest extends AkkaTest with WordSpecLike with Matchers {

  "Rpc client" should {

    "create result promise when do request and increment id counter" in new Context {
      getIdCounter should be(0)
      formRpcRequest()
      getPromiseMapping.contains(0)
      getIdCounter should be(1)
    }

    "fill result promise on response and remove it from map" in new Context {
      val (_, promise) = formRpcRequest()
      finishRpcRequestWithSuccess()
      whenReady(promise.future)(_ should be(testByteBuffer))
    }

    "throw exception if remote method parameters is invalid" in new Context {
      val error = InvalidMethodParametersError(Seq("a", "b", "c"))
      val (_, promise) = formRpcRequest()
      finishRpcRequestWithError(error)
      whenReady(promise.future.failed)(_ should be(InvalidProtocolException(error.message)))
    }

    "throw exception if remote method not found" in new Context {
      val error = MethodNotFoundError("", "")
      val (_, promise) = formRpcRequest()
      finishRpcRequestWithError(error)
      whenReady(promise.future.failed)(_ should be(InvalidProtocolException(error.message)))
    }

    "throw exception if server thrown an exception" in new Context {
      val error = ExceptionIsThrownError(new RuntimeException("Yay"))
      val (_, promise) = formRpcRequest()
      finishRpcRequestWithError(error)
      whenReady(promise.future.failed) { case ex: RemoteRpcException =>
        ex.name should be(error.name)
        ex.message should be(error.exMessage)
        ex.stackTrace should be(error.stackTrace)
      }
    }

  }

  trait Context {

    val rpcClient = new RpcClientImplementation(Tcp().outgoingConnection("anywhere", 8080))()
    val testByteBuffer = ByteBuffer.allocate(0)

    def getIdCounter =
      rpcClient.idCounter.get

    def getPromiseMapping =
      rpcClient.promiseMapping

    def formRpcRequest() =
      rpcClient.formRPCRequest(Nil, Map())

    def finishRpcRequestWithSuccess() =
      rpcClient.finishRPCRequest(RpcResponse(0, Some(testByteBuffer), None))

    def finishRpcRequestWithError(errorProtocol: ErrorProtocol) =
      rpcClient.finishRPCRequest(RpcResponse(0, None, Some(errorProtocol)))

    def putDataInFlow(rpcRequest: RpcRequest) =
      rpcClient.flow.offer(rpcRequest)

  }

}