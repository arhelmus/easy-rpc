package me.archdev.rpc.internal

import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.scaladsl.Flow
import akka.util.ByteString
import autowire.Error.InvalidInput
import me.archdev.rpc._
import me.archdev.rpc.protocol._

import scala.concurrent.ExecutionContext

private[rpc] class RpcServerImplementation(router: Router.Router, parallelism: Int = 100)
  (implicit as: ActorSystem, ec: ExecutionContext, m: Materializer) {

  val handleErrors: PartialFunction[Throwable, ErrorProtocol] = {
    case ex: MatchError =>
      MethodNotFoundError(ex)
    case ex: InvalidInput =>
      InvalidMethodParametersError(ex)
    case ex: Throwable =>
      ExceptionIsThrownError(
        ex.getClass.getSimpleName,
        ex.getMessage,
        ex.getStackTrace.map(_.toString)
      )
  }

  // TODO: refactor flow on XOR composition
  val rpcServerFlow = Flow[ByteString]
    .map(_.asByteBuffer)
    .map(RpcRequest.deserialize)
    .map(r => r.id -> autowire.Core.Request(r.path, r.params))
    .mapAsync(parallelism)(r => router(r._2).map(r._1 -> _))
    .map(r => RpcResponse(r._1, Some(r._2), None))
    .map(RpcResponse.serialize)
    .map(ByteString.apply)
    .recover(handleErrors andThen formErrorResponse)

  def launch(connection: IncomingTcpConnection) =
    connection.runForeach(_.handleWith(rpcServerFlow))

  private def formErrorResponse(errorProtocol: ErrorProtocol) =
    ByteString(RpcResponse.serialize(RpcResponse(1, None, Some(errorProtocol))))
}