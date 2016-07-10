package me.archdev

import java.nio.ByteBuffer

import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.scaladsl.Tcp.OutgoingConnection
import akka.stream.scaladsl.{Flow, Tcp}
import akka.util.ByteString
import autowire.ClientProxy
import boopickle.{Pickler => _, _}

import scala.concurrent.{ExecutionContext, Future}

package object rpc extends DefaultBoopicklePicklers {

  type OutgoingTcpConnection = Flow[ByteString, ByteString, Future[OutgoingConnection]]

  class RpcClient[T](host: String, port: Int)(implicit as: ActorSystem, ec: ExecutionContext, m: Materializer)
    extends ClientProxy[T, ByteBuffer, Pickler, Pickler](new RpcClientImplementation(Tcp().outgoingConnection(host, port)))

  class RpcServer(router: Router.Router, parallelism: Int = 100)(implicit as: ActorSystem, ec: ExecutionContext, m: Materializer) {
    private val server = new RpcServerImplementation(router, parallelism)
    def launch(host: String, port: Int) = server.launch(host, port)
  }

}

sealed trait DefaultBoopicklePicklers extends Base with
  BasicImplicitPicklers with
  TransformPicklers with
  TuplePicklers with
  MaterializePicklerFallback
