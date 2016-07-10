package me.archdev

import java.nio.ByteBuffer

import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.scaladsl.Tcp.{IncomingConnection, OutgoingConnection, ServerBinding}
import akka.stream.scaladsl.{Flow, Source, Tcp}
import akka.util.ByteString
import autowire.ClientProxy
import autowire.Error.InvalidInput
import autowire.Error.Param.{Invalid, Missing}
import boopickle.{Pickler => _, _}

import scala.concurrent.{ExecutionContext, Future}

package object rpc extends DefaultBoopicklePicklers {

  type IncomingTcpConnection = Source[IncomingConnection, Future[ServerBinding]]
  type OutgoingTcpConnection = Flow[ByteString, ByteString, Future[OutgoingConnection]]

  class RpcClient[T](host: String, port: Int)(implicit as: ActorSystem, ec: ExecutionContext, m: Materializer)
    extends ClientProxy[T, ByteBuffer, Pickler, Pickler](new RpcClientImplementation(Tcp().outgoingConnection(host, port)))

  class RpcServer(router: Router.Router, parallelism: Int = 100)(implicit as: ActorSystem, ec: ExecutionContext, m: Materializer) {
    private val server = new RpcServerImplementation(router, parallelism)
    def launch(host: String, port: Int) = server.launch(Tcp().bind(host, port))
  }

  class RpcInvocationException(message: String) extends RuntimeException(message)
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

}

sealed trait DefaultBoopicklePicklers extends Base with
  BasicImplicitPicklers with
  TransformPicklers with
  TuplePicklers with
  MaterializePicklerFallback
