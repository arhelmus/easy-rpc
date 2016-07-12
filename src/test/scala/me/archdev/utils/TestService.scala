package me.archdev.utils

import java.nio.ByteBuffer

import akka.util.ByteString
import me.archdev.rpc._
import me.archdev.rpc.protocol.{RpcRequest, RpcResponse}

import scala.concurrent.ExecutionContext

trait TestService {
  def echo(message: String): String
  def throwEx(): String
}

class TestServiceImpl extends TestService {
  override def echo(message: String): String = message
  override def throwEx(): String = throw new RuntimeException("Hey, we got an error!")
}

object TestServiceRouter {

  def apply(echoServiceImpl: TestServiceImpl = new TestServiceImpl)(implicit ec: ExecutionContext) =
    Router.route[TestService](echoServiceImpl)

  def echoRequest(message: String) =
    RpcRequest(1,
      Vector("me", "archdev", "utils", "TestService", "echo"),
      Map("message" -> Pickle.intoBytes(message))
    )

  def echoResponse(message: String) =
    RpcResponse(
      1,
      Some(ByteBuffer.wrap(Array(4.toByte) ++ s"$message".toArray.map(_.toByte))),
      None
    )

  def throwRequest() =
    RpcRequest(1,
      Vector("me", "archdev", "utils", "TestService", "throwEx"),
      Map()
    )

}