package me.archdev.rpc

import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.scaladsl.{Flow, Tcp, Source}
import akka.stream.scaladsl.Tcp.{ServerBinding, IncomingConnection}
import akka.util.ByteString
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.{Future, ExecutionContext}

class RpcServer(router: Router.Router, backpressure: Int = 42)
  (implicit actorSystem: ActorSystem, executionContext: ExecutionContext, materializer: Materializer) extends LazyLogging {

  def start(host: String, port: Int) = {
    val connections: Source[IncomingConnection, Future[ServerBinding]] = Tcp().bind(host, port)
    val flow = Flow[ByteString]
      .map(_.asByteBuffer)
      .map(RpcRequest.deserialize)
      .map(r => autowire.Core.Request(r.path, r.params))
      .mapAsync(backpressure)(router)
      .map(ByteString(_))
      .recover {
        case ex =>
          val message = "An error occurred during processing of RPC call"
          logger.error(message, ex)
          ByteString()
      }

    connections.runForeach(_.handleWith(flow))
  }

}