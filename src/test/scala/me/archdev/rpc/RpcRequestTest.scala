package me.archdev.rpc

import java.nio.ByteBuffer

import org.scalatest.{Matchers, WordSpec}

class RpcRequestTest extends WordSpec with Matchers {

  "serialize and deserialize RPC request" in {
    val byteBuffer = ByteBuffer.allocate(1000)
    byteBuffer.put("test".toArray.map(_.toByte))

    val rpcRequest = RpcRequest(Seq("a", "b", "c"), Map("a" -> byteBuffer))

    val deserializedRequest = RpcRequest.deserialize(RpcRequest.serialize(rpcRequest))
    val deserializedByteBuffer = deserializedRequest.params("a")

    deserializedRequest should be(rpcRequest)
    deserializedByteBuffer should be(byteBuffer)
  }

}
