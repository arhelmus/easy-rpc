package me.archdev.rpc

import java.nio.ByteBuffer

import me.archdev.rpc.protocol.{ExceptionIsThrownError, RpcRequest, RpcResponse}
import org.scalatest.{Matchers, WordSpec}

class ProtocolTest extends WordSpec with Matchers {

  "serialize and deserialize RPC request" in {
    val byteBuffer = ByteBuffer.allocate(1000)
    byteBuffer.put("test".toArray.map(_.toByte))

    val rpcRequest = RpcRequest(1, Seq("a", "b", "c"), Map("a" -> byteBuffer))

    val deserializedRequest = RpcRequest.deserialize(RpcRequest.serialize(rpcRequest))
    val deserializedByteBuffer = deserializedRequest.params("a")

    deserializedRequest should be(rpcRequest)
    deserializedByteBuffer should be(byteBuffer)
  }

  "serialize and deserialize RPC response" in {
    val byteBuffer = ByteBuffer.allocate(1000)
    byteBuffer.put("test".toArray.map(_.toByte))

    val ex = new RuntimeException("Error!")
    val error = ExceptionIsThrownError(ex)
    val rpcResponse = RpcResponse(1, Some(byteBuffer), Some(error))

    val result = RpcResponse.deserialize(RpcResponse.serialize(rpcResponse))
    result.id should be(1)
    result.data.get should be(byteBuffer)
    result.error.get should be(error)
  }

}
