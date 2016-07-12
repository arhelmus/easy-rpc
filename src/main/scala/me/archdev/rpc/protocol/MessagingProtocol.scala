package me.archdev.rpc.protocol

import java.nio.ByteBuffer

import me.archdev.rpc._

/**
  * Request model for method execution.
  *
  * @param id request id, must be unique per client instance.
  * @param path method path that contains full class path and method name in the end. Every part of path is separate element of list.
  * @param params parameters that must be applied to requested method. Map key is parameter name.
  */
case class RpcRequest(id: Long, path: Seq[String], params: Map[String, ByteBuffer])
object RpcRequest {
  def serialize(rpcRequest: RpcRequest): ByteBuffer = Pickle.intoBytes(rpcRequest)
  def deserialize(serializedRequest: ByteBuffer): RpcRequest = Unpickle[RpcRequest].fromBytes(serializedRequest)
}

/**
  * Response model that contains method result or error.
  *
  * @param id id from request.
  * @param data serialized method response. None if error occurred.
  * @param error error occurred during request execution. None if request executed successfully.
  */
case class RpcResponse(id: Long, data: Option[ByteBuffer], error: Option[ErrorProtocol])
object RpcResponse {
  def serialize(rpcResponse: RpcResponse): ByteBuffer = Pickle.intoBytes(rpcResponse)
  def deserialize(byteBuffer: ByteBuffer): RpcResponse = Unpickle[RpcResponse].fromBytes(byteBuffer)
}