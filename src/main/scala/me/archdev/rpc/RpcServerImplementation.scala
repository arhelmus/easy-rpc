package me.archdev.rpc

import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.scaladsl.Flow
import akka.util.ByteString
import autowire.Error.InvalidInput

import scala.concurrent.ExecutionContext

private[rpc] class RpcServerImplementation(router: Router.Router, parallelism: Int = 100)
  (implicit as: ActorSystem, ec: ExecutionContext, m: Materializer) {

  val handleErrors: PartialFunction[Throwable, ByteString] = {
    case ex: MatchError =>
      Utils.serialize(MethodNotFoundException(ex))
    case ex: InvalidInput =>
      Utils.serialize(InvalidMethodParametersException(ex))
    case ex: Throwable =>
      Utils.serialize(ErrorOccurredException(ex.getClass.getSimpleName, ex.getMessage, ex.getStackTrace.map(_.toString)))
  }

  val rpcServerFlow = Flow[ByteString]
    .map(_.asByteBuffer)
    .map(RpcRequest.deserialize)
    .map(r => autowire.Core.Request(r.path, r.params))
    .mapAsync(parallelism)(router)
    .map(ByteString.apply)
    .recover(handleErrors)

  def launch(connection: IncomingTcpConnection) =
    connection.runForeach(_.handleWith(rpcServerFlow))

}