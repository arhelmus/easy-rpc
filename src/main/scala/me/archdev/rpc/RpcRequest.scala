package me.archdev.rpc

import java.nio.ByteBuffer

import autowire.Error.InvalidInput
import autowire.Error.Param.{Invalid, Missing}
import boopickle.Default._

sealed trait RpcProtocol

case class RpcRequest(path: Seq[String], params: Map[String, ByteBuffer]) extends RpcProtocol
object RpcRequest {
  def serialize(rpcRequest: RpcRequest): ByteBuffer = Pickle.intoBytes(rpcRequest)
  def deserialize(serializedRequest: ByteBuffer): RpcRequest = Unpickle[RpcRequest].fromBytes(serializedRequest)
}

class RpcInvocationException(message: String) extends RuntimeException(message) with RpcProtocol
case class ErrorOccurredException(name: String, message: String, stackTrace: Seq[String]) extends RpcInvocationException("Rpc invocation exception. Error occurred.")

case class MethodNotFoundException(classPath: String, method: String)
  extends RpcInvocationException(s"Rpc invocation exception. Cannot find method '$method' in class '$classPath'.")
object MethodNotFoundException {
  def apply(matchError: MatchError) = {
    val path = matchError.getMessage().drop(15).takeWhile(_ != ')').split(",").map(_.trim)
    new MethodNotFoundException(path.dropRight(1).mkString("."), path.last)
  }
}

case class InvalidMethodParametersException(invalidParameters: Seq[String])
  extends RpcInvocationException(s"Rpc invocation exception. Invalid method parameters: ${invalidParameters.mkString(", ")}.")
object InvalidMethodParametersException {
  def apply(invalidInputEx: InvalidInput) =
    new InvalidMethodParametersException(invalidInputEx.exs.map {
      case Missing(p) => p
      case Invalid(p, ex) => p
    })
}
