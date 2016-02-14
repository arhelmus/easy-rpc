package me.archdev.rpc

import java.nio.ByteBuffer

import boopickle.Default._

case class RpcRequest(path: Seq[String], params: Map[String, ByteBuffer])

object RpcRequest {
  def serialize(rpcRequest: RpcRequest): ByteBuffer = Pickle.intoBytes(rpcRequest)
  def deserialize(serializedRequest: ByteBuffer): RpcRequest = Unpickle[RpcRequest].fromBytes(serializedRequest)
}
