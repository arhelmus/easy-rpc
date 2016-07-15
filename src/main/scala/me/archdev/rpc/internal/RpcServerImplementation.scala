package me.archdev.rpc.internal

import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.scaladsl.Flow
import akka.util.ByteString
import autowire.Error.InvalidInput
import me.archdev.rpc._
import me.archdev.rpc.protocol._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

private[rpc] class RpcServerImplementation(router: Router.Router, parallelism: Int = 100)
  (implicit as: ActorSystem, ec: ExecutionContext, m: Materializer) {

  val rpcServerFlow = Flow[ByteString]
    .map(deserializeRequest)
    .mapAsync(parallelism)(executeRpcRequest(_, router))
    .map(serializeResponse)
    .recover {
      case ex: Throwable =>
        throw new RuntimeException("Unexpected exception in rpc server flow.", ex)
    }

  def launch(connection: IncomingTcpConnection) =
    connection.runForeach(_.handleWith(rpcServerFlow))

  private def deserializeRequest(byteString: ByteString): Try[RpcRequest] =
    Try(RpcRequest.deserialize(byteString.asByteBuffer))

  private def executeRpcRequest(rpcRequestTry: Try[RpcRequest], router: Router.Router): Future[RpcResponse] =
    rpcRequestTry.map(wireRpcRequest(router, _)) match {
      case Success(rpcResponse) => rpcResponse
      case Failure(ex) => ???
    }

  private def serializeResponse(rpcResponse: RpcResponse): ByteString =
    ByteString(RpcResponse.serialize(rpcResponse))

  private val routerErrorHandler: PartialFunction[Throwable, ErrorProtocol] = {
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

  private def wireRpcRequest(router: Router.Router, r: RpcRequest) =
    Future.successful()
      .flatMap(_ => router(autowire.Core.Request(r.path, r.params)))
      .map(result => SuccessfulRpcResponse(r.id, result))
      .recover(routerErrorHandler.andThen(FailedRpcResponse(r.id, _)))

}